package dao;


import model.Customer;
import java.sql.*;
        import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {
    public boolean saveCustomer(Customer customer) {
        String sql = "INSERT INTO customers (id, name, phone, requirements) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE name = ?, phone = ?, requirements = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, customer.getId());
            stmt.setString(2, customer.getName());
            stmt.setString(3, customer.getPhone());
            stmt.setString(4, customer.getRequirements());
            stmt.setString(5, customer.getName());
            stmt.setString(6, customer.getPhone());
            stmt.setString(7, customer.getRequirements());

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Object[]> getAllCustomers() {
        List<Object[]> customers = new ArrayList<>();
        String sql = "SELECT * FROM customers";

        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                customers.add(new Object[]{
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("requirements")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customers;
    }

    public boolean deleteCustomer(String customerId) {
        String sql = "DELETE FROM customers WHERE id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, customerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}