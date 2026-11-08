package dinetrack.dao;

import dinetrack.db.Database;
import dinetrack.exception.DataAccessException;
import dinetrack.model.Food;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class FoodDAO {
    public boolean adjustStock(int foodId, int delta) {
        String sql = "UPDATE foods SET stock_quantity=stock_quantity+? WHERE food_id=? AND stock_quantity+?>=0";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, foodId);
            ps.setInt(3, delta);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error adjusting stock: " + e.getMessage(), e);
        }
    }

    public int add(Food f, BigDecimal cost, int stock, int reorderLevel) {
        String sql = "INSERT INTO foods (name,category,price,cost_price,stock_quantity,reorder_level,available) VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, f.getName());
            ps.setString(2, f.getCategory());
            ps.setBigDecimal(3, f.getPrice());
            ps.setBigDecimal(4, cost);
            ps.setInt(5, stock);
            ps.setInt(6, reorderLevel);
            ps.setBoolean(7, f.isAvailable());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getInt(1);
            }
            throw new SQLException("No generated menu item ID.");
        } catch (SQLException e) {
            throw new DataAccessException("Error adding menu item: " + e.getMessage(), e);
        }
    }

    public boolean updateInventorySettings(int id, BigDecimal cost, int reorderLevel) {
        String sql = "UPDATE foods SET cost_price=?, reorder_level=? WHERE food_id=?";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, cost);
            ps.setInt(2, reorderLevel);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error updating inventory settings: " + e.getMessage(), e);
        }
    }

    public boolean receiveStock(int foodId, int quantity) {
        String sql = "UPDATE foods SET stock_quantity = stock_quantity + ?, available = TRUE WHERE food_id = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, foodId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error receiving stock: " + e.getMessage(), e);
        }
    }

    public int add(Food f) {
        String sql = "INSERT INTO foods (name, category, price, available) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, f.getName());
            ps.setString(2, f.getCategory());
            ps.setBigDecimal(3, f.getPrice());
            ps.setBoolean(4, f.isAvailable());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getInt(1);
            }
            throw new SQLException("Failed to retrieve generated Food ID.");
        } catch (SQLException e) {
            throw new DataAccessException("Error adding menu item: " + e.getMessage(), e);
        }
    }

    public List<Food> getAll(boolean availableOnly) {
        List<Food> list = new ArrayList<>();
        String sql = "SELECT food_id, name, category, price, available FROM foods" +
                (availableOnly ? " WHERE available = TRUE AND stock_quantity > 0" : "") + " ORDER BY category, name";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Food(rs.getInt("food_id"), rs.getString("name"),
                        rs.getString("category"), rs.getBigDecimal("price"), rs.getBoolean("available")));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error loading food menu: " + e.getMessage(), e);
        }
    }

    public Food getById(int id) {
        String sql = "SELECT food_id, name, category, price, available FROM foods WHERE food_id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Food(rs.getInt("food_id"), rs.getString("name"),
                            rs.getString("category"), rs.getBigDecimal("price"), rs.getBoolean("available"));
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Error finding food #" + id + ": " + e.getMessage(), e);
        }
    }

    public boolean update(Food f) {
        String sql = "UPDATE foods SET name = ?, category = ?, price = ?, available = ? WHERE food_id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, f.getName());
            ps.setString(2, f.getCategory());
            ps.setBigDecimal(3, f.getPrice());
            ps.setBoolean(4, f.isAvailable());
            ps.setInt(5, f.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error updating food item: " + e.getMessage(), e);
        }
    }

    public boolean toggleAvailability(int id) {
        String sql = "UPDATE foods SET available = NOT available WHERE food_id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error updating item availability: " + e.getMessage(), e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM foods WHERE food_id = ?";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) {
                throw new DataAccessException(
                        "Cannot delete menu item #" + id + " because it is part of existing orders.", e);
            }
            throw new DataAccessException("Error deleting menu item: " + e.getMessage(), e);
        }
    }
}

