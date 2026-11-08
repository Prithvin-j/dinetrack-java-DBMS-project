package dinetrack.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Food {
    private int id;
    private String name;
    private String category;
    private BigDecimal price;
    private boolean available;

    public Food(int id, String name, String category, BigDecimal price, boolean available) {
        this.id = id;
        setName(name);
        setCategory(category);
        setPrice(price);
        this.available = available;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Food name cannot be empty.");
        }
        this.name = name.trim();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be empty.");
        }
        this.category = category.trim();
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive.");
        }
        this.price = price.setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return String.format("#%d | %-22s | %-12s | Rs.%7.2f | %s",
                id, name, category, price, available ? "Available" : "Out of Stock");
    }
}

