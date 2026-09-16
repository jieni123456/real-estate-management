package test;

import util.SecurityUtil;

/**
 * SecurityUtil 的测试。对应需求报告 G-016（及 G-011 的加盐改造）。
 *
 * <p>重点覆盖两件事：
 * <ol>
 *   <li>加盐哈希的行为——同密码两次结果不同、错误密码不通过、坏数据不抛异常</li>
 *   <li><b>对旧数据的兼容</b>——改造前写入的无盐哈希必须仍能校验通过，
 *       否则用户改完代码就登不进系统了</li>
 * </ol>
 */
public final class SecurityUtilTest {

    /**
     * 改造前数据库里 admin 账号的哈希值（无盐 SHA-256 后 Base64）。
     * 它是真实存在过的值，用它来验证兼容性比造一个假值更有意义。
     */
    private static final String LEGACY_ADMIN_HASH =
            "JAvlGPq9JyTdtvBO6x2llnRI1+gxwIyPqCKAn3THIKk=";

    private SecurityUtilTest() {
    }

    public static void run(TestRunner t) {
        t.suite("SecurityUtil · 密码加盐（G-011）");

        String hash = SecurityUtil.encryptPassword("admin123");
        t.check("加盐后格式为 salt:hash", hash.indexOf(':') > 0);
        t.check("同一密码两次哈希不相同（盐是随机的）",
                !hash.equals(SecurityUtil.encryptPassword("admin123")));
        t.check("正确密码校验通过", SecurityUtil.verifyPassword("admin123", hash));
        t.check("错误密码校验不通过", !SecurityUtil.verifyPassword("admin124", hash));
        t.check("大小写敏感", !SecurityUtil.verifyPassword("Admin123", hash));
        t.check("空密码不通过", !SecurityUtil.verifyPassword("", hash));
        t.check("存储值为 null 不通过", !SecurityUtil.verifyPassword("admin123", null));
        t.check("存储值为空串不通过", !SecurityUtil.verifyPassword("admin123", ""));
        t.check("存储值格式损坏时不抛异常且不通过",
                !SecurityUtil.verifyPassword("admin123", "不是Base64:也是坏的"));
        t.check("新格式不被判定为旧格式", !SecurityUtil.isLegacyHash(hash));
        t.check("isLegacyHash 对 null 返回 false", !SecurityUtil.isLegacyHash(null));

        t.suite("SecurityUtil · 兼容改造前的无盐哈希");

        t.check("旧格式哈希仍能校验通过（否则老账号会登不进）",
                SecurityUtil.verifyPassword("admin123", LEGACY_ADMIN_HASH));
        t.check("旧格式哈希被识别为待升级",
                SecurityUtil.isLegacyHash(LEGACY_ADMIN_HASH));
        t.check("旧格式下错误密码依然不通过",
                !SecurityUtil.verifyPassword("wrong", LEGACY_ADMIN_HASH));

        t.suite("SecurityUtil · 房东联系方式加解密");

        String encrypted = SecurityUtil.encryptContact("13800138000");
        t.check("加密结果与明文不同", !encrypted.equals("13800138000"));
        t.equals("解密可还原", "13800138000", SecurityUtil.decryptContact(encrypted));
        t.equals("中文与符号也能往返",
                "李四 138-0013",
                SecurityUtil.decryptContact(SecurityUtil.encryptContact("李四 138-0013")));
        t.check("相同明文每次密文相同——ECB 模式的已知局限（RISK-003 一并处理）",
                encrypted.equals(SecurityUtil.encryptContact("13800138000")));
    }
}
