package dinetrack.model;

public class Customer extends User {
    public Customer(int id, String name, String phone, String email) {
        super(id, name, phone, email);
    }

    @Override
    public String getRole() {
        return "Customer";
    }
}

