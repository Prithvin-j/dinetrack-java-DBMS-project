package dinetrack.dao;

import dinetrack.db.Database;
import dinetrack.exception.DataAccessException;
import dinetrack.model.Payment;
import dinetrack.payment.PaymentMethod;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class PaymentDAO {
    public int recordPayment(int orderId, BigDecimal amount, PaymentMethod method) {
        String sql = "INSERT INTO payments (order_id, amount, payment_method, reference_code, payment_status, is_simulated) VALUES (?, ?, ?, ?, 'SUCCESS', ?)";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, orderId);
            ps.setBigDecimal(2, amount);
            ps.setString(3, method.getMethodName());
            ps.setString(4, method.getReferenceCode());
            ps.setBoolean(5, !"CASH".equals(method.getMethodName()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getInt(1);
            }
            throw new SQLException("Failed to obtain Payment ID.");
        } catch (SQLException e) {
            throw new DataAccessException("Error saving payment: " + e.getMessage(), e);
        }
    }

    public Payment getPaymentByOrderId(int orderId) {
        String sql = "SELECT payment_id, order_id, amount, payment_method, reference_code, payment_status, payment_time "
                +
                "FROM payments WHERE order_id = ? ORDER BY payment_id DESC LIMIT 1";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Payment(
                            rs.getInt("payment_id"),
                            rs.getInt("order_id"),
                            rs.getBigDecimal("amount"),
                            rs.getString("payment_method"),
                            rs.getString("reference_code"),
                            rs.getString("payment_status"),
                            rs.getTimestamp("payment_time"));
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Error retrieving payment for order #" + orderId + ": " + e.getMessage(), e);
        }
    }

    public List<Payment> getAllPayments() {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT payment_id, order_id, amount, payment_method, reference_code, payment_status, payment_time FROM payments ORDER BY payment_id DESC";
        try (Connection conn = Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Payment(
                        rs.getInt("payment_id"),
                        rs.getInt("order_id"),
                        rs.getBigDecimal("amount"),
                        rs.getString("payment_method"),
                        rs.getString("reference_code"),
                        rs.getString("payment_status"),
                        rs.getTimestamp("payment_time")));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error loading payments: " + e.getMessage(), e);
        }
    }

    public boolean refundPayment(int paymentId) {
        String sql = "UPDATE payments SET payment_status='REFUNDED', refunded_amount=amount, refund_time=CURRENT_TIMESTAMP WHERE payment_id=? AND payment_status='SUCCESS'";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, paymentId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Error recording refund: " + e.getMessage(), e);
        }
    }
}

