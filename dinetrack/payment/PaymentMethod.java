package dinetrack.payment;

import java.math.BigDecimal;

public interface PaymentMethod {
    String getMethodName();

    String getReferenceCode();

    boolean processPayment(BigDecimal amount);

    default void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
    }
}

