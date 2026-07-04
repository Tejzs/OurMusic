package routes.subsonic;

import config.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class SubsonicTokenSecret {
    private static final String CIPHER_NAME = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SubsonicTokenSecret() {
    }

    public static String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt Subsonic token secret.", e);
        }
    }

    public static String decrypt(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            return null;
        }

        try {
            byte[] payload = Base64.getDecoder().decode(encryptedValue);
            if (payload.length <= IV_LENGTH) {
                return null;
            }

            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);

            Cipher cipher = Cipher.getInstance(CIPHER_NAME);
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] decrypted = cipher.doFinal(cipherText);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static SecretKeySpec secretKey() throws Exception {
        String configuredSecret = Properties.getSubsonicAuthSecret();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] key = digest.digest(configuredSecret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(Arrays.copyOf(key, 16), "AES");
    }
}
