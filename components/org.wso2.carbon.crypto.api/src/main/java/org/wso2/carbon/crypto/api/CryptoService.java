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

package org.wso2.carbon.crypto.api;

import java.security.cert.Certificate;

/**
 * The service contract of an implementation of a crypto service.
 * <p>
 * The service provides crypto related functionality such as signing and decrypting.
 */
public interface CryptoService {

    /**
     * Computes and returns the ciphertext of the given cleartext.
     * <p>
     * The encrypted data is only for internal usage (e.g. encrypting passwords before persisting),
     * therefore the ciphertext is <b>NOT</b> supposed to be shared with other systems.
     *
     * @param cleartext               The cleartext to be encrypted.
     * @param algorithm               The encryption / decryption algorithm
     * @param javaSecurityAPIProvider The Java Security API provider.
     * @return The ciphertext
     * @throws CryptoException If something unexpected happens during the encryption operation.
     */
    byte[] encrypt(byte[] cleartext, String algorithm, String javaSecurityAPIProvider) throws CryptoException;

    /**
     * Computes and returns the cleartext of the given ciphertext.
     * <p>
     * The input ciphertext should be a ciphertext which was generated from
     * {@link #encrypt(byte[], String, String)} method.
     *
     * @param ciphertext              The ciphertext to be decrypted.
     * @param algorithm               The signature + hashing algorithm to be used in signing.
     * @param javaSecurityAPIProvider The Java Security API provider.
     * @return The cleartext
     * @throws CryptoException If something unexpected happens during the decryption operation.
     */
    byte[] decrypt(byte[] ciphertext, String algorithm, String javaSecurityAPIProvider) throws CryptoException;

    /**
     * Computes and returns the signature of given data.
     *
     * @param data                    The content which the signature should be generated against.
     * @param algorithm               The signature + hashing algorithm to be used in signing.
     * @param javaSecurityAPIProvider The Java Security API provider.
     * @param cryptoContext           The context information which is needed to discover the private key.
     * @return The digital signature of given data.
     * @throws CryptoException If something unexpected happens during the signing operation.
     */
    byte[] sign(byte[] data, String algorithm, String javaSecurityAPIProvider, CryptoContext cryptoContext)
            throws CryptoException;

    /**
     * Computes and returns the cleartext of the given ciphertext comes from an external entity.
     * <b>Asymmetric cryptography</b> is used for decryption.
     *
     * @param ciphertext              The content which the signature should be generated against.
     * @param algorithm               The signature + hashing algorithm to be used in signing.
     * @param javaSecurityAPIProvider The Java Security API provider.
     * @param cryptoContext           The context information which is needed to discover the private key.
     * @return An array of bytes which contains the signature.
     * @throws CryptoException If something unexpected happens during the decryption operation.
     */
    byte[] decrypt(byte[] ciphertext, String algorithm, String javaSecurityAPIProvider, CryptoContext cryptoContext)
            throws CryptoException;

    /**
     * Computes and returns the ciphertext of the given cleartext, using <b>asymmetric cryptography</b>.
     * This ciphertext is indented to be used and decrypted by an external entity.
     *
     * @param cleartext               The cleartext to be encrypted.
     * @param algorithm               The signature + hashing algorithm to be used in signing.
     * @param javaSecurityAPIProvider The Java Security API provider.
     * @param cryptoContext           The context information which is needed to discover the public key of the external entity.
     * @return The encrypted data.
     * @throws CryptoException If something unexpected happens during the encryption operation.
     */
    byte[] encrypt(byte[] cleartext, String algorithm, String javaSecurityAPIProvider, CryptoContext cryptoContext)
            throws CryptoException;

    /**
     * Verifies whether given signature of the given data was generated by a trusted external party.
     *
     * @param data                    The data which was the signature generated on.
     * @param signature               The signature bytes of data.
     * @param algorithm               The signature + hashing algorithm to be used in signing.
     * @param javaSecurityAPIProvider The Java Security API provider.
     * @param cryptoContext           The context information which is needed to discover the public key of the external entity.
     * @return true if signature can be verified, false otherwise.
     * @throws CryptoException If something unexpected happens during the signature verification.
     */
    boolean verifySignature(byte[] data, byte[] signature, String algorithm,
                            String javaSecurityAPIProvider, CryptoContext cryptoContext) throws CryptoException;

    /**
     * Returns the {@link Certificate} based on the given {@link CryptoContext}.
     *
     * @param cryptoContext The context information which is used to discover the public key of the external entity.
     * @return The {@link Certificate} relates with the given context.
     * @throws CryptoException If something unexpected happens during certificate discovery.
     */
    Certificate getCertificate(CryptoContext cryptoContext) throws CryptoException;

    /**
     * In hybrid encryption clear data is encrypted using symmetric encryption mechanism
     * and symmetric key used for encryption is encrypted with asymmetric encryption.
     * Computes and return a {@link HybridEncryptionOutput} based on provided clear data.
     *
     * @param hybridEncryptionInput Input data for hybrid encryption.
     * @param symmetricAlgorithm    The symmetric encryption/decryption algorithm.
     * @param asymmetricAlgorithm   The asymmetric encryption/decryption algorithm.
     * @param javaSecurityProvider  The Java Security API provider.
     * @param cryptoContext         The context information which is used to discover the public key of the external entity.
     * @return {@link HybridEncryptionOutput} cipher text with required parameters
     * @throws CryptoException
     */
    default HybridEncryptionOutput hybridEncrypt(HybridEncryptionInput hybridEncryptionInput, String symmetricAlgorithm,
                                                 String asymmetricAlgorithm, String javaSecurityProvider,
                                                 CryptoContext cryptoContext) throws CryptoException {

        String errorMessage = "Hybrid encryption is not supported by this implementation.";
        throw new CryptoException(errorMessage);
    }

    /**
     * In hybrid decryption symmetric key used for encryption is decrypted with asymmetric decryption
     * and cipher data is decrypted using symmetric decryption mechanism.
     * Computes and return clear data based on provided {@link HybridEncryptionOutput}
     *
     * @param hybridEncryptionOutput {@link HybridEncryptionOutput} ciphered data with parameters.
     * @param symmetricAlgorithm     The symmetric encryption/decryption algorithm.
     * @param asymmetricAlgorithm    The asymmetric encryption/decryption algorithm.
     * @param javaSecurityProvider   The Java Security API provider.
     * @param cryptoContext          The context information which is used to discover the public key of the external entity.
     * @return the decrypted data
     * @throws CryptoException
     */
    default byte[] hybridDecrypt(HybridEncryptionOutput hybridEncryptionOutput, String symmetricAlgorithm,
                                 String asymmetricAlgorithm, String javaSecurityProvider,
                                 CryptoContext cryptoContext) throws CryptoException {

        String errorMessage = "Hybrid decryption is not supported by this implementation.";
        throw new CryptoException(errorMessage);
    }

    /**
     * Computes and returns the ciphertext of the given cleartext.
     * If using assymetric encryption and returnSelfContainedCipherText is true, the cipher text will be a self
     * contained cipher text.
     * The encrypted data is only for internal usage (e.g. encrypting passwords before persisting),
     * therefore the ciphertext is <b>NOT</b> supposed to be shared with other systems.
     *
     * @param cleartext                     The cleartext to be encrypted.
     * @param algorithm                     The encryption / decryption algorithm
     * @param javaSecurityAPIProvider       The Java Security API provider.
     * @param returnSelfContainedCipherText Whether cipher text need to be self contained.
     * @return The ciphertext
     * @throws CryptoException If something unexpected happens during the encryption operation.
     */
    default byte[] encrypt(byte[] cleartext, String algorithm, String javaSecurityAPIProvider,
                           boolean returnSelfContainedCipherText) throws CryptoException {

        String errorMessage = "Encryption with self contained cipher text is not supported by this implementation.";
        throw new CryptoException(errorMessage);
    }

    /**
     * Computes and returns the cipher text of the given cleartext.
     * In this method api, we can pass the internalCryptoProviderType and get the clear text encrypted using the
     * preferred internal crypto provider.
     *
     * @param cleartext                     The cleartext to be encrypted.
     * @param algorithm                     The encryption algorithm.
     * @param javaSecurityAPIProvider       The Java Security API provider.
     * @param returnSelfContainedCipherText Whether cipher text need to be self contained.
     * @param internalCryptoProviderType    Preferred internal crypto provider.
     * @return The ciphertext
     * @throws CryptoException If something unexpected happens during the encryption operation.
     */
    default byte[] encrypt(byte[] cleartext, String algorithm, String javaSecurityAPIProvider,
                                               boolean returnSelfContainedCipherText, String internalCryptoProviderType)
            throws CryptoException {

        String errorMessage =
                "Encryption with providing internal crypto provider type is not supported by this implementation.";
        throw new CryptoException(errorMessage);
    }

    /**
     * Computes and returns the plain text of a given cipher text.
     * In this method api, we can pass the internalCryptoProviderType and get the cipher text decrypted using the
     * preferred internal crypto provider.
     *
     * @param ciphertext                 The encrypted text.
     * @param algorithm                  The encryption algorithm.
     * @param javaSecurityAPIProvider    The Java Security API provider.
     * @param internalCryptoProviderType Preferred internal crypto provider.
     * @return TThe clear text in byte array format.
     * @throws CryptoException
     */
    default byte[] decrypt(byte[] ciphertext, String algorithm, String javaSecurityAPIProvider,
                           String internalCryptoProviderType) throws CryptoException {

        String errorMessage = "decryption with providing internal crypto provider type is not supported by this " +
                "implementation.";
        throw new CryptoException(errorMessage);
    }
}
