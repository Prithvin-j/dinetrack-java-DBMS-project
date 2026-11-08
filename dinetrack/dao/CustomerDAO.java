package dinetrack.dao;

import dinetrack.db.Database;
import dinetrack.exception.DataAccessException;
import dinetrack.model.Customer;

import java.sql.*;
import java.util.*;

public class CustomerDAO {
    public boolean deactivate(int id) {
        String sql = "UPDATE customers SET active=FALSE WHERE customer_id=? AND active=TRUE";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error deactivating customer: " + e.getMessage(), e);
        }
    }

    public int add(Customer c) {
        String sql = "INSERT INTO customers (name, phone, email) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getEmail());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getInt(1);
            }
            throw new SQLException("Failed to retrieve generated Customer ID.");
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new DataAccessException("Customer with this phone or email already exists.", e);
            }
            throw new DataAccessException("Error adding customer: " + e.getMessage(), e);
        }
    }

    public List<Customer> getAll() {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT customer_id, name, phone, email FROM customers WHERE active=TRUE ORDER BY customer_id";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Customer(rs.getInt("customer_id"), rs.getString("name"),
                        rs.getString("phone"), rs.getString("email")));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error loading customers: " + e.getMessage(), e);
        }
    }

    public Customer getById(int id) {
        String sql = "SELECT customer_id, name, phone, email FROM customers WHERE customer_id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Customer(rs.getInt("customer_id"), rs.getString("name"),
                            rs.getString("phone"), rs.getString("email"));
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Error finding customer #" + id + ": " + e.getMessage(), e);
        }
    }

    public boolean update(Customer c) {
        String sql = "UPDATE customers SET name = ?, phone = ?, email = ? WHERE customer_id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getEmail());
            ps.setInt(4, c.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new DataAccessException("Another customer already has this phone or email.", e);
            }
            throw new DataAccessException("Error updating customer: " + e.getMessage(), e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM customers WHERE customer_id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) {
                throw new DataAccessException(
                        "Cannot delete customer #" + id + " because they have existing order records.", e);
            }
            throw new DataAccessException("Error deleting customer: " + e.getMessage(), e);
        }
    }

    public List<Customer> search(String query) {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT customer_id, name, phone, email FROM customers WHERE active=TRUE AND (name LIKE ? OR phone LIKE ? OR email LIKE ?) ORDER BY customer_id";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            String term = "%" + query + "%";
            ps.setString(1, term);
            ps.setString(2, term);
            ps.setString(3, term);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Customer(rs.getInt("customer_id"), rs.getString("name"),
                            rs.getString("phone"), rs.getString("email")));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error searching customers: " + e.getMessage(), e);
        }
    }
}

