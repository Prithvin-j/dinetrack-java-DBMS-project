package dinetrack.payment;

import java.math.BigDecimal;

public class UpiPayment implements PaymentMethod {
    private final String upiId;
    private final String referenceCode;

    public UpiPayment(String upiId) {
        if (upiId == null || !upiId.trim().matches("^[a-zA-Z0-9.\\-_]{2,49}@[a-zA-Z]{2,49}$")) {
            throw new IllegalArgumentException("Invalid UPI ID format (e.g., name@bank).");
        }
        this.upiId = upiId.trim();
        this.referenceCode = "UPI-TXN-" + (10000000L + (long) (Math.random() * 90000000L));
    }

    @Override
    public String getMethodName() {
        return "UPI";
    }

    @Override
    public String getReferenceCode() {
        return referenceCode + " (" + upiId + ")";
    }

    @Override
    public boolean processPayment(BigDecimal amount) {
        validateAmount(amount);
        return true;
    }

    public String getUpiId() {
        return upiId;
    }
}

