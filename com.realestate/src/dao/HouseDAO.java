package dao;

import model.House;
import model.Landlord;
import util.SecurityUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HouseDAO {
    public boolean saveHouse(House house) {
        String landlordSql = "INSERT INTO landlords (id, name, encrypted_contact) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE name = ?, encrypted_contact = ?";

        String houseSql = "INSERT INTO houses (id, type, area, address, landlord_id) VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE type = ?, area = ?, address = ?, landlord_id = ?";

        try (Connection conn = DatabaseUtil.getConnection()) {
            // 保存房东信息
            try (PreparedStatement landlordStmt = conn.prepareStatement(landlordSql)) {
                String encryptedContact = SecurityUtil.encryptContact(house.getLandlord().getContact());

                landlordStmt.setString(1, house.getLandlord().getId());
                landlordStmt.setString(2, house.getLandlord().getName());
                landlordStmt.setString(3, encryptedContact);
                landlordStmt.setString(4, house.getLandlord().getName());
                landlordStmt.setString(5, encryptedContact);

                landlordStmt.executeUpdate();
                System.out.println("房东信息保存成功: " + house.getLandlord().getId());
            }

            // 保存房屋信息
            try (PreparedStatement houseStmt = conn.prepareStatement(houseSql)) {
                houseStmt.setString(1, house.getId());
                houseStmt.setString(2, house.getType());
                houseStmt.setDouble(3, house.getArea());
                houseStmt.setString(4, house.getAddress());
                houseStmt.setString(5, house.getLandlord().getId());
                houseStmt.setString(6, house.getType());
                houseStmt.setDouble(7, house.getArea());
                houseStmt.setString(8, house.getAddress());
                houseStmt.setString(9, house.getLandlord().getId());

                int result = houseStmt.executeUpdate();
                System.out.println("房屋信息保存成功: " + house.getId());
                return result > 0;
            }
        } catch (SQLException e) {
            System.err.println("保存房屋信息失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<Object[]> getAllHouses() {
        List<Object[]> houses = new ArrayList<>();
        String sql = "SELECT h.id, h.type, h.area, h.address, l.id AS landlord_id, l.name, l.encrypted_contact " +
                "FROM houses h JOIN landlords l ON h.landlord_id = l.id";

        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String contact = SecurityUtil.decryptContact(rs.getString("encrypted_contact"));
                houses.add(new Object[]{
                        rs.getString("id"),
                        rs.getString("type"),
                        rs.getDouble("area"),
                        rs.getString("address"),
                        rs.getString("landlord_id"),
                        rs.getString("name"),
                        contact
                });
            }
        } catch (SQLException e) {
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
            e.printStackTrace();
        }
        return false;
    }
}