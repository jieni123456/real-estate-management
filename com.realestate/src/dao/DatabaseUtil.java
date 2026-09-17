package dao;

import util.AppConfig;
import util.SecurityUtil;

import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 数据库连接工具。
 *
 * <p>连接参数的读取交给 {@link AppConfig}，优先级（从高到低）：
 * 系统属性 {@code db.url} / {@code db.user} / {@code db.password} →
 * 环境变量 {@code DB_URL} / {@code DB_USER} / {@code DB_PASSWORD} →
 * {@code db.properties} → 内置默认值。
 *
 * <p>{@code db.properties} 已被 .gitignore 排除，不会进版本库；模板见
 * {@code db.properties.example}。
 */
public class DatabaseUtil {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/real_estate_db?useSSL=false&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";

    private static final String DB_URL;
    private static final String DB_USER;
    private static final String DB_PASSWORD;

    static {
        DB_URL = AppConfig.get("db.url", "DB_URL", "db.url", DEFAULT_URL);
        DB_USER = AppConfig.get("db.user", "DB_USER", "db.user", DEFAULT_USER);
        DB_PASSWORD = AppConfig.get("db.password", "DB_PASSWORD", "db.password", "");

        try {
            // 显式加载驱动类
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL JDBC 驱动加载成功!");
        } catch (ClassNotFoundException e) {
            System.err.println("找不到 MySQL JDBC 驱动!");
            e.printStackTrace();
            showErrorDialog("找不到 MySQL JDBC 驱动: " + e.getMessage()
                    + "\n请确认 lib/mysql-connector-j-8.0.33.jar 已加入模块依赖。");
            throw new RuntimeException("找不到 MySQL JDBC 驱动", e);
        }

        if (DB_PASSWORD.isEmpty()) {
            System.out.println("[提示] 当前未配置数据库密码，请任选一种方式：");
            System.out.println("       1) 设置环境变量 DB_PASSWORD");
            System.out.println("       2) 复制 db.properties.example 为 db.properties 并填写密码");
        }
    }

    public static Connection getConnection() throws SQLException {
        System.out.println("连接数据库: " + DB_URL + " (用户: " + DB_USER + ")");
        try {
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            // 刻意不在这里弹模态框：连接失败的原因会被 DAO 归类为 DataAccessException，
            // 再由界面层转成用户能看懂的一句话（见需求报告 G-012）。
            // 顺带也避免了无头环境下弹框把线程卡住。
            System.err.println("数据库连接失败: " + e.getMessage());
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

            // 创建操作日志表（G-017）。
            // created_at 用数据库端默认值，取服务器时间，比客户端时间更可信。
            stmt.execute("CREATE TABLE IF NOT EXISTS operation_logs (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "operator VARCHAR(50) NOT NULL DEFAULT '', " +
                    "role VARCHAR(20) NOT NULL DEFAULT '', " +
                    "action VARCHAR(30) NOT NULL DEFAULT '', " +
                    "target VARCHAR(100) NOT NULL DEFAULT '', " +
                    "detail VARCHAR(255) NOT NULL DEFAULT '', " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "INDEX idx_operation_logs_created_at (created_at))");

            // 创建带看记录表（G-008）。这是「客户 → 带看 → 成交」这条业务链的载体，
            // 也是 customers 与 houses 两张表之间唯一的关联。
            //
            // 外键刻意用 ON DELETE CASCADE：房屋或客户被删除后，对应的带看记录已无意义。
            // 但级联删除不能是隐形的——界面在删除确认框里会明确提示「将同时删除 N 条
            // 带看记录」，让用户知道自己在删什么。
            stmt.execute("CREATE TABLE IF NOT EXISTS viewings (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "customer_id VARCHAR(50) NOT NULL, " +
                    "house_id VARCHAR(50) NOT NULL, " +
                    "viewed_at DATETIME NOT NULL, " +
                    "result VARCHAR(20) NOT NULL DEFAULT '意向中', " +
                    "note VARCHAR(255) NOT NULL DEFAULT '', " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (house_id) REFERENCES houses(id) ON DELETE CASCADE, " +
                    "INDEX idx_viewings_viewed_at (viewed_at))");

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
