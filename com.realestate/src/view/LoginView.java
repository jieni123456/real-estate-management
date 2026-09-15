package view;


import controller.AuthController;
import javax.swing.*;
        import java.awt.*;

public class LoginView extends JPanel {
    private final AuthController authController;
    private final Runnable onLoginSuccess;

    public LoginView(AuthController authController, Runnable onLoginSuccess) {
        this.authController = authController;
        this.onLoginSuccess = onLoginSuccess;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new GridBagLayout());
        setBackground(new Color(240, 248, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 标题
        JLabel titleLabel = new JLabel("二手房中介管理系统", JLabel.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 28));
        titleLabel.setForeground(new Color(25, 25, 112));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(titleLabel, gbc);

        // 表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(240, 248, 255));
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180)), "用户登录",
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 16),
                new Color(70, 130, 180)));

        GridBagConstraints fgbc = new GridBagConstraints();
        fgbc.insets = new Insets(10, 10, 10, 10);
        fgbc.fill = GridBagConstraints.HORIZONTAL;

        // 用户名字段
        JLabel userLabel = new JLabel("用户名:");
        userLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        fgbc.gridx = 0;
        fgbc.gridy = 0;
        formPanel.add(userLabel, fgbc);

        JTextField userField = new JTextField(20);
        userField.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        fgbc.gridx = 1;
        fgbc.gridy = 0;
        formPanel.add(userField, fgbc);

        // 密码字段
        JLabel passLabel = new JLabel("密码:");
        passLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        fgbc.gridx = 0;
        fgbc.gridy = 1;
        formPanel.add(passLabel, fgbc);

        JPasswordField passField = new JPasswordField(20);
        passField.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        fgbc.gridx = 1;
        fgbc.gridy = 1;
        formPanel.add(passField, fgbc);

        // 登录按钮
        JButton loginButton = new JButton("登录");
        loginButton.setFont(new Font("微软雅黑", Font.BOLD, 16));
        loginButton.setBackground(new Color(70, 130, 180));
        loginButton.setForeground(Color.WHITE);
        fgbc.gridx = 0;
        fgbc.gridy = 2;
        fgbc.gridwidth = 2;
        fgbc.fill = GridBagConstraints.NONE;
        loginButton.addActionListener(e -> {
            String username = userField.getText();
            String password = new String(passField.getPassword());

            if (authController.login(username, password) != null) {
                onLoginSuccess.run();
            } else {
                JOptionPane.showMessageDialog(this, "用户名或密码错误", "登录失败", JOptionPane.ERROR_MESSAGE);
            }
        });
        formPanel.add(loginButton, fgbc);

        gbc.gridy = 1;
        gbc.gridwidth = 2;
        add(formPanel, gbc);
    }
}