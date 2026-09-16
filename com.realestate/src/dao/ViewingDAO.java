package dao;

import model.Viewing;
import util.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 带看记录的数据访问。对应需求报告 G-008。
 *
 * <p>查询时 JOIN 客户与房屋表，把客户姓名与房屋地址一并取出——列表要显示这两个
 * 人看得懂的信息，只给 ID 毫无意义。
 *
 * <p>时间字段用 {@code setObject(LocalDateTime)} / {@code getObject(..., LocalDateTime.class)}
 * 收发，而不是 Timestamp：后者会经过 JVM 默认时区换算，再叠加连接串里的
 * serverTimezone，容易出现「写进去和读出来差 8 小时」这类问题。
 * JDBC 4.2 的 LocalDateTime 映射是字面值直传，与时区无关。
 */
public class ViewingDAO {

    private static final String SELECT_ALL_SQL =
            "SELECT v.id, v.customer_id, c.name AS customer_name, "
                    + "v.house_id, h.address AS house_address, "
                    + "v.viewed_at, v.result, v.note "
                    + "FROM viewings v "
                    + "JOIN customers c ON v.customer_id = c.id "
                    + "JOIN houses h ON v.house_id = h.id "
                    + "ORDER BY v.viewed_at DESC, v.id DESC";

    private static final String INSERT_SQL =
            "INSERT INTO viewings (customer_id, house_id, viewed_at, result, note) "
                    + "VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE viewings SET customer_id = ?, house_id = ?, viewed_at = ?, "
                    + "result = ?, note = ? WHERE id = ?";

    private static final String DELETE_SQL = "DELETE FROM viewings WHERE id = ?";

    private static final String COUNT_BY_HOUSE_SQL =
            "SELECT COUNT(*) FROM viewings WHERE house_id = ?";

    private static final String COUNT_BY_CUSTOMER_SQL =
            "SELECT COUNT(*) FROM viewings WHERE customer_id = ?";

    public List<Viewing> getAll() {
        List<Viewing> viewings = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                viewings.add(read(rs));
            }
        } catch (SQLException e) {
            System.err.println("查询带看记录失败: " + e.getMessage());
            throw DataAccessException.from(e);
        }
        return viewings;
    }

    /** 新增。自增主键由数据库分配 */
    public boolean insert(Viewing viewing) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, viewing.getCustomerId());
            stmt.setString(2, viewing.getHouseId());
            stmt.setObject(3, viewing.getViewedAt());
            stmt.setString(4, viewing.getResult());
            stmt.setString(5, viewing.getNote());

            int affected = stmt.executeUpdate();
            System.out.println("新增带看记录成功，受影响行数: " + affected);
            return affected > 0;

        } catch (SQLException e) {
            System.err.println("新增带看记录失败: " + e.getMessage());
            throw DataAccessException.from(e);
        }
    }

    public boolean update(Viewing viewing) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, viewing.getCustomerId());
            stmt.setString(2, viewing.getHouseId());
            stmt.setObject(3, viewing.getViewedAt());
            stmt.setString(4, viewing.getResult());
            stmt.setString(5, viewing.getNote());
            stmt.setLong(6, viewing.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("更新带看记录失败: " + e.getMessage());
            throw DataAccessException.from(e);
        }
    }

    public boolean delete(long id) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("删除带看记录失败: " + e.getMessage());
            throw DataAccessException.from(e);
        }
    }

    /** 某套房屋的带看记录条数。删除房屋前用它提示「将同时删除 N 条」 */
    public int countByHouse(String houseId) {
        return count(COUNT_BY_HOUSE_SQL, houseId);
    }

    /** 某位客户的带看记录条数。删除客户前用它提示「将同时删除 N 条」 */
    public int countByCustomer(String customerId) {
        return count(COUNT_BY_CUSTOMER_SQL, customerId);
    }

    private int count(String sql, String key) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw DataAccessException.from(e);
        }
    }

    private Viewing read(ResultSet rs) throws SQLException {
        LocalDateTime viewedAt = rs.getObject("viewed_at", LocalDateTime.class);
        return new Viewing(
                rs.getLong("id"),
                rs.getString("customer_id"),
                rs.getString("customer_name"),
                rs.getString("house_id"),
                rs.getString("house_address"),
                viewedAt,
                rs.getString("result"),
                rs.getString("note"));
    }
}
