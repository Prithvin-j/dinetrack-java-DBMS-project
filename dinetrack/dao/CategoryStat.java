package dinetrack.dao;

import java.math.BigDecimal;

public class CategoryStat {
    String category;
    int ordersCount;
    int qtySold;
    BigDecimal revenue;

    public CategoryStat(String category, int ordersCount, int qtySold, BigDecimal revenue) {
        this.category = category;
        this.ordersCount = ordersCount;
        this.qtySold = qtySold;
        this.revenue = revenue;
    }
}

