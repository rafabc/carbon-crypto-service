/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.crypto.provider;

import com.google.gson.Gson;
import org.apache.axiom.om.util.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.crypto.api.CipherMetaDataHolder;
import org.wso2.carbon.crypto.api.CryptoException;
import org.wso2.carbon.crypto.api.InternalCryptoProvider;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * The Java Keystore based implementation of {@link InternalCryptoProvider}
 */
public class KeyStoreBasedInternalCryptoProvider implements InternalCryptoProvider {

    private static Log log = LogFactory.getLog(KeyStoreBasedInternalCryptoProvider.class);
    private static final String DEFAULT_ASSYMETRIC_CRYPTO_ALGORITHM = "RSA";
    private static final String CIPHER_TRANSFORMATION_SYSTEM_PROPERTY = "org.wso2.CipherTransformation";

    private KeyStore keyStore;
    private String keyAlias;
    private String keyPassword;

    public KeyStoreBasedInternalCryptoProvider(KeyStore keyStore, String keyAlias, String keyPassword) {

        this.keyStore = keyStore;
        this.keyAlias = keyAlias;
        this.keyPassword = keyPassword;
    }

    /**
     * Computes and returns the ciphertext of the given cleartext, using the underlying key store.
     *
     * @param cleartext               The cleartext to be encrypted.
     * @param algorithm               The encryption / decryption algorithm
     * @param javaSecurityAPIProvider
     * @param params
     * @return the ciphertext
     * @throws CryptoException
     */
    @Override
    public byte[] encrypt(byte[] cleartext, String algorithm, String javaSecurityAPIProvider, Object... params) throws CryptoException {

        try {
            Cipher cipher;

            if (StringUtils.isBlank(algorithm)) {
                algorithm = DEFAULT_ASSYMETRIC_CRYPTO_ALGORITHM;
            }
            if (StringUtils.isBlank(javaSecurityAPIProvider)) {
                cipher = Cipher.getInstance(algorithm);
            } else {
                cipher = Cipher.getInstance(algorithm, javaSecurityAPIProvider);
            }

            Certificate certificate = getCertificateFromStore();

            if (log.isDebugEnabled()) {
                log.debug("Certificate used for encrypting : " + certificate);
            }

            cipher.init(Cipher.ENCRYPT_MODE, certificate.getPublicKey());

            byte[] ciphertext = cipher.doFinal(cleartext);

            if (log.isDebugEnabled()) {
                log.debug(String.format("Successfully encrypted data using the algorithm '%s' and the " +
                        "Java Security API provider '%s'", algorithm, javaSecurityAPIProvider));
            }

            return ciphertext;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException
                | IllegalBlockSizeException | KeyStoreException | InvalidKeyException | NoSuchProviderException e) {
            String errorMessage = String.format("An error occurred while encrypting using the algorithm '%s' and the " +
                    "Java Security API provider '%s'", algorithm, javaSecurityAPIProvider);

            // Log the exception from client libraries, to avoid missing information if callers code doesn't log it
            if(log.isDebugEnabled()){
                log.debug(errorMessage, e);
            }

            throw new CryptoException(errorMessage, e);
        }
    }

    /**
     * Computes and returns the cleartext of the given ciphertext.
     *
     * @param ciphertext              The ciphertext to be decrypted.
     * @param algorithm               The encryption / decryption algorithm
     * @param javaSecurityAPIProvider
     * @param params
     * @return The cleartext
     * @throws CryptoException If something unexpected happens during the decryption operation.
     */
    public byte[] decrypt(byte[] ciphertext, String algorithm, String javaSecurityAPIProvider, Object... params) throws CryptoException {

        try {
            Cipher cipher;

            if (StringUtils.isBlank(algorithm)) {
                algorithm = DEFAULT_ASSYMETRIC_CRYPTO_ALGORITHM;
            }
            if (StringUtils.isBlank(javaSecurityAPIProvider)) {
                cipher = Cipher.getInstance(algorithm);
            } else {
                cipher = Cipher.getInstance(algorithm, javaSecurityAPIProvider);
            }

            cipher.init(Cipher.DECRYPT_MODE, getPrivateKeyFromKeyStore());

            if (log.isDebugEnabled()) {
                log.debug(String.format("Successfully decrypted data using the algorithm '%s' and the " +
                        "Java Security API provider '%s'", algorithm, javaSecurityAPIProvider));
            }

            return cipher.doFinal(ciphertext);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | NoSuchProviderException | BadPaddingException
                | IllegalBlockSizeException | InvalidKeyException | UnrecoverableKeyException | KeyStoreException e) {
            String errorMessage = String.format("An error occurred while decrypting using the algorithm : '%s', and " +
                    "crypto provider : '%s'", algorithm, this.getClass().getName());

            // Log the exception from client libraries, to avoid missing information if callers code doesn't log it
            if(log.isDebugEnabled()){
                log.debug(errorMessage, e);
            }

            throw new CryptoException(errorMessage, e);
        }
    }

    @Override
    public byte[] encrypt(byte[] cleartext, String algorithm, String javaSecurityAPIProvider,
                          boolean returnSelfContainedCipherText) throws CryptoException {

        byte[] encryptedKey;
        if (cleartext == null) {
            throw new CryptoException("Plaintext can't be null.");
        }
        if (StringUtils.isNotBlank(algorithm) && cleartext.length == 0) {
            if (log.isDebugEnabled()) {
                log.debug("Plaintext is empty. An empty array will be used as the ciphertext bytes.");
            }
            encryptedKey = StringUtils.EMPTY.getBytes();
        } else {
            encryptedKey = encrypt(cleartext, algorithm, javaSecurityAPIProvider);
        }
        if (returnSelfContainedCipherText) {
            Certificate certificate = null;
            try {
                certificate = getCertificateFromStore();
                encryptedKey = createSelfContainedCiphertext(encryptedKey, algorithm, certificate);
            } catch (KeyStoreException | CertificateEncodingException | NoSuchAlgorithmException e) {
                String errorMessage = String.format("An error occurred while encrypting using the algorithm : '%s', " +
                                "and crypto provider : '%s'"
                        , algorithm, this.getClass().getName());

                // Log the exception from client libraries, to avoid missing information if callers code doesn't log it
                if (log.isDebugEnabled()) {
                    log.debug(errorMessage, e);
                }

                throw new CryptoException(errorMessage, e);
            }

        }
        return encryptedKey;
    }

    private Certificate getCertificateFromStore() throws KeyStoreException {

        return keyStore.getCertificate(keyAlias);
    }

    private PrivateKey getPrivateKeyFromKeyStore() throws UnrecoverableKeyException, NoSuchAlgorithmException
            , KeyStoreException {

        Key key = keyStore.getKey(keyAlias, keyPassword.toCharArray());

        if (key instanceof PrivateKey) {
            return (PrivateKey) key;
        } else {
            return null;
        }
    }

    private byte[] createSelfContainedCiphertext(byte[] originalCipher, String transformation, Certificate certificate)
            throws CertificateEncodingException, NoSuchAlgorithmException {

        Gson gson = new Gson();
        CipherMetaDataHolder cipherHolder = new CipherMetaDataHolder();
        cipherHolder.setCipherText(Base64.encode(originalCipher));
        cipherHolder.setTransformation(transformation);
        cipherHolder.setThumbPrint(calculateThumbprint(certificate, "SHA-1"), "SHA-1");
        String cipherWithMetadataStr = gson.toJson(cipherHolder);
        if (log.isDebugEnabled()) {
            log.debug("Cipher with meta data : " + cipherWithMetadataStr);
        }
        return cipherWithMetadataStr.getBytes(Charset.defaultCharset());
    }

    private String calculateThumbprint(Certificate certificate, String digest)
            throws NoSuchAlgorithmException, CertificateEncodingException {

        MessageDigest messageDigest = MessageDigest.getInstance(digest);
        messageDigest.update(certificate.getEncoded());
        byte[] digestByteArray = messageDigest.digest();
        char[] hexCharacters = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B',
                'C', 'D', 'E', 'F'};


        // convert digest in form of byte array to hex format
        StringBuffer strBuffer = new StringBuffer();

        for (int i = 0; i < digestByteArray.length; i++) {
            int leftNibble = (digestByteArray[i] & 0xF0) >> 4;
            int rightNibble = (digestByteArray[i] & 0x0F);
            strBuffer.append(hexCharacters[leftNibble]).append(hexCharacters[rightNibble]);
        }

        return strBuffer.toString();
    }
}
