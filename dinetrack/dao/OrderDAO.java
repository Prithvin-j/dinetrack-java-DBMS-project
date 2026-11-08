package dinetrack.dao;

import dinetrack.db.Database;
import dinetrack.exception.DataAccessException;
import dinetrack.exception.InvalidOrderException;
import dinetrack.model.Order;
import dinetrack.model.OrderItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.*;

public class OrderDAO {
    public Map<String, Object> getBillingSummary(int orderId) {
        String sql = "SELECT order_type, table_number, staff_name, subtotal, discount_amount, tax_rate, tax_amount FROM orders WHERE order_id=?";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    return Collections.emptyMap();
                Map<String, Object> result = new HashMap<>();
                for (String key : new String[] { "order_type", "table_number", "staff_name", "subtotal",
                        "discount_amount", "tax_rate", "tax_amount" })
                    result.put(key, rs.getObject(key));
                return result;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error retrieving order totals: " + e.getMessage(), e);
        }
    }

    public int createOrder(int customerId, Map<Integer, Integer> items) throws InvalidOrderException {
        return createOrder(customerId, items, "TAKEAWAY", null, null, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public int createOrder(int customerId, Map<Integer, Integer> items, String orderType, String tableNumber,
            String staffName, BigDecimal discount, BigDecimal taxRate) throws InvalidOrderException {
        if (taxRate == null || taxRate.compareTo(BigDecimal.ZERO) < 0 || taxRate.compareTo(BigDecimal.valueOf(100)) > 0)
            throw new InvalidOrderException("Tax percentage must be between 0 and 100.");
        if (discount == null || discount.compareTo(BigDecimal.ZERO) < 0)
            throw new InvalidOrderException("Discount cannot be negative.");
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one food item.");
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (customerId > 0) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT customer_id FROM customers WHERE customer_id = ? AND active=TRUE")) {
                        ps.setInt(1, customerId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next())
                                throw new InvalidOrderException("Customer ID #" + customerId + " not found.");
                        }
                    }
                }

                BigDecimal totalAmount = BigDecimal.ZERO;
                Map<Integer, BigDecimal> unitPrices = new HashMap<>();

                for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
                    int foodId = entry.getKey();
                    int qty = entry.getValue();

                    if (qty <= 0) {
                        throw new InvalidOrderException("Quantity for item #" + foodId + " must be positive.");
                    }

                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT name, price, available, stock_quantity FROM foods WHERE food_id = ?")) {
                        ps.setInt(1, foodId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new InvalidOrderException("Food item #" + foodId + " does not exist.");
                            }
                            if (!rs.getBoolean("available") || rs.getInt("stock_quantity") < qty) {
                                throw new InvalidOrderException("Food item '" + rs.getString("name")
                                        + "' is unavailable or has insufficient stock.");
                            }
                            BigDecimal price = rs.getBigDecimal("price");
                            unitPrices.put(foodId, price);
                            totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(qty)));
                        }
                    }
                }

                BigDecimal safeDiscount = discount.max(BigDecimal.ZERO).min(totalAmount).setScale(2,
                        RoundingMode.HALF_UP);
                BigDecimal taxable = totalAmount.subtract(safeDiscount);
                BigDecimal tax = taxable.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal finalTotal = taxable.add(tax);
                Integer staffId = null;
                if (staffName != null && !staffName.trim().isEmpty()) {
                    try (PreparedStatement ps = conn
                            .prepareStatement("SELECT staff_id FROM staff WHERE name=? AND active=TRUE")) {
                        ps.setString(1, staffName.trim());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next())
                                throw new InvalidOrderException("Selected staff member is not active.");
                            staffId = rs.getInt(1);
                        }
                    }
                } else {
                    try (PreparedStatement ps = conn
                            .prepareStatement("SELECT staff_id, name FROM staff WHERE active=TRUE ORDER BY staff_id LIMIT 1");
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            staffId = rs.getInt("staff_id");
                            staffName = rs.getString("name");
                        } else {
                            staffName = "Staff";
                        }
                    }
                }
                int orderId;
                String insertOrderSql = "INSERT INTO orders (customer_id, status, order_type, table_number, staff_id, staff_name, subtotal, discount_amount, tax_rate, tax_amount, total_amount) VALUES (?, 'PLACED', ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                    if (customerId > 0)
                        ps.setInt(1, customerId);
                    else
                        ps.setNull(1, Types.INTEGER);
                    ps.setString(2, orderType != null ? orderType : "TAKEAWAY");
                    ps.setString(3, tableNumber);
                    if (staffId != null)
                        ps.setInt(4, staffId);
                    else
                        ps.setNull(4, Types.INTEGER);
                    ps.setString(5, staffName);
                    ps.setBigDecimal(6, totalAmount);
                    ps.setBigDecimal(7, safeDiscount);
                    ps.setBigDecimal(8, taxRate);
                    ps.setBigDecimal(9, tax);
                    ps.setBigDecimal(10, finalTotal);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next())
                            throw new SQLException("Failed to obtain Order ID.");
                        orderId = rs.getInt(1);
                    }
                }

                String insertItemSql = "INSERT INTO order_items (order_id, food_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertItemSql)) {
                    for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
                        int foodId = entry.getKey();
                        int qty = entry.getValue();
                        BigDecimal price = unitPrices.get(foodId);

                        ps.setInt(1, orderId);
                        ps.setInt(2, foodId);
                        ps.setInt(3, qty);
                        ps.setBigDecimal(4, price);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE foods SET stock_quantity = stock_quantity - ? WHERE food_id = ? AND available = TRUE AND stock_quantity >= ?")) {
                    for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
                        ps.setInt(1, entry.getValue());
                        ps.setInt(2, entry.getKey());
                        ps.setInt(3, entry.getValue());
                        if (ps.executeUpdate() != 1)
                            throw new InvalidOrderException("Stock changed while placing the order. Please retry.");
                    }
                }

                conn.commit();
                return orderId;

            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof InvalidOrderException) {
                    throw (InvalidOrderException) ex;
                }
                throw new DataAccessException("Order transaction failed: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database connection error: " + e.getMessage(), e);
        }
    }

    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.order_id, o.customer_id, c.name AS customer_name, o.order_time, o.status, o.total_amount "
                +
                "FROM orders o LEFT JOIN customers c ON o.customer_id = c.customer_id ORDER BY o.order_id DESC";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Order(
                        rs.getInt("order_id"),
                        rs.getInt("customer_id"),
                        rs.getString("customer_name") == null ? "Walk-in" : rs.getString("customer_name"),
                        rs.getTimestamp("order_time"),
                        rs.getString("status"),
                        rs.getBigDecimal("total_amount")));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error loading orders: " + e.getMessage(), e);
        }
    }

    public Order getOrderDetails(int orderId) {
        String orderSql = "SELECT o.order_id, o.customer_id, c.name AS customer_name, o.order_time, o.status, o.total_amount "
                +
                "FROM orders o LEFT JOIN customers c ON o.customer_id = c.customer_id WHERE o.order_id = ?";
        String itemsSql = "SELECT oi.order_item_id, oi.order_id, oi.food_id, f.name AS food_name, oi.quantity, oi.unit_price "
                +
                "FROM order_items oi JOIN foods f ON oi.food_id = f.food_id WHERE oi.order_id = ?";

        try (Connection conn = Database.getConnection()) {
            Order order = null;
            try (PreparedStatement ps = conn.prepareStatement(orderSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        order = new Order(
                                rs.getInt("order_id"),
                                rs.getInt("customer_id"),
                                rs.getString("customer_name") == null ? "Walk-in" : rs.getString("customer_name"),
                                rs.getTimestamp("order_time"),
                                rs.getString("status"),
                                rs.getBigDecimal("total_amount"));
                    }
                }
            }

            if (order != null) {
                List<OrderItem> items = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(itemsSql)) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            items.add(new OrderItem(
                                    rs.getInt("order_item_id"),
                                    rs.getInt("order_id"),
                                    rs.getInt("food_id"),
                                    rs.getString("food_name"),
                                    rs.getInt("quantity"),
                                    rs.getBigDecimal("unit_price")));
                        }
                    }
                }
                order.setItems(items);
            }
            return order;
        } catch (SQLException e) {
            throw new DataAccessException("Error retrieving order #" + orderId + ": " + e.getMessage(), e);
        }
    }

    public boolean updateStatus(int orderId, String newStatus) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String current;
                try (PreparedStatement ps = conn
                        .prepareStatement("SELECT status FROM orders WHERE order_id=? FOR UPDATE")) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next())
                            return false;
                        current = rs.getString(1);
                    }
                }
                if ("COMPLETED".equals(current) || "CANCELLED".equals(current))
                    return false;
                try (PreparedStatement ps = conn.prepareStatement("UPDATE orders SET status=? WHERE order_id=?")) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, orderId);
                    ps.executeUpdate();
                }
                if ("CANCELLED".equals(newStatus)) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE foods f JOIN (SELECT food_id, SUM(quantity) qty FROM order_items WHERE order_id=? GROUP BY food_id) i ON f.food_id=i.food_id SET f.stock_quantity=f.stock_quantity+i.qty")) {
                        ps.setInt(1, orderId);
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof SQLException se)
                    throw se;
                throw new SQLException(e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error updating order status: " + e.getMessage(), e);
        }
    }
}

