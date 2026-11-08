package dinetrack.db;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static String host = "localhost";
    private static int port = 3306;
    private static String databaseName = "dinetrack";
    private static String user = "root";
    private static String password = "root";

    private Database() {
    }

    public static synchronized void setCredentials(String h, int p, String db, String u, String pwd) {
        host = h;
        port = p;
        databaseName = db;
        user = u;
        password = pwd;
    }

    public static synchronized void setPassword(String pwd) {
        password = pwd;
    }

    public static String getUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + databaseName
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), user, password);
    }

    public static boolean tryConnect(String pwd) {
        try (Connection conn = DriverManager.getConnection(getUrl(), user, pwd)) {
            if (conn != null && !conn.isClosed()) {
                ensureSchema(conn);
                return true;
            }
            return false;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1049) {
                return initDatabase(pwd);
            }
            return false;
        }
    }

    public static void ensureSchema(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            try {
                stmt.executeUpdate("ALTER TABLE customers ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE");
            } catch (SQLException ignored) {}

            try {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS staff ("
                        + "staff_id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "name VARCHAR(100) NOT NULL UNIQUE, "
                        + "active BOOLEAN NOT NULL DEFAULT TRUE)");
                stmt.executeUpdate("INSERT IGNORE INTO staff (name) VALUES ('Aakash Verma'), ('Sunita Rao'), ('Pooja Chawla'), ('Kunal Sen')");
            } catch (SQLException ignored) {}

            String[][] orderCols = {
                {"order_type", "ALTER TABLE orders ADD COLUMN order_type ENUM('DINE_IN','TAKEAWAY','DELIVERY') NOT NULL DEFAULT 'TAKEAWAY'"},
                {"table_number", "ALTER TABLE orders ADD COLUMN table_number VARCHAR(12) NULL"},
                {"staff_id", "ALTER TABLE orders ADD COLUMN staff_id INT NULL"},
                {"subtotal", "ALTER TABLE orders ADD COLUMN subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00"},
                {"discount_amount", "ALTER TABLE orders ADD COLUMN discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00"},
                {"tax_rate", "ALTER TABLE orders ADD COLUMN tax_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00"},
                {"tax_amount", "ALTER TABLE orders ADD COLUMN tax_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00"}
            };
            for (String[] col : orderCols) {
                try {
                    stmt.executeUpdate(col[1]);
                } catch (SQLException ignored) {}
            }
            try {
                stmt.executeUpdate("UPDATE orders SET subtotal = total_amount WHERE subtotal = 0.00 AND total_amount > 0");
                stmt.executeUpdate("UPDATE orders o JOIN staff s ON o.staff_name = s.name SET o.staff_id = s.staff_id WHERE o.staff_id IS NULL");
            } catch (SQLException ignored) {}

            String[][] paymentCols = {
                {"is_simulated", "ALTER TABLE payments ADD COLUMN is_simulated BOOLEAN NOT NULL DEFAULT TRUE"},
                {"refunded_amount", "ALTER TABLE payments ADD COLUMN refunded_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00"},
                {"refund_time", "ALTER TABLE payments ADD COLUMN refund_time TIMESTAMP NULL"}
            };
            for (String[] col : paymentCols) {
                try {
                    stmt.executeUpdate(col[1]);
                } catch (SQLException ignored) {}
            }
        } catch (SQLException ignored) {}
    }

    public static boolean initDatabase(String pwd) {
        String serverUrl = "jdbc:mysql://" + host + ":" + port
                + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try (Connection conn = DriverManager.getConnection(serverUrl, user, pwd);
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + databaseName
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            java.io.File sqlFile = new java.io.File("dinetrack.sql");
            if (sqlFile.exists()) {
                String content = java.nio.file.Files.readString(sqlFile.toPath());
                stmt.execute("USE " + databaseName);
                for (String part : content.split(";")) {
                    String sql = part.trim();
                    if (!sql.isEmpty()) {
                        try {
                            stmt.execute(sql);
                        } catch (SQLException ignored) {}
                    }
                }
            }
            ensureSchema(conn);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean testConnection() {
        return tryConnect(password);
    }

    public static String getUser() {
        return user;
    }

    public static String getDatabaseName() {
        return databaseName;
    }
}

