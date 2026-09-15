package util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;

public class SecurityUtil {
    private static final String ENCRYPTION_KEY = "MySecretKey12345";

    public static String encryptPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Password encryption failed", e);
        }
    }

    public static String encryptContact(String contact) {
        try {
            SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(contact.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Contact encryption failed", e);
        }
    }

    public static String decryptContact(String encryptedContact) {
        try {
            SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedContact));
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Contact decryption failed", e);
        }
    }
}