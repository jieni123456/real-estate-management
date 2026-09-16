package util;

/**
 * 共用的输入校验规则。
 *
 * <p>对应需求报告 G-003。约定：<b>校验通过返回 {@code null}，不通过返回给用户看的原因</b>。
 *
 * <p>各条规则的长度上限取自数据库表的列宽，避免写入时被数据库截断或报错：
 * <pre>
 *   houses.id / houses.type / houses.landlord_id   VARCHAR(50)
 *   houses.address / landlords.name / customers.name  VARCHAR(100 / 255)
 *   customers.id                                    VARCHAR(50)
 *   customers.phone                                 VARCHAR(20)
 * </pre>
 */
public final class Validators {

    /** 电话号码允许出现的字符：数字、加号、减号、空格、括号。手机与座机都能覆盖 */
    private static final String PHONE_ALLOWED = "[0-9+\\-\\s()]+";

    private Validators() {
    }

    /**
     * 必填文本。
     *
     * @return 通过返回 null；为空返回「XX不能为空」；超长返回「XX不能超过 N 个字符」
     */
    public static String requiredText(String label, String value, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            return label + "不能为空";
        }
        if (value.trim().length() > maxLength) {
            return label + "不能超过 " + maxLength + " 个字符";
        }
        return null;
    }

    /** 选填文本，仅校验长度 */
    public static String optionalText(String label, String value, int maxLength) {
        if (value != null && value.trim().length() > maxLength) {
            return label + "不能超过 " + maxLength + " 个字符";
        }
        return null;
    }

    /**
     * 电话号码：必填，长度 5–20，且只允许数字与常见分隔符。
     * 不按手机号规则强校验（11 位 1 开头），因为座机同样合法。
     */
    public static String phone(String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            return label + "不能为空";
        }
        String trimmed = value.trim();
        if (trimmed.length() < 5 || trimmed.length() > 20) {
            return label + "长度应在 5 到 20 位之间";
        }
        if (!trimmed.matches(PHONE_ALLOWED)) {
            return label + "只能包含数字、加号、减号、空格与括号";
        }
        return null;
    }

    /** 数量类：必须大于 0 */
    public static String positiveNumber(String label, double value) {
        if (value <= 0) {
            return label + "必须大于 0";
        }
        return null;
    }
}
