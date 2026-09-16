package view;

import controller.AuthController;
import controller.CustomerController;
import controller.HouseController;
import util.Icons;
import util.Permissions;
import util.Session;
import util.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 主界面。
 *
 * <p>按需求报告 R-002 第 5 轮决定采用三栏结构：
 * <ul>
 *   <li>顶部应用栏——系统名、当前用户与角色、退出登录</li>
 *   <li>左侧可折叠侧边栏——模块导航（展开 120px / 收起 38px）</li>
 *   <li>中部内容区——各模块页面，用 CardLayout 切换</li>
 *   <li>底部状态栏——记录数与当前角色</li>
 * </ul>
 *
 * <p>同时承载 R-001 的界面层权限控制：模块与按钮按当前用户角色启用或置灰。
 */
public class MainView extends JPanel {

    private static final String CARD_HOUSE = "house";
    private static final String CARD_CUSTOMER = "customer";

    /** 侧边栏选中项的背景：主色的极淡版本，避免与表格选中行抢视觉 */
    private static final Color SELECTED_BG = new Color(0xE8, 0xF0, 0xFB);

    private final AuthController authController;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentArea = new JPanel(cardLayout);
    private final JPanel sidebar = new JPanel();
    private final List<NavItem> navItems = new ArrayList<>();

    private final JLabel userLabel = new JLabel();
    private final JLabel roleLabel = new JLabel();
    private final JLabel statusLabel = new JLabel("就绪");
    private final JButton collapseButton = new JButton();

    private final HouseView houseView;
    private final CustomerView customerView;

    private Runnable onLogout;
    private boolean sidebarCollapsed;

    public MainView(AuthController authController,
                    HouseController houseController,
                    CustomerController customerController) {
        this.authController = authController;

        setLayout(new BorderLayout());

        // 内容区：两个模块页面
        houseView = new HouseView(houseController, this::setStatus);
        customerView = new CustomerView(customerController, this::setStatus);
        contentArea.add(houseView, CARD_HOUSE);
        contentArea.add(customerView, CARD_CUSTOMER);
        contentArea.setBackground(Theme.PAGE_BG);

        add(createAppBar(), BorderLayout.NORTH);
        add(createCenter(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    /** 由 RealEstateSystem 注入退出登录后的动作（隐藏本窗口、重新显示登录窗口） */
    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    /** 供子页面更新状态栏左侧文字，例如「共 4 条记录」 */
    public void setStatus(String text) {
        statusLabel.setText(text == null || text.isEmpty() ? "就绪" : text);
    }

    // ------------------------------------------------------------ 顶部应用栏

    private JPanel createAppBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.ACCENT);
        bar.setBorder(new EmptyBorder(10, 18, 10, 14));

        JLabel systemName = new JLabel("二手房中介管理系统");
        systemName.setFont(Theme.FONT_SUBTITLE);
        systemName.setForeground(Color.WHITE);
        bar.add(systemName, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        right.setOpaque(false);

        userLabel.setFont(Theme.FONT_CAPTION);
        userLabel.setForeground(Theme.ACCENT_LIGHT);

        JButton logoutButton = new JButton("退出登录");
        logoutButton.setFont(Theme.FONT_CAPTION);
        logoutButton.setBackground(Theme.ACCENT_DARK);
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutButton.setFocusPainted(false);
        logoutButton.setBorder(new EmptyBorder(5, 14, 5, 14));
        logoutButton.addActionListener(e -> logout());

        right.add(userLabel);
        right.add(logoutButton);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ------------------------------------------------------ 侧边栏 + 内容区

    private JPanel createCenter() {
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Theme.PAGE_BG);

        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.SUBTLE_BG);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));

        // 折叠开关做成无边框透明样式，否则会渲染成一个浮在侧边栏上的小方块
        collapseButton.putClientProperty("JButton.buttonType", "borderless");
        collapseButton.setContentAreaFilled(false);
        collapseButton.setFont(Theme.FONT_CAPTION);
        collapseButton.setForeground(Theme.TEXT_SECONDARY);
        collapseButton.setFocusPainted(false);
        collapseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        collapseButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        collapseButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        collapseButton.setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 28));
        collapseButton.setBorder(new EmptyBorder(2, 0, 2, 0));
        collapseButton.setToolTipText("收起 / 展开侧边栏");
        collapseButton.addActionListener(e -> toggleSidebar());

        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(collapseButton);
        sidebar.add(Box.createVerticalStrut(8));

        navItems.add(new NavItem("房屋管理", true, CARD_HOUSE, Permissions.HOUSE_VIEW));
        navItems.add(new NavItem("客户管理", false, CARD_CUSTOMER, Permissions.CUSTOMER_VIEW));
        for (NavItem item : navItems) {
            sidebar.add(item);
        }

        applySidebarState();

        center.add(sidebar, BorderLayout.WEST);
        center.add(contentArea, BorderLayout.CENTER);
        return center;
    }

    private void toggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;
        applySidebarState();
    }

    private void applySidebarState() {
        int width = sidebarCollapsed ? Theme.SIDEBAR_COLLAPSED_WIDTH : Theme.SIDEBAR_WIDTH;
        sidebar.setPreferredSize(new Dimension(width, 0));

        collapseButton.setText(sidebarCollapsed ? "»" : "«");
        for (NavItem item : navItems) {
            item.setPreferredSize(new Dimension(width, 40));
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            item.repaint();
        }

        sidebar.revalidate();
        sidebar.repaint();
    }

    // ------------------------------------------------------------ 底部状态栏

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.SUBTLE_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(6, 18, 6, 18)));

        statusLabel.setFont(Theme.FONT_CAPTION);
        statusLabel.setForeground(Theme.TEXT_SECONDARY);

        roleLabel.setFont(Theme.FONT_CAPTION);
        roleLabel.setForeground(Theme.TEXT_SECONDARY);

        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(roleLabel, BorderLayout.EAST);
        return bar;
    }

    // -------------------------------------------------------------- 对外方法

    /**
     * 登录成功后调用：按当前用户角色应用权限，并显示第一个可用模块。
     *
     * <p>本方法必须可重复调用——MainView 是复用实例，退出后再次登录仍走同一个对象，
     * 若不可重复调用，第二次登录会残留上一次的权限状态。
     */
    public void showMainView() {
        userLabel.setText(Session.currentUserLabel());
        roleLabel.setText("当前角色：" + Session.currentRoleName());

        for (NavItem item : navItems) {
            item.setVisible(Session.can(item.permission));
        }

        // 两个子视图是在登录之前构造的，那时 Session 里还没有用户，
        // 删除按钮必然处于禁用态。这里按已登录的角色重新应用一次按钮权限。
        // 漏掉这一步会导致：无论用哪个账号登录，删除按钮都一直是灰的。
        houseView.applyPermissions();
        customerView.applyPermissions();

        NavItem firstAllowed = null;
        for (NavItem item : navItems) {
            if (item.isVisible()) {
                firstAllowed = item;
                break;
            }
        }

        if (firstAllowed == null) {
            // 理论上不会发生：两个角色都至少有查看权限
            setStatus("当前账号没有任何可访问的模块");
            return;
        }

        firstAllowed.selected = true;
        firstAllowed.repaint();
        cardLayout.show(contentArea, firstAllowed.cardName);
        refreshCard(firstAllowed.cardName);
    }

    private void refreshCard(String cardName) {
        if (CARD_HOUSE.equals(cardName)) {
            houseView.refresh();
        } else if (CARD_CUSTOMER.equals(cardName)) {
            customerView.refresh();
        }
    }

    private void logout() {
        authController.logout();

        // 清空界面上的用户状态，避免残留
        userLabel.setText("未登录");
        roleLabel.setText("");
        setStatus("就绪");
        for (NavItem item : navItems) {
            item.selected = false;
            item.repaint();
        }

        // Session 已清空，按钮权限同步复位为禁用，避免残留上一次登录的状态
        houseView.applyPermissions();
        customerView.applyPermissions();

        JOptionPane.showMessageDialog(this, "您已成功退出系统", "退出",
                JOptionPane.INFORMATION_MESSAGE);

        if (onLogout != null) {
            onLogout.run();
        }
    }

    /** 侧边栏导航项。自行绘制，以便精确控制选中 / 悬停 / 收起三种形态 */
    private final class NavItem extends JPanel {

        private final String title;
        private final boolean houseIcon;
        private final String cardName;
        private final String permission;

        private boolean selected;
        private boolean hovered;

        private NavItem(String title, boolean houseIcon, String cardName, String permission) {
            this.title = title;
            this.houseIcon = houseIcon;
            this.cardName = cardName;
            this.permission = permission;

            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(title);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    select();
                }
            });
        }

        private void select() {
            for (NavItem item : navItems) {
                item.selected = false;
                item.repaint();
            }
            selected = true;
            repaint();
            cardLayout.show(contentArea, cardName);
            refreshCard(cardName);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                if (selected) {
                    g2.setColor(SELECTED_BG);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(Theme.ACCENT);
                    g2.fillRect(0, 0, 3, getHeight());
                } else if (hovered) {
                    g2.setColor(Theme.HOVER_BG);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }

                Color foreground = selected ? Theme.ACCENT : Theme.TEXT_SECONDARY;
                Icon icon = houseIcon
                        ? Icons.house(foreground, Theme.ICON_SIZE)
                        : Icons.person(foreground, Theme.ICON_SIZE);

                int iconX = sidebarCollapsed ? (getWidth() - icon.getIconWidth()) / 2 : 14;
                int iconY = (getHeight() - icon.getIconHeight()) / 2;
                icon.paintIcon(this, g2, iconX, iconY);

                if (!sidebarCollapsed) {
                    g2.setFont(Theme.FONT_BODY);
                    g2.setColor(foreground);
                    FontMetrics metrics = g2.getFontMetrics();
                    int baseline = (getHeight() + metrics.getAscent() - metrics.getDescent()) / 2;
                    g2.drawString(title, iconX + icon.getIconWidth() + 9, baseline);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
