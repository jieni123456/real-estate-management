package dao;

import util.SecurityUtil;

import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseUtil {
    // 修改为您的实际数据库密码
    private static final String DB_URL = "jdbc:mysql://localhost:3306/real_estate_db?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "123456";

    static {
        try {
            // 显式加载驱动类
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL JDBC 驱动加载成功!");
        } catch (ClassNotFoundException e) {
            System.err.println("找不到 MySQL JDBC 驱动!");
            e.printStackTrace();
            showErrorDialog("找不到 MySQL JDBC 驱动: " + e.getMessage());
            throw new RuntimeException("找不到 MySQL JDBC 驱动", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        System.out.println("连接数据库: " + DB_URL);
        try {
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            System.err.println("数据库连接失败: " + e.getMessage());
            showErrorDialog("数据库连接失败: " + e.getMessage());
            throw e;
        }
    }

    public static void initializeDatabase() {
        System.out.println("开始初始化数据库...");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 创建数据库（如果不存在）
            stmt.execute("CREATE DATABASE IF NOT EXISTS real_estate_db");
            stmt.execute("USE real_estate_db");

            // 创建用户表
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "username VARCHAR(50) PRIMARY KEY, " +
                    "encrypted_password VARCHAR(100) NOT NULL, " +
                    "role VARCHAR(20) NOT NULL)");

            // 创建房东表
            stmt.execute("CREATE TABLE IF NOT EXISTS landlords (" +
                    "id VARCHAR(50) PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "encrypted_contact TEXT NOT NULL)");

            // 创建房屋表
            stmt.execute("CREATE TABLE IF NOT EXISTS houses (" +
                    "id VARCHAR(50) PRIMARY KEY, " +
                    "type VARCHAR(50) NOT NULL, " +
                    "area DOUBLE NOT NULL, " +
                    "address VARCHAR(255) NOT NULL, " +
                    "landlord_id VARCHAR(50) NOT NULL, " +
                    "FOREIGN KEY (landlord_id) REFERENCES landlords(id))");

            // 创建客户表
            stmt.execute("CREATE TABLE IF NOT EXISTS customers (" +
                    "id VARCHAR(50) PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "phone VARCHAR(20) NOT NULL, " +
                    "requirements TEXT)");

            // 添加默认用户
            String adminPass = SecurityUtil.encryptPassword("admin123");
            String agentPass = SecurityUtil.encryptPassword("agent456");

            System.out.println("admin123 加密: " + adminPass);
            System.out.println("agent456 加密: " + agentPass);

            stmt.executeUpdate("INSERT IGNORE INTO users VALUES " +
                    "('admin', '" + adminPass + "', 'ADMIN'), " +
                    "('agent', '" + agentPass + "', 'AGENT')");

            System.out.println("数据库初始化成功!");

        } catch (SQLException e) {
            System.err.println("数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("数据库初始化失败: " + e.getMessage());
        }
    }

    private static void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(null,
                message,
                "数据库错误",
                JOptionPane.ERROR_MESSAGE);
    }
}