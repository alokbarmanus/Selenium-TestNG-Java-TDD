package com.seleniumproject.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public final class AESUtil {

    private static final Logger LOG = LoggerFactory.getLogger(AESUtil.class);

    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final String KDF_ALGO = "PBKDF2WithHmacSHA256";

    private static final int KEY_SIZE_BITS = 256;
    private static final int ITERATIONS = 120_000;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private static final String TOKEN_VERSION = "v1";

    private static final String SYS_SECRET = "aes.secret";
    private static final String ENV_SECRET = "AES_SECRET";

    private AESUtil() {
    }

    /**
     * Encrypts plain text using a secret loaded from -Daes.secret or AES_SECRET env var.
     */
    public static String encrypt(String plainText) {
        return encrypt(plainText, resolveSecretOrThrow());
    }

    /**
     * Decrypts token using a secret loaded from -Daes.secret or AES_SECRET env var.
     */
    public static String decrypt(String token) {
        return decrypt(token, resolveSecretOrThrow());
    }

    /**
     * Encrypts plain text with AES-GCM and returns a versioned token:
     * v1:base64(salt):base64(iv):base64(ciphertext)
     */
    public static String encrypt(String plainText, String secret) {
        if (plainText == null) {
            throw new IllegalArgumentException("plainText must not be null");
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("secret must not be blank");
        }

        try {
            byte[] salt = randomBytes(SALT_LENGTH_BYTES);
            byte[] iv = randomBytes(IV_LENGTH_BYTES);
            SecretKeySpec key = deriveKey(secret, salt);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return TOKEN_VERSION + ":"
                + base64(salt) + ":"
                + base64(iv) + ":"
                + base64(encrypted);
        } catch (Exception e) {
            LOG.error("Encryption failed", e);
            throw new IllegalStateException("Encryption failed. Check secret/key configuration.", e);
        }
    }

    /**
     * Decrypts a token in format v1:base64(salt):base64(iv):base64(ciphertext).
     */
    public static String decrypt(String token, String secret) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("secret must not be blank");
        }

        try {
            String[] parts = token.split(":");
            if (parts.length != 4 || !TOKEN_VERSION.equals(parts[0])) {
                throw new IllegalArgumentException(
                    "Unsupported token format. Expected: v1:base64(salt):base64(iv):base64(ciphertext)");
            }

            byte[] salt = fromBase64(parts[1]);
            byte[] iv = fromBase64(parts[2]);
            byte[] encrypted = fromBase64(parts[3]);

            SecretKeySpec key = deriveKey(secret, salt);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Decryption failed", e);
            throw new IllegalStateException("Decryption failed. Input token or secret may be invalid.", e);
        }
    }

    public static String getDecryptedString(String encryptedValue) {
        return decrypt(encryptedValue);
    }

    private static SecretKeySpec deriveKey(String secret, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, KEY_SIZE_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGO);
        byte[] key = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(key, "AES");
    }

    private static String resolveSecretOrThrow() {
        String secret = System.getProperty(SYS_SECRET);
        if (secret == null || secret.isBlank()) {
            secret = System.getenv(ENV_SECRET);
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Missing AES secret. Set -D" + SYS_SECRET + "=<value> or env " + ENV_SECRET);
        }
        return secret;
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private static String base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static byte[] fromBase64(String value) {
        return Base64.getDecoder().decode(value);
    }
}
