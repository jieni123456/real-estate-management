import controller.AuthController;
import controller.CustomerController;
import controller.HouseController;
import dao.DatabaseUtil;
import view.LoginView;
import view.MainView;

import javax.swing.*;

public class RealEstateSystem {
    public static void main(String[] args) {
        // 初始化数据库
        DatabaseUtil.initializeDatabase();

        // 创建控制器
        AuthController authController = new AuthController();
        HouseController houseController = new HouseController();
        CustomerController customerController = new CustomerController();

        // 创建登录窗口
        JFrame loginFrame = new JFrame("登录");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(600, 400);
        loginFrame.setLocationRelativeTo(null);

        // 创建主窗口
        JFrame mainFrame = new JFrame("二手房中介管理系统");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(900, 650);
        mainFrame.setLocationRelativeTo(null);

        // 创建主视图面板（不是JFrame）
        MainView mainView = new MainView(authController, houseController, customerController);
        mainFrame.add(mainView);
        mainFrame.setVisible(false); // 初始不显示

        // 创建登录视图
        LoginView loginView = new LoginView(authController, () -> {
            // 登录成功后隐藏登录窗口，显示主窗口
            loginFrame.setVisible(false);
            mainFrame.setVisible(true);
            mainView.showMainView(); // 启用主视图的功能
        });

        loginFrame.add(loginView);
        loginFrame.setVisible(true);
    }
}