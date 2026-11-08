package dinetrack.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Payment {
    private int paymentId;
    private int orderId;
    private BigDecimal amount;
    private String paymentMethod;
    private String referenceCode;
    private String paymentStatus;
    private Timestamp paymentTime;

    public Payment(int paymentId, int orderId, BigDecimal amount, String paymentMethod, String referenceCode,
            String paymentStatus, Timestamp paymentTime) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.referenceCode = referenceCode;
        this.paymentStatus = paymentStatus;
        this.paymentTime = paymentTime;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public int getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public Timestamp getPaymentTime() {
        return paymentTime;
    }
}

