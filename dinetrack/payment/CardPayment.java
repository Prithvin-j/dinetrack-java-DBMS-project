package dinetrack.payment;

import java.math.BigDecimal;

public class CardPayment implements PaymentMethod {
    private final String maskedCardNumber;
    private final String cardHolderName;
    private final String referenceCode;

    public CardPayment(String cardNumber, String cardHolderName) {
        if (cardNumber == null || !cardNumber.replaceAll("\\s+", "").matches("^\\d{16}$")) {
            if (cardNumber != null && cardNumber.matches("^\\d{4}$")) {
                this.maskedCardNumber = "****-****-****-" + cardNumber;
            } else {
                throw new IllegalArgumentException("Card number must be 16 digits (or last 4 digits).");
            }
        } else {
            String clean = cardNumber.replaceAll("\\s+", "");
            this.maskedCardNumber = "****-****-****-" + clean.substring(12);
        }

        if (cardHolderName == null || cardHolderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Cardholder name cannot be empty.");
        }
        this.cardHolderName = cardHolderName.trim();
        this.referenceCode = "CARD-AUTH-" + (100000 + (int) (Math.random() * 900000));
    }

    @Override
    public String getMethodName() {
        return "CARD";
    }

    @Override
    public String getReferenceCode() {
        return referenceCode + " (" + maskedCardNumber + ")";
    }

    @Override
    public boolean processPayment(BigDecimal amount) {
        validateAmount(amount);
        return true;
    }

    public String getMaskedCardNumber() {
        return maskedCardNumber;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }
}

