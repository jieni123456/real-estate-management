package util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码哈希与敏感字段加解密。
 *
 * <p>对应需求报告 G-011：密码由「SHA-256(密码) 直接 Base64」改为
 * <b>「每用户独立随机盐 + SHA-256」</b>，存储格式为 {@code salt:hash}（两段均为 Base64）。
 * 加盐之后相同密码在不同账号上的哈希也不同，彩虹表无法直接反查。
 *
 * <p><b>兼容旧数据</b>：历史记录是无盐哈希（不含分隔符）。{@link #verifyPassword}
 * 仍能校验通过，{@link #isLegacyHash} 用于识别，登录成功后由 AuthService
 * 顺手把它升级为加盐格式——这样不必手工改库，也不会让已有账号登录不了。
 *
 * <p>房东联系方式的 AES 加解密维持原状（密钥硬编码问题见 RISK-003）。
 */
public final class SecurityUtil {

    private static final String ENCRYPTION_KEY = "MySecretKey12345";

    /** 加盐哈希中分隔盐值与哈希值。Base64 字符集不含冒号，因此用它做分隔是安全的 */
    private static final char SEPARATOR = ':';
    private static final int SALT_BYTES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private SecurityUtil() {
    }

    // ------------------------------------------------------------ 密码

    /**
     * 生成带随机盐的密码哈希，格式 {@code salt:hash}。
     * 每次调用结果都不同（盐是随机的），这是加盐的预期行为。
     */
    public static String encryptPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt) + SEPARATOR + hashWithSalt(password, salt);
    }

    /**
     * 校验密码。
     *
     * @param rawPassword 用户输入的明文
     * @param stored      库中存储的值——加盐格式 {@code salt:hash} 或旧的无盐 Base64
     * @return 匹配返回 true；任一参数为空、格式非法、值不匹配均返回 false
     */
    public static boolean verifyPassword(String rawPassword, String stored) {
        if (rawPassword == null || stored == null || stored.isEmpty()) {
            return false;
        }

        int index = stored.indexOf(SEPARATOR);
        if (index < 0) {
            // 旧格式：无盐值。为兼容历史数据保留，登录后会被自动升级
            return constantTimeEquals(legacyHash(rawPassword), stored);
        }

        try {
            byte[] salt = Base64.getDecoder().decode(stored.substring(0, index));
            String expected = stored.substring(index + 1);
            return constantTimeEquals(hashWithSalt(rawPassword, salt), expected);
        } catch (IllegalArgumentException e) {
            // 存储值被改坏或格式非法：一律视为不匹配，不抛异常
            return false;
        }
    }

    /** 是否为无盐值的旧格式哈希（登录成功后应顺手升级） */
    public static boolean isLegacyHash(String stored) {
        return stored != null && !stored.isEmpty() && stored.indexOf(SEPARATOR) < 0;
    }

    private static String hashWithSalt(String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            digest.update(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest.digest());
        } catch (Exception e) {
            throw new IllegalStateException("密码哈希失败", e);
        }
    }

    /** 旧格式（无盐）的哈希，仅供兼容校验使用 */
    private static String legacyHash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(
                    digest.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("密码哈希失败", e);
        }
    }

    /** 定时安全比较，避免按字符提前返回泄露信息。用 MessageDigest.isEqual 而非 equals */
    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------ 房东联系方式

    public static String encryptContact(String contact) {
        try {
            SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(
                    cipher.doFinal(contact.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("联系方式加密失败", e);
        }
    }

    public static String decryptContact(String encryptedContact) {
        try {
            SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedContact));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("联系方式解密失败", e);
        }
    }
}
