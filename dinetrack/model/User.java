package dinetrack.model;

public abstract class User {
    private int id;
    private String name;
    private String phone;
    private String email;

    public User(int id, String name, String phone, String email) {
        this.id = id;
        setName(name);
        setPhone(phone);
        setEmail(email);
    }

    public abstract String getRole();

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
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        this.name = name.trim();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        if (phone != null && !phone.trim().isEmpty() && !phone.trim().matches("^[0-9+() -]{7,15}$")) {
            throw new IllegalArgumentException("Invalid phone number format.");
        }
        this.phone = phone == null || phone.trim().isEmpty() ? null : phone.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email != null && !email.trim().isEmpty()
                && !email.trim().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        this.email = email == null || email.trim().isEmpty() ? null : email.trim();
    }

    @Override
    public String toString() {
        return String.format("[%s #%d] %s | Phone: %s | Email: %s", getRole(), id, name, phone, email);
    }
}

