import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.*;

public class DineTrack {
    private static final Scanner scanner = new Scanner(System.in);
    private static final CustomerDAO customerDAO = new CustomerDAO();
    private static final FoodDAO foodDAO = new FoodDAO();
    private static final OrderDAO orderDAO = new OrderDAO();
    private static final PaymentDAO paymentDAO = new PaymentDAO();
    private static final ReportDAO reportDAO = new ReportDAO();

    public static void main(String[] args) {
        printBanner();
        ensureDatabaseConnection();

        boolean running = true;
        while (running) {
            try {
                printMainMenu();
                int choice = promptInt("Enter your choice (1-8): ", 1, 8);
                switch (choice) {
                    case 1 -> customerMenu();
                    case 2 -> foodMenu();
                    case 3 -> createOrderWorkflow();
                    case 4 -> viewOrdersWorkflow();
                    case 5 -> updateOrderStatusWorkflow();
                    case 6 -> paymentWorkflow();
                    case 7 -> reportsMenu();
                    case 8 -> {
                        running = false;
                        System.out.println("\nThank you for using DineTrack. Goodbye!");
                    }
                }
            } catch (InvalidOrderException e) {
                System.out.println("\n[Order Error] " + e.getMessage());
            } catch (DataAccessException e) {
                System.out.println("\n[Database Notice] " + e.getMessage());
            } catch (Exception e) {
                System.out.println("\n[Error] " + e.getMessage());
            }
        }
    }

    private static void ensureDatabaseConnection() {
        String[] candidatePasswords = {"root", "", "admin", "1234", "12345678", "root123", "password"};
        for (String pwd : candidatePasswords) {
            if (Database.tryConnect(pwd)) {
                Database.setPassword(pwd);
                System.out.println("[Connected] MySQL database connected: " + Database.getDatabaseName());
                return;
            }
        }

        System.out.println("\nMySQL is running, but requires password authentication.");
        while (true) {
            System.out.print("Enter your MySQL root password (press Enter if no password): ");
            String pwd = scanner.nextLine();
            if (Database.tryConnect(pwd)) {
                Database.setPassword(pwd);
                System.out.println("[Connected] Database connected successfully!\n");
                break;
            } else {
                System.out.println("[Failed] Could not connect with that password. Please try again.\n");
                System.out.print("Retry? (y/n): ");
                if (!scanner.nextLine().trim().equalsIgnoreCase("y")) {
                    System.out.println("Exiting application.");
                    System.exit(1);
                }
            }
        }
    }

    private static void printBanner() {
        System.out.println("==========================================================================");
        System.out.println("                  DINETRACK RESTAURANT MANAGEMENT SYSTEM                  ");
        System.out.println("==========================================================================");
    }

    private static void printMainMenu() {
        System.out.println("\n============================= MAIN MENU ==================================");
        System.out.println("  1. Customer Management       (Add, View, Update, Delete, Search)");
        System.out.println("  2. Food Menu Management      (Add, View, Update, Stock Status)");
        System.out.println("  3. Create New Order          (Select items and place order)");
        System.out.println("  4. View Orders & Invoices    (Order history and detailed bill)");
        System.out.println("  5. Update Order Status       (Placed, Preparing, Ready, Completed)");
        System.out.println("  6. Payment Processing        (Cash, Card, UPI payment)");
        System.out.println("  7. Reports & Statistics      (Dashboard & Top selling foods)");
        System.out.println("  8. Exit");
        System.out.println("==========================================================================");
    }

    private static void customerMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- CUSTOMER MANAGEMENT ---");
            System.out.println("1. Add New Customer");
            System.out.println("2. View All Customers");
            System.out.println("3. Search Customer (by Name / Phone / Email)");
            System.out.println("4. Update Customer Information");
            System.out.println("5. Delete Customer");
            System.out.println("6. Back to Main Menu");
            int choice = promptInt("Choice (1-6): ", 1, 6);

            switch (choice) {
                case 1 -> {
                    System.out.println("\n[Register New Customer]");
                    String name = promptNonEmptyString("Enter Customer Name: ");
                    String phone = promptRegexString("Enter Phone (7-15 digits): ", "^[0-9+() -]{7,15}$", "Invalid phone format.");
                    String email = promptRegexString("Enter Email: ", "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", "Invalid email format.");
                    Customer c = new Customer(0, name, phone, email);
                    int id = customerDAO.add(c);
                    System.out.println(">> Success: Customer registered with ID #" + id);
                }
                case 2 -> displayCustomerList(customerDAO.getAll());
                case 3 -> {
                    System.out.print("Enter search term (Name/Phone/Email): ");
                    String query = scanner.nextLine().trim();
                    displayCustomerList(customerDAO.search(query));
                }
                case 4 -> {
                    int id = promptInt("Enter Customer ID to update: ");
                    Customer existing = customerDAO.getById(id);
                    if (existing == null) {
                        System.out.println(">> Customer not found with ID #" + id);
                        break;
                    }
                    System.out.println("Current Details: " + existing);
                    System.out.print("Enter New Name (leave blank to keep '" + existing.getName() + "'): ");
                    String n = scanner.nextLine().trim();
                    if (!n.isEmpty()) existing.setName(n);

                    System.out.print("Enter New Phone (leave blank to keep '" + existing.getPhone() + "'): ");
                    String p = scanner.nextLine().trim();
                    if (!p.isEmpty()) existing.setPhone(p);

                    System.out.print("Enter New Email (leave blank to keep '" + existing.getEmail() + "'): ");
                    String e = scanner.nextLine().trim();
                    if (!e.isEmpty()) existing.setEmail(e);

                    if (customerDAO.update(existing)) {
                        System.out.println(">> Success: Customer details updated!");
                    } else {
                        System.out.println(">> Could not update customer.");
                    }
                }
                case 5 -> {
                    int id = promptInt("Enter Customer ID to delete: ");
                    System.out.print("Are you sure you want to delete customer #" + id + "? (yes/no): ");
                    if (scanner.nextLine().trim().equalsIgnoreCase("yes")) {
                        if (customerDAO.delete(id)) {
                            System.out.println(">> Success: Customer deleted.");
                        } else {
                            System.out.println(">> Customer not found.");
                        }
                    } else {
                        System.out.println(">> Deletion cancelled.");
                    }
                }
                case 6 -> back = true;
            }
        }
    }

    private static void displayCustomerList(List<Customer> list) {
        if (list.isEmpty()) {
            System.out.println("\nNo customer records found.");
            return;
        }
        System.out.println("\n----------------------------------------------------------------------------------");
        System.out.printf("%-5s | %-22s | %-16s | %-26s | %s\n", "ID", "Name", "Phone", "Email", "Role");
        System.out.println("------+------------------------+------------------+----------------------------+-----------");
        for (Customer c : list) {
            System.out.printf("%-5d | %-22s | %-16s | %-26s | %s\n",
                    c.getId(), c.getName(), c.getPhone(), c.getEmail(), c.getRole());
        }
        System.out.println("----------------------------------------------------------------------------------");
    }

    private static void foodMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- FOOD MENU MANAGEMENT ---");
            System.out.println("1. View Full Menu (All Items)");
            System.out.println("2. View Available Items Only");
            System.out.println("3. Add New Food Item");
            System.out.println("4. Update Food Item Details");
            System.out.println("5. Toggle Stock Availability");
            System.out.println("6. Delete Food Item");
            System.out.println("7. Back to Main Menu");
            int choice = promptInt("Choice (1-7): ", 1, 7);

            switch (choice) {
                case 1 -> displayFoodList(foodDAO.getAll(false));
                case 2 -> displayFoodList(foodDAO.getAll(true));
                case 3 -> {
                    System.out.println("\n[Add New Food Item]");
                    String name = promptNonEmptyString("Item Name: ");
                    String category = promptNonEmptyString("Category: ");
                    BigDecimal price = promptPositiveBigDecimal("Price (Rs.): ");
                    System.out.print("Is it currently available? (yes/no) [yes]: ");
                    String availStr = scanner.nextLine().trim();
                    boolean avail = availStr.isEmpty() || availStr.equalsIgnoreCase("yes") || availStr.equalsIgnoreCase("y");

                    Food f = new Food(0, name, category, price, avail);
                    int id = foodDAO.add(f);
                    System.out.println(">> Success: Food item added to menu with ID #" + id);
                }
                case 4 -> {
                    int id = promptInt("Enter Food ID to update: ");
                    Food f = foodDAO.getById(id);
                    if (f == null) {
                        System.out.println(">> Food item not found with ID #" + id);
                        break;
                    }
                    System.out.println("Current: " + f);
                    System.out.print("Enter New Name (leave blank to keep '" + f.getName() + "'): ");
                    String n = scanner.nextLine().trim();
                    if (!n.isEmpty()) f.setName(n);

                    System.out.print("Enter New Category (leave blank to keep '" + f.getCategory() + "'): ");
                    String c = scanner.nextLine().trim();
                    if (!c.isEmpty()) f.setCategory(c);

                    System.out.print("Enter New Price (leave blank to keep Rs. " + f.getPrice() + "): ");
                    String pStr = scanner.nextLine().trim();
                    if (!pStr.isEmpty()) {
                        f.setPrice(new BigDecimal(pStr));
                    }

                    System.out.print("Available? (yes/no, leave blank to keep " + (f.isAvailable() ? "yes" : "no") + "): ");
                    String aStr = scanner.nextLine().trim();
                    if (!aStr.isEmpty()) {
                        f.setAvailable(aStr.equalsIgnoreCase("yes") || aStr.equalsIgnoreCase("y"));
                    }

                    if (foodDAO.update(f)) {
                        System.out.println(">> Success: Menu item updated!");
                    } else {
                        System.out.println(">> Failed to update menu item.");
                    }
                }
                case 5 -> {
                    int id = promptInt("Enter Food ID to toggle availability: ");
                    Food f = foodDAO.getById(id);
                    if (f == null) {
                        System.out.println(">> Food item not found with ID #" + id);
                        break;
                    }
                    if (foodDAO.toggleAvailability(id)) {
                        System.out.println(">> Success: Availability changed to: " + (!f.isAvailable() ? "AVAILABLE" : "OUT OF STOCK"));
                    }
                }
                case 6 -> {
                    int id = promptInt("Enter Food ID to delete: ");
                    System.out.print("Are you sure you want to delete food #" + id + "? (yes/no): ");
                    if (scanner.nextLine().trim().equalsIgnoreCase("yes")) {
                        if (foodDAO.delete(id)) {
                            System.out.println(">> Success: Food item removed from menu.");
                        } else {
                            System.out.println(">> Food item not found.");
                        }
                    }
                }
                case 7 -> back = true;
            }
        }
    }

    private static void displayFoodList(List<Food> list) {
        if (list.isEmpty()) {
            System.out.println("\nNo menu items found.");
            return;
        }
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.printf("%-5s | %-24s | %-16s | %-12s | %s\n", "ID", "Item Name", "Category", "Price", "Status");
        System.out.println("------+--------------------------+------------------+--------------+------------");
        for (Food f : list) {
            System.out.printf("%-5d | %-24s | %-16s | Rs.%9.2f | %s\n",
                    f.getId(), f.getName(), f.getCategory(), f.getPrice(),
                    f.isAvailable() ? "In Stock" : "[Out of Stock]");
        }
        System.out.println("--------------------------------------------------------------------------------");
    }

    private static void createOrderWorkflow() throws InvalidOrderException {
        System.out.println("\n========================== CREATE NEW ORDER ==============================");
        int customerId = promptInt("Enter Customer ID: ");
        Customer customer = customerDAO.getById(customerId);
        if (customer == null) {
            System.out.println(">> Customer #" + customerId + " not found!");
            System.out.print("Would you like to quickly register this customer? (yes/no): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("yes")) {
                String name = promptNonEmptyString("Customer Name: ");
                String phone = promptRegexString("Phone: ", "^[0-9+() -]{7,15}$", "Invalid phone format.");
                String email = promptRegexString("Email: ", "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", "Invalid email format.");
                customerId = customerDAO.add(new Customer(0, name, phone, email));
                customer = customerDAO.getById(customerId);
                System.out.println(">> Registered! Proceeding with Customer #" + customerId + " (" + customer.getName() + ")");
            } else {
                return;
            }
        } else {
            System.out.println("Customer Selected: " + customer.getName() + " (" + customer.getPhone() + ")");
        }

        System.out.println("\nAvailable Menu Items:");
        displayFoodList(foodDAO.getAll(true));

        Map<Integer, Integer> cart = new LinkedHashMap<>();
        BigDecimal estimatedTotal = BigDecimal.ZERO;

        while (true) {
            System.out.print("\nEnter Food ID to add (or 0 to finish selection): ");
            int foodId = promptInt("");
            if (foodId == 0) break;

            Food food = foodDAO.getById(foodId);
            if (food == null) {
                System.out.println(">> Invalid Food ID. Please choose from the menu.");
                continue;
            }
            if (!food.isAvailable()) {
                System.out.println(">> Sorry, '" + food.getName() + "' is currently out of stock.");
                continue;
            }

            int qty = promptInt("Enter quantity for " + food.getName() + " (Rs. " + food.getPrice() + "): ", 1, 99);
            cart.put(foodId, cart.getOrDefault(foodId, 0) + qty);

            estimatedTotal = BigDecimal.ZERO;
            System.out.println("\n--- Current Cart ---");
            for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
                Food item = foodDAO.getById(e.getKey());
                BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(e.getValue()));
                estimatedTotal = estimatedTotal.add(lineTotal);
                System.out.printf("  * %-20s x %2d = Rs.%7.2f\n", item.getName(), e.getValue(), lineTotal);
            }
            System.out.printf("Estimated Total: Rs. %.2f\n", estimatedTotal);
        }

        if (cart.isEmpty()) {
            System.out.println(">> Order cancelled: No items selected.");
            return;
        }

        System.out.print("\nConfirm order placement for Rs. " + estimatedTotal + "? (yes/no) [yes]: ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.isEmpty() && !confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) {
            System.out.println(">> Order discarded.");
            return;
        }

        int orderId = orderDAO.createOrder(customerId, cart);
        System.out.println("\n==========================================================================");
        System.out.println(">> ORDER PLACED SUCCESSFULLY! Order ID: #" + orderId);
        System.out.println(">> Status: PLACED | Total Amount: Rs. " + estimatedTotal);
        System.out.println("==========================================================================");

        System.out.print("\nWould you like to process payment for Order #" + orderId + " now? (yes/no) [yes]: ");
        String payNow = scanner.nextLine().trim();
        if (payNow.isEmpty() || payNow.equalsIgnoreCase("yes") || payNow.equalsIgnoreCase("y")) {
            processPaymentForOrder(orderId);
        }
    }

    private static void viewOrdersWorkflow() {
        System.out.println("\n--- VIEW ORDERS & BILLS ---");
        System.out.println("1. List All Recent Orders");
        System.out.println("2. View Detailed Order Invoice / Bill");
        int choice = promptInt("Choice (1-2): ", 1, 2);

        if (choice == 1) {
            List<Order> orders = orderDAO.getAllOrders();
            if (orders.isEmpty()) {
                System.out.println("\nNo orders found in database.");
                return;
            }
            System.out.println("\n--------------------------------------------------------------------------------------------");
            System.out.printf("%-8s | %-20s | %-19s | %-12s | %-12s | %s\n",
                    "Order ID", "Customer Name", "Order Date/Time", "Status", "Total (Rs.)", "Payment");
            System.out.println("---------+----------------------+---------------------+--------------+--------------+-------------");
            for (Order o : orders) {
                Payment p = paymentDAO.getPaymentByOrderId(o.getOrderId());
                String paymentBadge = (p != null) ? "PAID (" + p.getPaymentMethod() + ")" : "PENDING";
                String timeStr = o.getOrderTime() != null ? o.getOrderTime().toString().substring(0, 19) : "N/A";
                System.out.printf("#%-7d | %-20s | %-19s | %-12s | Rs.%9.2f | %s\n",
                        o.getOrderId(), o.getCustomerName(), timeStr, o.getStatus(), o.getTotalAmount(), paymentBadge);
            }
            System.out.println("--------------------------------------------------------------------------------------------");
        } else {
            int orderId = promptInt("Enter Order ID to view bill: ");
            printInvoice(orderId);
        }
    }

    private static void printInvoice(int orderId) {
        Order order = orderDAO.getOrderDetails(orderId);
        if (order == null) {
            System.out.println(">> Order #" + orderId + " does not exist.");
            return;
        }

        Customer cust = customerDAO.getById(order.getCustomerId());
        Payment payment = paymentDAO.getPaymentByOrderId(orderId);

        System.out.println("\n==========================================================================");
        System.out.println("                       DINETRACK RESTAURANT BILL                          ");
        System.out.println("==========================================================================");
        System.out.printf("Order ID    : #%-15d Status: %s\n", order.getOrderId(), order.getStatus());
        System.out.printf("Date & Time : %s\n", order.getOrderTime());
        System.out.printf("Customer    : %s (Phone: %s)\n", order.getCustomerName(), (cust != null ? cust.getPhone() : "N/A"));
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("%-4s | %-26s | %-8s | %-10s | %-12s\n", "#", "Item", "Qty", "Unit Price", "Subtotal");
        System.out.println("-----+----------------------------+----------+------------+---------------");

        int idx = 1;
        for (OrderItem item : order.getItems()) {
            System.out.printf("%-4d | %-26s | %-8d | Rs.%7.2f | Rs.%9.2f\n",
                    idx++, item.getFoodName(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal());
        }
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("TOTAL AMOUNT DUE :                                         Rs.%10.2f\n", order.getTotalAmount());
        System.out.println("--------------------------------------------------------------------------");

        if (payment != null) {
            System.out.println("PAYMENT STATUS   : PAID (SUCCESS)");
            System.out.printf("Payment Method   : %s\n", payment.getPaymentMethod());
            System.out.printf("Reference Code   : %s\n", payment.getReferenceCode());
            System.out.printf("Paid At          : %s\n", payment.getPaymentTime());
        } else {
            System.out.println("PAYMENT STATUS   : UNPAID / PENDING");
        }
        System.out.println("==========================================================================\n");
    }

    private static void updateOrderStatusWorkflow() {
        System.out.println("\n--- UPDATE ORDER STATUS ---");
        int orderId = promptInt("Enter Order ID: ");
        Order order = orderDAO.getOrderDetails(orderId);
        if (order == null) {
            System.out.println(">> Order #" + orderId + " not found.");
            return;
        }

        System.out.println("Order #" + orderId + " for " + order.getCustomerName() + " | Current Status: " + order.getStatus());
        System.out.println("Select New Status:");
        System.out.println("1. PLACED");
        System.out.println("2. PREPARING");
        System.out.println("3. READY");
        System.out.println("4. COMPLETED");
        System.out.println("5. CANCELLED");
        int stChoice = promptInt("Choice (1-5): ", 1, 5);

        String newStatus = switch (stChoice) {
            case 1 -> "PLACED";
            case 2 -> "PREPARING";
            case 3 -> "READY";
            case 4 -> "COMPLETED";
            case 5 -> "CANCELLED";
            default -> "PLACED";
        };

        if (orderDAO.updateStatus(orderId, newStatus)) {
            System.out.println(">> Success: Status for Order #" + orderId + " changed to " + newStatus);
        } else {
            System.out.println(">> Could not update status.");
        }
    }

    private static void paymentWorkflow() {
        System.out.println("\n--- PAYMENT PROCESSING ---");
        System.out.println("1. Pay for an Order");
        System.out.println("2. View All Payment Records");
        int ch = promptInt("Choice (1-2): ", 1, 2);

        if (ch == 1) {
            int orderId = promptInt("Enter Order ID to process payment: ");
            processPaymentForOrder(orderId);
        } else {
            List<Payment> list = paymentDAO.getAllPayments();
            if (list.isEmpty()) {
                System.out.println("\nNo payment records found.");
                return;
            }
            System.out.println("\n---------------------------------------------------------------------------------------------------------");
            System.out.printf("%-6s | %-8s | %-12s | %-8s | %-32s | %s\n",
                    "Pay ID", "Order ID", "Amount", "Method", "Reference Code", "Time");
            System.out.println("-------+----------+--------------+----------+----------------------------------+-------------------------");
            for (Payment p : list) {
                System.out.printf("%-6d | #%-7d | Rs.%9.2f | %-8s | %-32s | %s\n",
                        p.getPaymentId(), p.getOrderId(), p.getAmount(),
                        p.getPaymentMethod(), p.getReferenceCode(), p.getPaymentTime());
            }
            System.out.println("---------------------------------------------------------------------------------------------------------");
        }
    }

    private static void processPaymentForOrder(int orderId) {
        Order order = orderDAO.getOrderDetails(orderId);
        if (order == null) {
            System.out.println(">> Order #" + orderId + " not found.");
            return;
        }

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            System.out.println(">> Cannot process payment: Order #" + orderId + " is CANCELLED.");
            return;
        }

        Payment existing = paymentDAO.getPaymentByOrderId(orderId);
        if (existing != null) {
            System.out.println(">> Notice: Order #" + orderId + " has already been paid!");
            System.out.println("   Method: " + existing.getPaymentMethod() + " | Ref: " + existing.getReferenceCode());
            return;
        }

        BigDecimal bill = order.getTotalAmount();
        System.out.println("\n--- Payment for Order #" + orderId + " ---");
        System.out.println("Customer    : " + order.getCustomerName());
        System.out.println("Total Amount: Rs. " + bill);
        System.out.println("\nChoose Payment Method:");
        System.out.println("1. Cash Payment");
        System.out.println("2. Debit/Credit Card");
        System.out.println("3. UPI (Google Pay, PhonePe, Paytm, etc.)");

        int methodChoice = promptInt("Choice (1-3): ", 1, 3);
        PaymentMethod paymentMethod;

        switch (methodChoice) {
            case 1 -> {
                System.out.println("\n[Cash Payment]");
                BigDecimal tendered = promptPositiveBigDecimal("Enter cash tendered by customer (Rs.): ");
                while (tendered.compareTo(bill) < 0) {
                    System.out.println(">> Tendered amount is less than bill amount (Rs. " + bill + ").");
                    tendered = promptPositiveBigDecimal("Enter sufficient cash (Rs.): ");
                }
                CashPayment cash = new CashPayment(bill, tendered);
                paymentMethod = cash;
                System.out.printf(">> Change to return to customer: Rs. %.2f\n", cash.getChangeAmount());
            }
            case 2 -> {
                System.out.println("\n[Card Payment]");
                String cardNum = promptRegexString("Enter Card Number (16 digits or 4 digits): ", "^(\\d{16}|\\d{4})$", "Must be 16 digits or 4 digits.");
                String cardHolder = promptNonEmptyString("Enter Cardholder Name: ");
                paymentMethod = new CardPayment(cardNum, cardHolder);
            }
            case 3 -> {
                System.out.println("\n[UPI Payment]");
                String upiId = promptRegexString("Enter Customer UPI ID (e.g. name@bank): ", "^[a-zA-Z0-9.\\-_]{2,49}@[a-zA-Z]{2,49}$", "Invalid UPI format.");
                paymentMethod = new UpiPayment(upiId);
            }
            default -> throw new IllegalStateException("Invalid choice");
        }

        paymentMethod.processPayment(bill);
        int payId = paymentDAO.recordPayment(orderId, bill, paymentMethod);

        if (!"COMPLETED".equalsIgnoreCase(order.getStatus())) {
            orderDAO.updateStatus(orderId, "COMPLETED");
        }

        System.out.println("\n==========================================================================");
        System.out.println(">> PAYMENT SUCCESSFUL!");
        System.out.println("   Payment ID     : #" + payId);
        System.out.println("   Method         : " + paymentMethod.getMethodName());
        System.out.println("   Reference Code : " + paymentMethod.getReferenceCode());
        System.out.println("   Amount Paid    : Rs. " + bill);
        System.out.println("   Order Status   : COMPLETED");
        System.out.println("==========================================================================");
    }

    private static void reportsMenu() {
        System.out.println("\n--- REPORTS & STATISTICS ---");
        System.out.println("1. System Summary Dashboard");
        System.out.println("2. Top Selling Menu Items");
        int ch = promptInt("Choice (1-2): ", 1, 2);
        if (ch == 1) {
            reportDAO.printDashboard();
        } else {
            reportDAO.printTopSellingFoods();
        }
    }

    private static int promptInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println(">> Invalid number. Please enter a valid number.");
            }
        }
    }

    private static int promptInt(String prompt, int min, int max) {
        while (true) {
            int val = promptInt(prompt);
            if (val >= min && val <= max) return val;
            System.out.printf(">> Please enter a number between %d and %d.\n", min, max);
        }
    }

    private static int promptInt(String prompt, int min, int max, int defaultVal) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) return defaultVal;
            try {
                int val = Integer.parseInt(line);
                if (val >= min && val <= max) return val;
                System.out.printf(">> Please enter a number between %d and %d.\n", min, max);
            } catch (NumberFormatException e) {
                System.out.println(">> Invalid number.");
            }
        }
    }

    private static BigDecimal promptPositiveBigDecimal(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                BigDecimal val = new BigDecimal(scanner.nextLine().trim());
                if (val.compareTo(BigDecimal.ZERO) > 0) {
                    return val.setScale(2, RoundingMode.HALF_UP);
                }
                System.out.println(">> Amount must be greater than zero.");
            } catch (NumberFormatException e) {
                System.out.println(">> Invalid decimal amount.");
            }
        }
    }

    private static String promptNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) return input;
            System.out.println(">> Field cannot be left empty.");
        }
    }

    private static String promptRegexString(String prompt, String regex, String errorMsg) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.matches(regex)) return input;
            System.out.println(">> " + errorMsg);
        }
    }
}

class InvalidOrderException extends Exception {
    public InvalidOrderException(String message) {
        super(message);
    }
}

class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}

class Database {
    private static String host = "localhost";
    private static int port = 3306;
    private static String databaseName = "dinetrack";
    private static String user = "root";
    private static String password = "root";

    private Database() {}

    public static synchronized void setCredentials(String h, int p, String db, String u, String pwd) {
        host = h;
        port = p;
        databaseName = db;
        user = u;
        password = pwd;
    }

    public static synchronized void setPassword(String pwd) {
        password = pwd;
    }

    public static String getUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), user, password);
    }

    public static boolean tryConnect(String pwd) {
        try (Connection conn = DriverManager.getConnection(getUrl(), user, pwd)) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1049) {
                return initDatabase(pwd);
            }
            return false;
        }
    }

    public static boolean initDatabase(String pwd) {
        String serverUrl = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try (Connection conn = DriverManager.getConnection(serverUrl, user, pwd);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + databaseName + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            java.io.File sqlFile = new java.io.File("dinetrack.sql");
            if (sqlFile.exists()) {
                String content = java.nio.file.Files.readString(sqlFile.toPath());
                stmt.execute("USE " + databaseName);
                for (String part : content.split(";")) {
                    String sql = part.trim();
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                }
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean testConnection() {
        return tryConnect(password);
    }

    public static String getUser() { return user; }
    public static String getDatabaseName() { return databaseName; }
}

abstract class User {
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

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        this.name = name.trim();
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) {
        if (phone == null || !phone.trim().matches("^[0-9+() -]{7,15}$")) {
            throw new IllegalArgumentException("Invalid phone number format.");
        }
        this.phone = phone.trim();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        if (email == null || !email.trim().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        this.email = email.trim();
    }

    @Override
    public String toString() {
        return String.format("[%s #%d] %s | Phone: %s | Email: %s", getRole(), id, name, phone, email);
    }
}

class Customer extends User {
    public Customer(int id, String name, String phone, String email) {
        super(id, name, phone, email);
    }

    @Override
    public String getRole() {
        return "Customer";
    }
}

class Staff extends User {
    private String jobTitle;

    public Staff(int id, String name, String phone, String email, String jobTitle) {
        super(id, name, phone, email);
        setJobTitle(jobTitle);
    }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) {
        if (jobTitle == null || jobTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Job title cannot be empty.");
        }
        this.jobTitle = jobTitle.trim();
    }

    @Override
    public String getRole() {
        return "Staff (" + jobTitle + ")";
    }
}

class Food {
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

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Food name cannot be empty.");
        }
        this.name = name.trim();
    }

    public String getCategory() { return category; }
    public void setCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be empty.");
        }
        this.category = category.trim();
    }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive.");
        }
        this.price = price.setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public String toString() {
        return String.format("#%d | %-22s | %-12s | Rs.%7.2f | %s",
                id, name, category, price, available ? "Available" : "Out of Stock");
    }
}

class OrderItem {
    private int orderItemId;
    private int orderId;
    private int foodId;
    private String foodName;
    private int quantity;
    private BigDecimal unitPrice;

    public OrderItem(int orderItemId, int orderId, int foodId, String foodName, int quantity, BigDecimal unitPrice) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.foodId = foodId;
        this.foodName = foodName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getOrderItemId() { return orderItemId; }
    public int getOrderId() { return orderId; }
    public int getFoodId() { return foodId; }
    public String getFoodName() { return foodName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getSubtotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}

class Order {
    private int orderId;
    private int customerId;
    private String customerName;
    private Timestamp orderTime;
    private String status;
    private BigDecimal totalAmount;
    private List<OrderItem> items = new ArrayList<>();

    public Order(int orderId, int customerId, String customerName, Timestamp orderTime, String status, BigDecimal totalAmount) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.orderTime = orderTime;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    public int getOrderId() { return orderId; }
    public int getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public Timestamp getOrderTime() { return orderTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}

class Payment {
    private int paymentId;
    private int orderId;
    private BigDecimal amount;
    private String paymentMethod;
    private String referenceCode;
    private String paymentStatus;
    private Timestamp paymentTime;

    public Payment(int paymentId, int orderId, BigDecimal amount, String paymentMethod, String referenceCode, String paymentStatus, Timestamp paymentTime) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.referenceCode = referenceCode;
        this.paymentStatus = paymentStatus;
        this.paymentTime = paymentTime;
    }

    public int getPaymentId() { return paymentId; }
    public int getOrderId() { return orderId; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getReferenceCode() { return referenceCode; }
    public String getPaymentStatus() { return paymentStatus; }
    public Timestamp getPaymentTime() { return paymentTime; }
}

interface PaymentMethod {
    String getMethodName();
    String getReferenceCode();
    boolean processPayment(BigDecimal amount);
    default void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
    }
}

class CashPayment implements PaymentMethod {
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
    public String getMethodName() { return "CASH"; }

    @Override
    public String getReferenceCode() { return referenceCode; }

    @Override
    public boolean processPayment(BigDecimal amount) { return true; }

    public BigDecimal getTenderedAmount() { return tenderedAmount; }
    public BigDecimal getChangeAmount() { return changeAmount; }
}

class CardPayment implements PaymentMethod {
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
        this.referenceCode = "CARD-AUTH-" + (100000 + (int)(Math.random() * 900000));
    }

    @Override
    public String getMethodName() { return "CARD"; }

    @Override
    public String getReferenceCode() { return referenceCode + " (" + maskedCardNumber + ")"; }

    @Override
    public boolean processPayment(BigDecimal amount) {
        validateAmount(amount);
        return true;
    }

    public String getMaskedCardNumber() { return maskedCardNumber; }
    public String getCardHolderName() { return cardHolderName; }
}

class UpiPayment implements PaymentMethod {
    private final String upiId;
    private final String referenceCode;

    public UpiPayment(String upiId) {
        if (upiId == null || !upiId.trim().matches("^[a-zA-Z0-9.\\-_]{2,49}@[a-zA-Z]{2,49}$")) {
            throw new IllegalArgumentException("Invalid UPI ID format (e.g., name@bank).");
        }
        this.upiId = upiId.trim();
        this.referenceCode = "UPI-TXN-" + (10000000L + (long)(Math.random() * 90000000L));
    }

    @Override
    public String getMethodName() { return "UPI"; }

    @Override
    public String getReferenceCode() { return referenceCode + " (" + upiId + ")"; }

    @Override
    public boolean processPayment(BigDecimal amount) {
        validateAmount(amount);
        return true;
    }

    public String getUpiId() { return upiId; }
}

class CustomerDAO {
    public int add(Customer c) {
        String sql = "INSERT INTO customers (name, phone, email) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getEmail());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            throw new SQLException("Failed to retrieve generated Customer ID.");
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new DataAccessException("Customer with this phone or email already exists.", e);
            }
            throw new DataAccessException("Error adding customer: " + e.getMessage(), e);
        }
    }

    public List<Customer> getAll() {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT customer_id, name, phone, email FROM customers ORDER BY customer_id";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Customer(rs.getInt("customer_id"), rs.getString("name"),
                        rs.getString("phone"), rs.getString("email")));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error loading customers: " + e.getMessage(), e);
        }
    }

    public Customer getById(int id) {
        String sql = "SELECT customer_id, name, phone, email FROM customers WHERE customer_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Customer(rs.getInt("customer_id"), rs.getString("name"),
                            rs.getString("phone"), rs.getString("email"));
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Error finding customer #" + id + ": " + e.getMessage(), e);
        }
    }

    public boolean update(Customer c) {
        String sql = "UPDATE customers SET name = ?, phone = ?, email = ? WHERE customer_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getEmail());
            ps.setInt(4, c.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new DataAccessException("Another customer already has this phone or email.", e);
            }
            throw new DataAccessException("Error updating customer: " + e.getMessage(), e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM customers WHERE customer_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) {
                throw new DataAccessException("Cannot delete customer #" + id + " because they have existing order records.", e);
            }
            throw new DataAccessException("Error deleting customer: " + e.getMessage(), e);
        }
    }

    public List<Customer> search(String query) {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT customer_id, name, phone, email FROM customers WHERE name LIKE ? OR phone LIKE ? OR email LIKE ? ORDER BY customer_id";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String term = "%" + query + "%";
            ps.setString(1, term);
            ps.setString(2, term);
            ps.setString(3, term);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Customer(rs.getInt("customer_id"), rs.getString("name"),
                            rs.getString("phone"), rs.getString("email")));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error searching customers: " + e.getMessage(), e);
        }
    }
}

class FoodDAO {
    public int add(Food f) {
        String sql = "INSERT INTO foods (name, category, price, available) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, f.getName());
            ps.setString(2, f.getCategory());
            ps.setBigDecimal(3, f.getPrice());
            ps.setBoolean(4, f.isAvailable());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            throw new SQLException("Failed to retrieve generated Food ID.");
        } catch (SQLException e) {
            throw new DataAccessException("Error adding menu item: " + e.getMessage(), e);
        }
    }

    public List<Food> getAll(boolean availableOnly) {
        List<Food> list = new ArrayList<>();
        String sql = "SELECT food_id, name, category, price, available FROM foods" +
                (availableOnly ? " WHERE available = TRUE" : "") + " ORDER BY category, name";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Food(rs.getInt("food_id"), rs.getString("name"),
                        rs.getString("category"), rs.getBigDecimal("price"), rs.getBoolean("available")));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error loading food menu: " + e.getMessage(), e);
        }
    }

    public Food getById(int id) {
        String sql = "SELECT food_id, name, category, price, available FROM foods WHERE food_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Food(rs.getInt("food_id"), rs.getString("name"),
                            rs.getString("category"), rs.getBigDecimal("price"), rs.getBoolean("available"));
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Error finding food #" + id + ": " + e.getMessage(), e);
        }
    }

    public boolean update(Food f) {
        String sql = "UPDATE foods SET name = ?, category = ?, price = ?, available = ? WHERE food_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, f.getName());
            ps.setString(2, f.getCategory());
            ps.setBigDecimal(3, f.getPrice());
            ps.setBoolean(4, f.isAvailable());
            ps.setInt(5, f.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error updating food item: " + e.getMessage(), e);
        }
    }

    public boolean toggleAvailability(int id) {
        String sql = "UPDATE foods SET available = NOT available WHERE food_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error updating item availability: " + e.getMessage(), e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM foods WHERE food_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) {
                throw new DataAccessException("Cannot delete menu item #" + id + " because it is part of existing orders.", e);
            }
            throw new DataAccessException("Error deleting menu item: " + e.getMessage(), e);
        }
    }
}

class OrderDAO {
    public int createOrder(int customerId, Map<Integer, Integer> items) throws InvalidOrderException {
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one food item.");
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("SELECT customer_id FROM customers WHERE customer_id = ?")) {
                    ps.setInt(1, customerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new InvalidOrderException("Customer ID #" + customerId + " not found.");
                        }
                    }
                }

                BigDecimal totalAmount = BigDecimal.ZERO;
                Map<Integer, BigDecimal> unitPrices = new HashMap<>();

                for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
                    int foodId = entry.getKey();
                    int qty = entry.getValue();

                    if (qty <= 0) {
                        throw new InvalidOrderException("Quantity for item #" + foodId + " must be positive.");
                    }

                    try (PreparedStatement ps = conn.prepareStatement("SELECT name, price, available FROM foods WHERE food_id = ?")) {
                        ps.setInt(1, foodId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new InvalidOrderException("Food item #" + foodId + " does not exist.");
                            }
                            if (!rs.getBoolean("available")) {
                                throw new InvalidOrderException("Food item '" + rs.getString("name") + "' is out of stock.");
                            }
                            BigDecimal price = rs.getBigDecimal("price");
                            unitPrices.put(foodId, price);
                            totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(qty)));
                        }
                    }
                }

                int orderId;
                String insertOrderSql = "INSERT INTO orders (customer_id, status, total_amount) VALUES (?, 'PLACED', ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, customerId);
                    ps.setBigDecimal(2, totalAmount);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) throw new SQLException("Failed to obtain Order ID.");
                        orderId = rs.getInt(1);
                    }
                }

                String insertItemSql = "INSERT INTO order_items (order_id, food_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertItemSql)) {
                    for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
                        int foodId = entry.getKey();
                        int qty = entry.getValue();
                        BigDecimal price = unitPrices.get(foodId);

                        ps.setInt(1, orderId);
                        ps.setInt(2, foodId);
                        ps.setInt(3, qty);
                        ps.setBigDecimal(4, price);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
                return orderId;

            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof InvalidOrderException) {
                    throw (InvalidOrderException) ex;
                }
                throw new DataAccessException("Order transaction failed: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database connection error: " + e.getMessage(), e);
        }
    }

    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.order_id, o.customer_id, c.name AS customer_name, o.order_time, o.status, o.total_amount " +
                "FROM orders o JOIN customers c ON o.customer_id = c.customer_id ORDER BY o.order_id DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Order(
                        rs.getInt("order_id"),
                        rs.getInt("customer_id"),
                        rs.getString("customer_name"),
                        rs.getTimestamp("order_time"),
                        rs.getString("status"),
                        rs.getBigDecimal("total_amount")
                ));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error loading orders: " + e.getMessage(), e);
        }
    }

    public Order getOrderDetails(int orderId) {
        String orderSql = "SELECT o.order_id, o.customer_id, c.name AS customer_name, o.order_time, o.status, o.total_amount " +
                "FROM orders o JOIN customers c ON o.customer_id = c.customer_id WHERE o.order_id = ?";
        String itemsSql = "SELECT oi.order_item_id, oi.order_id, oi.food_id, f.name AS food_name, oi.quantity, oi.unit_price " +
                "FROM order_items oi JOIN foods f ON oi.food_id = f.food_id WHERE oi.order_id = ?";

        try (Connection conn = Database.getConnection()) {
            Order order = null;
            try (PreparedStatement ps = conn.prepareStatement(orderSql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        order = new Order(
                                rs.getInt("order_id"),
                                rs.getInt("customer_id"),
                                rs.getString("customer_name"),
                                rs.getTimestamp("order_time"),
                                rs.getString("status"),
                                rs.getBigDecimal("total_amount")
                        );
                    }
                }
            }

            if (order != null) {
                List<OrderItem> items = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(itemsSql)) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            items.add(new OrderItem(
                                    rs.getInt("order_item_id"),
                                    rs.getInt("order_id"),
                                    rs.getInt("food_id"),
                                    rs.getString("food_name"),
                                    rs.getInt("quantity"),
                                    rs.getBigDecimal("unit_price")
                            ));
                        }
                    }
                }
                order.setItems(items);
            }
            return order;
        } catch (SQLException e) {
            throw new DataAccessException("Error retrieving order #" + orderId + ": " + e.getMessage(), e);
        }
    }

    public boolean updateStatus(int orderId, String newStatus) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Error updating order status: " + e.getMessage(), e);
        }
    }
}

class PaymentDAO {
    public int recordPayment(int orderId, BigDecimal amount, PaymentMethod method) {
        String sql = "INSERT INTO payments (order_id, amount, payment_method, reference_code, payment_status) VALUES (?, ?, ?, ?, 'SUCCESS')";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, orderId);
            ps.setBigDecimal(2, amount);
            ps.setString(3, method.getMethodName());
            ps.setString(4, method.getReferenceCode());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            throw new SQLException("Failed to obtain Payment ID.");
        } catch (SQLException e) {
            throw new DataAccessException("Error saving payment: " + e.getMessage(), e);
        }
    }

    public Payment getPaymentByOrderId(int orderId) {
        String sql = "SELECT payment_id, order_id, amount, payment_method, reference_code, payment_status, payment_time " +
                "FROM payments WHERE order_id = ? AND payment_status = 'SUCCESS' ORDER BY payment_id DESC LIMIT 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Payment(
                            rs.getInt("payment_id"),
                            rs.getInt("order_id"),
                            rs.getBigDecimal("amount"),
                            rs.getString("payment_method"),
                            rs.getString("reference_code"),
                            rs.getString("payment_status"),
                            rs.getTimestamp("payment_time")
                    );
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Error retrieving payment for order #" + orderId + ": " + e.getMessage(), e);
        }
    }

    public List<Payment> getAllPayments() {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT payment_id, order_id, amount, payment_method, reference_code, payment_status, payment_time FROM payments ORDER BY payment_id DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Payment(
                        rs.getInt("payment_id"),
                        rs.getInt("order_id"),
                        rs.getBigDecimal("amount"),
                        rs.getString("payment_method"),
                        rs.getString("reference_code"),
                        rs.getString("payment_status"),
                        rs.getTimestamp("payment_time")
                ));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error loading payments: " + e.getMessage(), e);
        }
    }
}

class ReportDAO {
    public void printDashboard() {
        String custSql = "SELECT COUNT(*) FROM customers";
        String foodSql = "SELECT COUNT(*), SUM(CASE WHEN available = TRUE THEN 1 ELSE 0 END) FROM foods";
        String orderSql = "SELECT COUNT(*), COALESCE(SUM(total_amount), 0) FROM orders";
        String statusSql = "SELECT status, COUNT(*) FROM orders GROUP BY status";
        String revenueSql = "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE payment_status = 'SUCCESS'";

        try (Connection conn = Database.getConnection()) {
            int totalCust = 0;
            int totalFoods = 0, availFoods = 0;
            int totalOrders = 0;
            BigDecimal recordedOrdersTotal = BigDecimal.ZERO;
            BigDecimal collectedRevenue = BigDecimal.ZERO;
            Map<String, Integer> statusMap = new LinkedHashMap<>();

            try (PreparedStatement ps = conn.prepareStatement(custSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totalCust = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(foodSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalFoods = rs.getInt(1);
                    availFoods = rs.getInt(2);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(orderSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalOrders = rs.getInt(1);
                    recordedOrdersTotal = rs.getBigDecimal(2);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(revenueSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) collectedRevenue = rs.getBigDecimal(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(statusSql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    statusMap.put(rs.getString(1), rs.getInt(2));
                }
            }

            System.out.println("\n+-------------------------------------------------------------+");
            System.out.println("|              DINETRACK RESTAURANT DASHBOARD                 |");
            System.out.println("+-------------------------------------------------------------+");
            System.out.printf("| Total Customers Registered : %-30d |\n", totalCust);
            System.out.printf("| Menu Items Total           : %d (Available: %d, Out: %-5d) |\n",
                    totalFoods, availFoods, (totalFoods - availFoods));
            System.out.printf("| Total Orders Placed        : %-30d |\n", totalOrders);
            System.out.printf("| Total Order Sales Value    : Rs. %-26.2f |\n", recordedOrdersTotal);
            System.out.printf("| Actual Collected Revenue   : Rs. %-26.2f |\n", collectedRevenue);
            System.out.println("+-------------------------------------------------------------+");
            System.out.println("| Orders by Status Breakdown:                                 |");
            for (String s : Arrays.asList("PLACED", "PREPARING", "READY", "COMPLETED", "CANCELLED")) {
                int count = statusMap.getOrDefault(s, 0);
                System.out.printf("|   - %-12s : %-39d |\n", s, count);
            }
            System.out.println("+-------------------------------------------------------------+");

        } catch (SQLException e) {
            throw new DataAccessException("Error compiling dashboard report: " + e.getMessage(), e);
        }
    }

    public void printTopSellingFoods() {
        String sql = "SELECT f.name, f.category, SUM(oi.quantity) AS total_qty, SUM(oi.quantity * oi.unit_price) AS sales " +
                "FROM order_items oi JOIN foods f ON oi.food_id = f.food_id " +
                "JOIN orders o ON oi.order_id = o.order_id " +
                "WHERE o.status != 'CANCELLED' " +
                "GROUP BY f.food_id, f.name, f.category " +
                "ORDER BY total_qty DESC LIMIT 10";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n==========================================================================");
            System.out.println("                 TOP SELLING FOOD ITEMS (BY QUANTITY)                     ");
            System.out.println("==========================================================================");
            System.out.printf("%-4s | %-24s | %-14s | %-10s | %-12s\n", "#", "Item Name", "Category", "Qty Sold", "Sales (Rs.)");
            System.out.println("-----+--------------------------+----------------+------------+------------");

            int rank = 1;
            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                System.out.printf("%-4d | %-24s | %-14s | %-10d | Rs.%10.2f\n",
                        rank++, rs.getString("name"), rs.getString("category"),
                        rs.getInt("total_qty"), rs.getBigDecimal("sales"));
            }
            if (!hasData) {
                System.out.println("  No sales records found yet.");
            }
            System.out.println("==========================================================================");
        } catch (SQLException e) {
            throw new DataAccessException("Error loading top selling foods: " + e.getMessage(), e);
        }
    }
}
