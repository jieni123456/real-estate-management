package view;

import controller.AuthController;
import controller.CustomerController;
import controller.HouseController;
import javax.swing.*;
import java.awt.*;

public class MainView extends JPanel {  // 修改为继承自JPanel
    private final AuthController authController;
    private final HouseController houseController;
    private final CustomerController customerController;

    private final JTabbedPane tabbedPane;
    private final JButton logoutButton;

    public MainView(AuthController authController,
                    HouseController houseController,
                    CustomerController customerController) {
        this.authController = authController;
        this.houseController = houseController;
        this.customerController = customerController;

        setLayout(new BorderLayout());  // 设置布局管理器

        // 创建选项卡面板
        tabbedPane = new JTabbedPane();

        // 创建房屋管理视图
        HouseView houseView = new HouseView(houseController);
        tabbedPane.addTab("房屋管理", houseView);

        // 创建客户管理视图
        CustomerView customerView = new CustomerView(customerController);
        tabbedPane.addTab("客户管理", customerView);

        // 初始禁用管理选项卡
        tabbedPane.setEnabledAt(0, false);
        tabbedPane.setEnabledAt(1, false);

        // 创建登出按钮
        logoutButton = new JButton("退出登录");
        logoutButton.setEnabled(false);
        logoutButton.addActionListener(e -> logout());

        // 添加组件到主界面
        add(tabbedPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(logoutButton);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void showMainView() {
        tabbedPane.setEnabledAt(0, true);
        tabbedPane.setEnabledAt(1, true);
        tabbedPane.setSelectedIndex(0);
        logoutButton.setEnabled(true);
    }

    private void logout() {
        authController.logout();
        tabbedPane.setEnabledAt(0, false);
        tabbedPane.setEnabledAt(1, false);
        logoutButton.setEnabled(false);
        JOptionPane.showMessageDialog(this, "您已成功退出系统", "退出", JOptionPane.INFORMATION_MESSAGE);
    }
}