package util;

/**
 * 展示用的格式化工具。
 *
 * <p>集中放置，避免同一套格式在多个界面里各写一遍（例如面积的「去掉多余 .0」
 * 原先只在 HouseView 内部实现，概览页出现后就需要用同一套规则）。
 */
public final class Formats {

    private Formats() {
    }

    /**
     * 面积：128.0 显示为 128，89.5 保持 89.5。
     *
     * <p>刻意不做四舍五入——表格里显示的数值会被「编辑」对话框原样回填，
     * 若这里把 89.25 显示成 89.3，用户保存后就会把库里的值改成 89.3，
     * 属于静默的数据变更。
     */
    public static String area(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "—";
        }
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    /** 平均值一类的派生数值：保留一位小数，且不显示多余的 .0 */
    public static String average(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) {
            return "—";
        }
        return area(Math.round(value * 10) / 10.0);
    }
}
