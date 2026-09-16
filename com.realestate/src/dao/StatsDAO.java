package dao;

import model.Overview;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 统计查询。对应需求报告 G-009（概览页）。
 *
 * <p>全部查询共用一条连接，避免为一个页面反复建立连接。
 * 这些语句都是只读聚合，不涉及事务。
 */
public class StatsDAO {

    private static final String COUNT_HOUSES = "SELECT COUNT(*) FROM houses";
    private static final String COUNT_CUSTOMERS = "SELECT COUNT(*) FROM customers";
    private static final String COUNT_LANDLORDS = "SELECT COUNT(*) FROM landlords";
    private static final String AVERAGE_AREA = "SELECT AVG(area) FROM houses";
    private static final String TYPE_DISTRIBUTION =
            "SELECT type, COUNT(*) AS total FROM houses GROUP BY type ORDER BY total DESC, type";

    /** 一次性取回概览页所需的全部统计值 */
    public Overview loadOverview() {
        try (Connection conn = DatabaseUtil.getConnection()) {
            int houseCount = count(conn, COUNT_HOUSES);
            int customerCount = count(conn, COUNT_CUSTOMERS);
            int landlordCount = count(conn, COUNT_LANDLORDS);
            double averageArea = averageArea(conn);
            List<Overview.TypeCount> typeCounts = typeDistribution(conn);

            return new Overview(houseCount, customerCount, landlordCount, averageArea, typeCounts);

        } catch (SQLException e) {
            System.err.println("统计查询失败: " + e.getMessage());
            e.printStackTrace();
            return Overview.empty();
        }
    }

    private int count(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** 房屋表为空时 AVG 返回 NULL，需用 wasNull 区分「0 套房的平均值」与「平均值为 0」 */
    private double averageArea(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(AVERAGE_AREA);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                double value = rs.getDouble(1);
                return rs.wasNull() ? 0 : value;
            }
            return 0;
        }
    }

    private List<Overview.TypeCount> typeDistribution(Connection conn) throws SQLException {
        List<Overview.TypeCount> result = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(TYPE_DISTRIBUTION);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(new Overview.TypeCount(rs.getString("type"), rs.getInt("total")));
            }
        }
        return result;
    }
}
