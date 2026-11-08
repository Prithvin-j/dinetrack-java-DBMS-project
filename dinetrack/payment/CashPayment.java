package dinetrack.payment;

import java.math.BigDecimal;

public class CashPayment implements PaymentMethod {
    private final BigDecimal tenderedAmount;
    private final BigDecimal changeAmount;
    private final String referenceCode;

    public CashPayment(BigDecimal billAmount, BigDecimal tenderedAmount) {
        validateAmount(billAmount);
        if (tenderedAmount == null || tenderedAmount.compareTo(billAmount) < 0) {
            throw new IllegalArgumentException("Cash tendered is less than the bill amount.");
        }
        this.tenderedAmount = tenderedAmount;
        this.changeAmount = tenderedAmount.subtract(billAmount);
        this.referenceCode = "CASH-REC-" + (System.currentTimeMillis() % 1000000);
    }

    @Override
    public String getMethodName() {
        return "CASH";
    }

    @Override
    public String getReferenceCode() {
        return referenceCode;
    }

    @Override
    public boolean processPayment(BigDecimal amount) {
        return true;
    }

    public BigDecimal getTenderedAmount() {
        return tenderedAmount;
    }

    public BigDecimal getChangeAmount() {
        return changeAmount;
    }
}

