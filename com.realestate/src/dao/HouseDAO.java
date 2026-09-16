package dao;

import model.House;
import model.Landlord;
import util.SecurityUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 房屋与房东的数据访问。
 *
 * <p>对应需求报告阶段二的三项改动：
 * <ul>
 *   <li>G-013  返回强类型 {@link House}，不再返回 {@code Object[]}</li>
 *   <li>G-001  新增走纯 INSERT，ID 冲突即失败，<b>不再静默覆盖</b>已有记录</li>
 *   <li>G-010  房东与房屋两条写入包在同一事务中，避免出现孤儿数据</li>
 * </ul>
 *
 * <p><b>房东信息的处理原则：INSERT IGNORE——不存在则创建，已存在则沿用原信息，
 * 绝不覆盖。</b>因为一个房东可能关联多套房屋，凭一次表单提交改写房东资料会连带
 * 影响其它房屋显示出来的房东信息。（房东的独立维护见缺口 G-007。）
 */
public class HouseDAO {

    private static final String LANDLORD_SQL =
            "INSERT IGNORE INTO landlords (id, name, encrypted_contact) VALUES (?, ?, ?)";

    private static final String INSERT_HOUSE_SQL =
            "INSERT INTO houses (id, type, area, address, landlord_id) VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE_HOUSE_SQL =
            "UPDATE houses SET type = ?, area = ?, address = ?, landlord_id = ? WHERE id = ?";

    private static final String SELECT_ALL_SQL =
            "SELECT h.id, h.type, h.area, h.address, "
                    + "l.id AS landlord_id, l.name AS landlord_name, l.encrypted_contact "
                    + "FROM houses h JOIN landlords l ON h.landlord_id = l.id "
                    + "ORDER BY h.id";

    /** 房屋 ID 是否已存在。供「新增 / 编辑」区分与冲突提示使用 */
    public boolean exists(String houseId) {
        String sql = "SELECT 1 FROM houses WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, houseId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("校验房屋ID是否已存在时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /** 房东 ID 是否已存在。用于在成功提示里区分「新建房东」与「沿用已有房东」 */
    public boolean landlordExists(String landlordId) {
        String sql = "SELECT 1 FROM landlords WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, landlordId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("校验房东ID是否已存在时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /** 新增房屋。ID 已存在时返回 false，不修改原记录 */
    public boolean insertHouse(House house) {
        return write(house, false);
    }

    /** 更新房屋。记录不存在时返回 false */
    public boolean updateHouse(House house) {
        return write(house, true);
    }

    private boolean write(House house, boolean update) {
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            // G-010：房东与房屋两条写入必须同生共死。
            // 否则第二条失败时第一条已提交，库里会留下「有房东、无房屋」的孤儿数据。
            conn.setAutoCommit(false);

            try (PreparedStatement landlordStmt = conn.prepareStatement(LANDLORD_SQL)) {
                landlordStmt.setString(1, house.getLandlord().getId());
                landlordStmt.setString(2, house.getLandlord().getName());
                landlordStmt.setString(3,
                        SecurityUtil.encryptContact(house.getLandlord().getContact()));
                landlordStmt.executeUpdate();
            }

            int affected;
            try (PreparedStatement houseStmt =
                         conn.prepareStatement(update ? UPDATE_HOUSE_SQL : INSERT_HOUSE_SQL)) {
                if (update) {
                    houseStmt.setString(1, house.getType());
                    houseStmt.setDouble(2, house.getArea());
                    houseStmt.setString(3, house.getAddress());
                    houseStmt.setString(4, house.getLandlord().getId());
                    houseStmt.setString(5, house.getId());
                } else {
                    houseStmt.setString(1, house.getId());
                    houseStmt.setString(2, house.getType());
                    houseStmt.setDouble(3, house.getArea());
                    houseStmt.setString(4, house.getAddress());
                    houseStmt.setString(5, house.getLandlord().getId());
                }
                affected = houseStmt.executeUpdate();
            }

            conn.commit();
            System.out.println((update ? "更新" : "新增") + "房屋成功: " + house.getId());
            return affected > 0;

        } catch (SQLException e) {
            rollbackQuietly(conn);
            System.err.println((update ? "更新" : "新增") + "房屋失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

    public List<House> getAllHouses() {
        List<House> houses = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Landlord landlord = new Landlord(
                        rs.getString("landlord_id"),
                        rs.getString("landlord_name"),
                        SecurityUtil.decryptContact(rs.getString("encrypted_contact")));

                houses.add(new House(
                        rs.getString("id"),
                        rs.getString("type"),
                        rs.getDouble("area"),
                        rs.getString("address"),
                        landlord));
            }
        } catch (SQLException e) {
            System.err.println("查询房屋列表失败: " + e.getMessage());
            e.printStackTrace();
        }
        return houses;
    }

    public boolean deleteHouse(String houseId) {
        String sql = "DELETE FROM houses WHERE id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, houseId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("删除房屋失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ------------------------------------------------------------ 事务辅助

    private void rollbackQuietly(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException e) {
            System.err.println("回滚事务失败: " + e.getMessage());
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.close();
        } catch (SQLException e) {
            System.err.println("关闭连接失败: " + e.getMessage());
        }
    }
}
