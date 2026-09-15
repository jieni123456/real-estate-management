package dao;

import model.User;
import util.SecurityUtil;

import java.sql.*;

public class UserDAO {
    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND encrypted_password = ?";
        System.out.println("尝试登录: " + username);

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String encryptedPassword = SecurityUtil.encryptPassword(password);
            System.out.println("输入密码加密: " + encryptedPassword);

            stmt.setString(1, username);
            stmt.setString(2, encryptedPassword);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("登录成功: " + username);
                    return new User(
                            rs.getString("username"),
                            "", // 不返回实际密码
                            rs.getString("role")
                    );
                } else {
                    System.out.println("登录失败: 用户名或密码错误");
                    return null;
                }
            }
        } catch (SQLException e) {
            System.err.println("数据库查询错误: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}