package util;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * 侧边栏图标。
 *
 * <p>按 R-002 第 8 轮决定，仅侧边栏使用图标，工具栏按钮保持纯文字。
 *
 * <p>图标用 Java2D 直接绘制，不引入外部图标资源，因此也无需为不同主题准备多套
 * 图片——颜色在绘制时传入即可。
 */
public final class Icons {

    private Icons() {
    }

    /** 房屋图标，用于「房屋管理」导航项 */
    public static Icon house(Color color, int size) {
        return new NavIcon(color, size, true);
    }

    /** 人物图标，用于「客户管理」导航项 */
    public static Icon person(Color color, int size) {
        return new NavIcon(color, size, false);
    }

    /**
     * 线描导航图标。绘制坐标按 24 × 24 的画布设计，再按目标尺寸等比缩放，
     * 这样在不同尺寸下都是同一套比例。
     */
    private static final class NavIcon implements Icon {

        private final Color color;
        private final int size;
        private final boolean house;

        private NavIcon(Color color, int size, boolean house) {
            this.color = color;
            this.size = size;
            this.house = house;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                        RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(Math.max(1.2f, size / 11f),
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                double scale = size / 24.0;
                if (house) {
                    paintHouse(g2, scale);
                } else {
                    paintPerson(g2, scale);
                }
            } finally {
                g2.dispose();
            }
        }

        private void paintHouse(Graphics2D g2, double s) {
            Path2D roof = new Path2D.Double();
            roof.moveTo(3 * s, 10.5 * s);
            roof.lineTo(12 * s, 3 * s);
            roof.lineTo(21 * s, 10.5 * s);
            g2.draw(roof);

            Path2D body = new Path2D.Double();
            body.moveTo(5.5 * s, 9.2 * s);
            body.lineTo(5.5 * s, 20 * s);
            body.lineTo(18.5 * s, 20 * s);
            body.lineTo(18.5 * s, 9.2 * s);
            g2.draw(body);
        }

        private void paintPerson(Graphics2D g2, double s) {
            g2.draw(new Ellipse2D.Double(8.6 * s, 4.6 * s, 6.8 * s, 6.8 * s));

            Path2D shoulders = new Path2D.Double();
            shoulders.moveTo(5.5 * s, 20 * s);
            shoulders.curveTo(5.5 * s, 14.2 * s, 18.5 * s, 14.2 * s, 18.5 * s, 20 * s);
            g2.draw(shoulders);
        }
    }
}
