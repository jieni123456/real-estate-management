package dao;

import util.SecurityUtil;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * 数据库连接工具。
 *
 * <p>配置读取优先级（从高到低）：
 * <ol>
 *   <li>环境变量 {@code DB_URL} / {@code DB_USER} / {@code DB_PASSWORD}</li>
 *   <li>外部配置文件 {@code db.properties}（已被 .gitignore 排除，不会进版本库）</li>
 *   <li>内置默认值</li>
 * </ol>
 *
 * <p>{@code db.properties} 查找顺序：{@code -Ddb.config=<路径>} 指定的文件 →
 * 运行目录 → 上一级目录 → {@code config/} 子目录 → classpath 根目录。
 * 可复制 {@code db.properties.example} 为 {@code db.properties} 后填写真实密码。
 */
public class DatabaseUtil {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/real_estate_db?useSSL=false&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";

    private static final String DB_URL;
    private static final String DB_USER;
    private static final String DB_PASSWORD;

    static {
        Properties fileProps = loadFileProperties();

        DB_URL = firstNonBlank(System.getenv("DB_URL"), fileProps.getProperty("db.url"), DEFAULT_URL);
        DB_USER = firstNonBlank(System.getenv("DB_USER"), fileProps.getProperty("db.user"), DEFAULT_USER);
        DB_PASSWORD = firstNonBlank(System.getenv("DB_PASSWORD"), fileProps.getProperty("db.password"), "");

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

    /** 按既定顺序查找并读取 db.properties，全部不存在时返回空配置 */
    private static Properties loadFileProperties() {
        Properties props = new Properties();

        Path[] candidates = {
                pathOrNull(System.getProperty("db.config")),
                Paths.get("db.properties"),
                Paths.get("..", "db.properties"),
                Paths.get("config", "db.properties")
        };

        for (Path candidate : candidates) {
            if (candidate != null && Files.isRegularFile(candidate)) {
                try (Reader reader = Files.newBufferedReader(candidate, StandardCharsets.UTF_8)) {
                    props.load(reader);
                    System.out.println("已加载数据库配置: " + candidate.toAbsolutePath());
                    return props;
                } catch (IOException e) {
                    System.err.println("读取配置文件失败: " + candidate + " -> " + e.getMessage());
                }
            }
        }

        // 兜底：classpath 下的 db.properties（打成 jar 后仍然可用）
        try (InputStream in = DatabaseUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                System.out.println("已加载 classpath 下的数据库配置");
            }
        } catch (IOException e) {
            System.err.println("读取 classpath 配置失败: " + e.getMessage());
        }

        return props;
    }

    private static Path pathOrNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : Paths.get(value.trim());
    }

    /** 返回第一个非空白值；若均空白则返回最后一个参数作为兜底值 */
    private static String firstNonBlank(String... values) {
        for (int i = 0; i < values.length - 1; i++) {
            String value = values[i];
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return values[values.length - 1];
    }

    public static Connection getConnection() throws SQLException {
        System.out.println("连接数据库: " + DB_URL + " (用户: " + DB_USER + ")");
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
