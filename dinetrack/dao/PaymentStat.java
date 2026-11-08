package dinetrack.dao;

import java.math.BigDecimal;

public class PaymentStat {
    String method;
    int count;
    BigDecimal collected;
    BigDecimal avg;

    public PaymentStat(String method, int count, BigDecimal collected, BigDecimal avg) {
        this.method = method;
        this.count = count;
        this.collected = collected;
        this.avg = avg;
    }
}
