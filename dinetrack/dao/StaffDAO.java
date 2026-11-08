package dinetrack.dao;

import dinetrack.db.Database;
import dinetrack.exception.DataAccessException;

import java.sql.*;
import java.util.*;

public class StaffDAO {
    public static void add(String name) {
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement("INSERT INTO staff(name) VALUES (?)")) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error adding staff: " + e.getMessage(), e);
        }
    }

    public static void deactivate(String name) {
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement("UPDATE staff SET active=FALSE WHERE name=?")) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error deactivating staff: " + e.getMessage(), e);
        }
    }

    public static List<String> activeNames() {
        List<String> names = new ArrayList<>();
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT name FROM staff WHERE active=TRUE ORDER BY name");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                names.add(rs.getString(1));
            return names;
        } catch (SQLException e) {
            throw new DataAccessException("Error loading staff: " + e.getMessage(), e);
        }
    }
}

