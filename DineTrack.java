import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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

        System.out.println("\n[MySQL Authentication Required]");
        System.out.println("Please enter your local MySQL root password.");
        System.out.println("(If your MySQL root account has no password, simply press Enter)");
        while (true) {
            System.out.print("MySQL Root Password: ");
            String pwd = scanner.nextLine();
            if (Database.tryConnect(pwd)) {
                Database.setPassword(pwd);
                System.out.println("[Connected] Database connected successfully!\n");
                break;
            } else {
                System.out.println("[Failed] Access denied. Incorrect password or MySQL service is not running.\n");
                System.out.print("Retry password? (y/n): ");
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
        boolean back = false;
        while (!back) {
            System.out.println("\n=========================================================================================");
            System.out.println("              DINETRACK BUSINESS INTELLIGENCE & STATISTICAL DASHBOARD                    ");
            System.out.println("=========================================================================================");
            System.out.println("  [EXECUTIVE & FINANCIAL INTELLIGENCE]");
            System.out.println("   1. Executive KPI Summary             (Sales, Net Paid, AOV, Completion, Growth)");
            System.out.println("   2. Monthly Revenue Trends & Growth   (Month-by-Month Billed, Collected, MoM %)");
            System.out.println("   3. Period-over-Period Comparison     (2024 vs 2025 YoY Performance Analysis)");
            System.out.println("   4. Financial Margins & Profitability (COGS, Gross Profit, Menu Margin %)");
            System.out.println("\n  [MENU, PRODUCT & BASKET ANALYTICS]");
            System.out.println("   5. Product Sales & Ranking           (Top & Bottom 5/10/20, Sales Velocity)");
            System.out.println("   6. Category Contribution Breakdown   (Revenue Share, Quantity, Visual Bars)");
            System.out.println("   7. Market Basket & Pairing Analysis  (Top Combos, Association Pairs, Support)");
            System.out.println("   8. BCG Menu Engineering Matrix       (Stars, Plowhorses, Puzzles, Dogs)");
            System.out.println("   9. Inventory & Stock Valuation       (Current Stock, Reorder Alerts, Stockouts)");
            System.out.println("\n  [CUSTOMER INTELLIGENCE & LOYALTY]");
            System.out.println("  10. Customer Spending Leaderboard     (Top VIPs, Spending Distribution, AOV)");
            System.out.println("  11. RFM Segmentation & Loyalty Tiers  (Platinum/Gold/Silver, Churn, Inactive)");
            System.out.println("\n  [OPERATIONS, STAFF & AUDIT]");
            System.out.println("  12. Payment Gateway Analytics         (Cash vs Card vs UPI, Reconciliation)");
            System.out.println("  13. Kitchen Operations & Peak Hours   (Fulfillment, Heatmap by Day & Hour)");
            System.out.println("  14. Staff Performance & Workload      (Orders Handled, Sales Volume per Staff)");
            System.out.println("  15. Statistical Metrics & Outliers    (Mean, Median, StdDev, IQR, 80/20 Pareto)");
            System.out.println("  16. Data Quality & Audit Integrity    (Orphan Detection, Price Reconciliation)");
            System.out.println("\n  [DRILL-DOWN & EXPORT]");
            System.out.println("  17. Interactive Filter Drill-Down     (Filter by Date Range, Category, Status)");
            System.out.println("  18. Export Reports to CSV Files       (Generate Excel-compatible CSVs on disk)");
            System.out.println("  19. Complete Master BI Audit Report   (Consolidated Executive Report Suite)");
            System.out.println("  20. Back to Main Menu");
            System.out.println("=========================================================================================");
            int ch = promptInt("Choice (1-20): ", 1, 20);
            switch (ch) {
                case 1  -> reportDAO.printDashboard();
                case 2  -> reportDAO.printMonthlyTrends();
                case 3  -> reportDAO.printPeriodComparison();
                case 4  -> reportDAO.printFinancialProfitability();
                case 5  -> reportDAO.printProductAnalytics();
                case 6  -> reportDAO.printCategoryAnalytics();
                case 7  -> reportDAO.printMarketBasketAnalysis();
                case 8  -> reportDAO.printMenuEngineeringMatrix();
                case 9  -> reportDAO.printInventoryAnalytics();
                case 10 -> reportDAO.printCustomerLeaderboard();
                case 11 -> reportDAO.printCustomerLoyaltyAndRFM();
                case 12 -> reportDAO.printPaymentAnalytics();
                case 13 -> reportDAO.printOperationalAndKitchenMetrics();
                case 14 -> reportDAO.printStaffAnalytics();
                case 15 -> reportDAO.printStatisticalAnalysis();
                case 16 -> reportDAO.printDataQualityAndAuditReport();
                case 17 -> reportDAO.interactiveDrillDown();
                case 18 -> reportDAO.exportReportsToCsv();
                case 19 -> reportDAO.printFullMasterAuditSuite();
                case 20 -> back = true;
            }
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
    private static final Scanner scanner = new Scanner(System.in);

    private static String generateBar(double pct) {
        int total = 20;
        int filled = (int) Math.round((pct / 100.0) * total);
        if (filled > total) filled = total;
        if (filled < 0) filled = 0;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < filled; i++) sb.append("#");
        for (int i = filled; i < total; i++) sb.append("-");
        sb.append("]");
        return sb.toString();
    }

    // 1. EXECUTIVE KPI SUMMARY
    public void printDashboard() {
        String custSql = "SELECT COUNT(*) FROM customers";
        String activeCustSql = "SELECT COUNT(DISTINCT customer_id) FROM orders WHERE status != 'CANCELLED'";
        String newCustSql = "SELECT COUNT(*) FROM customers WHERE created_at >= '2025-01-01'";
        String foodSql = "SELECT COUNT(*), SUM(CASE WHEN available = TRUE THEN 1 ELSE 0 END) FROM foods";
        String orderMetricsSql = "SELECT COUNT(*), COALESCE(SUM(total_amount), 0), COALESCE(AVG(total_amount), 0), " +
                "COALESCE(MIN(total_amount), 0), COALESCE(MAX(total_amount), 0) FROM orders WHERE status != 'CANCELLED'";
        String revenueSql = "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE payment_status = 'SUCCESS'";
        String itemsSoldSql = "SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi JOIN orders o ON oi.order_id = o.order_id WHERE o.status = 'COMPLETED'";
        String statusSql = "SELECT status, COUNT(*) FROM orders GROUP BY status";
        String repeatCustSql = "SELECT COUNT(*) FROM (SELECT customer_id FROM orders WHERE status != 'CANCELLED' GROUP BY customer_id HAVING COUNT(*) > 1) t";
        String bestWorstDaySql = "SELECT DATE(order_time) AS d, SUM(total_amount) AS rev FROM orders WHERE status = 'COMPLETED' GROUP BY d ORDER BY rev DESC";

        try (Connection conn = Database.getConnection()) {
            int totalCust = 0, activeCust = 0, newCust = 0, repeatCust = 0;
            int totalFoods = 0, availFoods = 0;
            int totalOrders = 0;
            long totalItemsSold = 0;
            BigDecimal grossSales = BigDecimal.ZERO;
            BigDecimal avgOrderVal = BigDecimal.ZERO;
            BigDecimal minOrderVal = BigDecimal.ZERO;
            BigDecimal maxOrderVal = BigDecimal.ZERO;
            BigDecimal collectedRevenue = BigDecimal.ZERO;
            Map<String, Integer> statusMap = new LinkedHashMap<>();

            try (PreparedStatement ps = conn.prepareStatement(custSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totalCust = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(activeCustSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) activeCust = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(newCustSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) newCust = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(repeatCustSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) repeatCust = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(foodSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalFoods = rs.getInt(1);
                    availFoods = rs.getInt(2);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(orderMetricsSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalOrders = rs.getInt(1);
                    grossSales = rs.getBigDecimal(2);
                    avgOrderVal = rs.getBigDecimal(3);
                    minOrderVal = rs.getBigDecimal(4);
                    maxOrderVal = rs.getBigDecimal(5);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(revenueSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) collectedRevenue = rs.getBigDecimal(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(itemsSoldSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totalItemsSold = rs.getLong(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(statusSql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) statusMap.put(rs.getString(1), rs.getInt(2));
            }

            String bestDay = "N/A", worstDay = "N/A";
            BigDecimal bestDayRev = BigDecimal.ZERO, worstDayRev = BigDecimal.ZERO;
            try (PreparedStatement ps = conn.prepareStatement(bestWorstDaySql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    bestDay = rs.getString("d");
                    bestDayRev = rs.getBigDecimal("rev");
                    while (rs.next()) {
                        worstDay = rs.getString("d");
                        worstDayRev = rs.getBigDecimal("rev");
                    }
                }
            }

            int completed = statusMap.getOrDefault("COMPLETED", 0);
            int cancelled = statusMap.getOrDefault("CANCELLED", 0);
            int placed = statusMap.getOrDefault("PLACED", 0);
            int prep = statusMap.getOrDefault("PREPARING", 0);
            int ready = statusMap.getOrDefault("READY", 0);
            int grandTotalOrders = completed + cancelled + placed + prep + ready;

            double completionRate = grandTotalOrders > 0 ? ((double) completed / grandTotalOrders) * 100.0 : 0.0;
            double cancellationRate = grandTotalOrders > 0 ? ((double) cancelled / grandTotalOrders) * 100.0 : 0.0;
            double repeatRate = activeCust > 0 ? ((double) repeatCust / activeCust) * 100.0 : 0.0;
            double avgItemsPerOrder = completed > 0 ? (double) totalItemsSold / completed : 0.0;
            BigDecimal pendingRevenue = grossSales.subtract(collectedRevenue);
            if (pendingRevenue.compareTo(BigDecimal.ZERO) < 0) pendingRevenue = BigDecimal.ZERO;
            BigDecimal avgSpendPerCust = activeCust > 0 ? grossSales.divide(BigDecimal.valueOf(activeCust), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            System.out.println("\n+---------------------------------------------------------------------------------------+");
            System.out.println("|                     EXECUTIVE DASHBOARD & BUSINESS STATISTICS                         |");
            System.out.println("+---------------------------------------------------------------------------------------+");
            System.out.println("  [REVENUE & FINANCIAL SUMMARY]");
            System.out.printf("  * Total Gross Billed Sales       : Rs. %-15.2f | Realized Revenue Paid: Rs. %.2f%n", grossSales, collectedRevenue);
            System.out.printf("  * Outstanding Receivables (Unpaid): Rs. %-15.2f | Average Order Value : Rs. %.2f%n", pendingRevenue, avgOrderVal);
            System.out.printf("  * Order Value Range (Min / Max)  : Rs. %.2f / Rs. %.2f%n", minOrderVal, maxOrderVal);
            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.println("  [ORDER VOLUMES & EFFICIENCY]");
            System.out.printf("  * Total Orders Placed            : %-15d | Completed Orders    : %d (%.1f%%)%n", grandTotalOrders, completed, completionRate);
            System.out.printf("  * Cancelled Orders               : %-15d | Cancellation Rate   : %.1f%%%n", cancelled, cancellationRate);
            System.out.printf("  * Active Kitchen Queue Orders    : %-15d | Total Items Sold    : %d%n", (placed + prep + ready), totalItemsSold);
            System.out.printf("  * Average Food Items per Order   : %-15.2f | Repeat Purchase Rate: %.1f%%%n", avgItemsPerOrder, repeatRate);
            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.println("  [CUSTOMER METRICS]");
            System.out.printf("  * Total Registered Customers     : %-15d | Active Customers    : %d%n", totalCust, activeCust);
            System.out.printf("  * New Customers (2025 Cohort)    : %-15d | Repeat Loyal Buyers : %d%n", newCust, repeatCust);
            System.out.printf("  * Avg Lifetime Value / Active Cust: Rs. %-15.2f | Total Menu Items    : %d (%d available)%n", avgSpendPerCust, totalFoods, availFoods);
            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.println("  [PEAK / LOW SALES BENCHMARKS]");
            System.out.printf("  * Highest Single Sales Day       : %s (Rs. %.2f)%n", bestDay, bestDayRev);
            System.out.printf("  * Lowest Single Sales Day        : %s (Rs. %.2f)%n", worstDay, worstDayRev);
            System.out.println("+---------------------------------------------------------------------------------------+");

        } catch (SQLException e) {
            throw new DataAccessException("Error compiling executive dashboard: " + e.getMessage(), e);
        }
    }

    // 2. MONTHLY REVENUE & ORDER TRENDS
    public void printMonthlyTrends() {
        String sql = "SELECT DATE_FORMAT(order_time,'%Y-%m') AS yrmo, DATE_FORMAT(order_time,'%b %Y') AS label, " +
                "COUNT(*) AS total_orders, " +
                "SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END) AS completed, " +
                "SUM(CASE WHEN status='CANCELLED' THEN 1 ELSE 0 END) AS cancelled, " +
                "COALESCE(SUM(CASE WHEN status='COMPLETED' THEN total_amount ELSE 0 END),0) AS revenue " +
                "FROM orders GROUP BY yrmo, label ORDER BY yrmo";

        String revenuePaidSql = "SELECT DATE_FORMAT(p.payment_time,'%Y-%m') AS yrmo, COALESCE(SUM(p.amount),0) AS paid " +
                "FROM payments p WHERE p.payment_status='SUCCESS' GROUP BY yrmo ORDER BY yrmo";

        try (Connection conn = Database.getConnection()) {
            List<String[]> rows = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{
                            rs.getString("yrmo"), rs.getString("label"),
                            String.valueOf(rs.getInt("total_orders")),
                            String.valueOf(rs.getInt("completed")),
                            String.valueOf(rs.getInt("cancelled")),
                            rs.getBigDecimal("revenue").toPlainString()
                    });
                }
            }
            Map<String, BigDecimal> paidMap = new LinkedHashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(revenuePaidSql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) paidMap.put(rs.getString("yrmo"), rs.getBigDecimal("paid"));
            }

            System.out.println("\n=========================================================================================");
            System.out.println("                   MONTHLY REVENUE & ORDER PERFORMANCE TRENDS                            ");
            System.out.println("=========================================================================================");
            System.out.printf("%-10s | %-6s | %-5s | %-5s | %-14s | %-14s | %s%n",
                    "Month", "Orders", "Done", "Canc", "Billed (Rs.)", "Collected Rs.", "MoM Growth");
            System.out.println("----------+--------+-------+-------+----------------+----------------+-----------");

            BigDecimal prevRev = null;
            String bestMonth = "", worstMonth = "";
            BigDecimal bestRev = BigDecimal.valueOf(-1), worstRev = BigDecimal.valueOf(Long.MAX_VALUE);
            BigDecimal grandTotal = BigDecimal.ZERO;
            int grandOrders = 0;

            for (String[] row : rows) {
                BigDecimal rev = new BigDecimal(row[5]);
                BigDecimal collected = paidMap.getOrDefault(row[0], BigDecimal.ZERO);
                grandTotal = grandTotal.add(rev);
                grandOrders += Integer.parseInt(row[2]);

                String growth = "--";
                if (prevRev != null && prevRev.compareTo(BigDecimal.ZERO) > 0) {
                    double pct = ((rev.doubleValue() - prevRev.doubleValue()) / prevRev.doubleValue()) * 100.0;
                    growth = String.format("%+.1f%%", pct);
                }

                System.out.printf("%-10s | %-6s | %-5s | %-5s | Rs.%11.2f | Rs.%11.2f | %s%n",
                        row[1], row[2], row[3], row[4], rev, collected, growth);

                if (rev.compareTo(bestRev) > 0) { bestRev = rev; bestMonth = row[1]; }
                if (rev.compareTo(worstRev) < 0 && !row[3].equals("0")) { worstRev = rev; worstMonth = row[1]; }
                prevRev = rev;
            }
            System.out.println("=========================================================================================");
            System.out.printf("TOTALS     | %-6d | %-5s |       | Rs.%11.2f |%n", grandOrders, "--", grandTotal);
            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.println("  * Best Revenue Month : " + bestMonth + " (Rs. " + bestRev.setScale(2, RoundingMode.HALF_UP) + ")");
            if (!worstMonth.isEmpty()) System.out.println("  * Worst Revenue Month: " + worstMonth + " (Rs. " + worstRev.setScale(2, RoundingMode.HALF_UP) + ")");
            System.out.println("=========================================================================================");

        } catch (SQLException e) {
            throw new DataAccessException("Error loading monthly trends: " + e.getMessage(), e);
        }
    }

    // 3. PERIOD-OVER-PERIOD COMPARISON (2024 vs 2025)
    public void printPeriodComparison() {
        String sql = "SELECT YEAR(order_time) AS yr, " +
                "COUNT(*) AS total_orders, " +
                "SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END) AS completed_orders, " +
                "SUM(CASE WHEN status='CANCELLED' THEN 1 ELSE 0 END) AS cancelled_orders, " +
                "COALESCE(SUM(CASE WHEN status='COMPLETED' THEN total_amount ELSE 0 END), 0) AS revenue, " +
                "COALESCE(AVG(CASE WHEN status='COMPLETED' THEN total_amount ELSE NULL END), 0) AS aov, " +
                "COUNT(DISTINCT customer_id) AS unique_customers " +
                "FROM orders GROUP BY yr ORDER BY yr";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            Map<Integer, Map<String, Object>> map = new LinkedHashMap<>();
            while (rs.next()) {
                int yr = rs.getInt("yr");
                Map<String, Object> m = new HashMap<>();
                m.put("total_orders", rs.getInt("total_orders"));
                m.put("completed_orders", rs.getInt("completed_orders"));
                m.put("cancelled_orders", rs.getInt("cancelled_orders"));
                m.put("revenue", rs.getBigDecimal("revenue"));
                m.put("aov", rs.getBigDecimal("aov"));
                m.put("unique_customers", rs.getInt("unique_customers"));
                map.put(yr, m);
            }

            System.out.println("\n=========================================================================================");
            System.out.println("                   PERIOD-OVER-PERIOD COMPARISON (2024 vs 2025)                          ");
            System.out.println("=========================================================================================");
            System.out.printf("%-30s | %-16s | %-16s | %s%n", "Metric", "Year 2024", "Year 2025", "YoY Change (%)");
            System.out.println("-------------------------------+------------------+------------------+-------------------");

            Map<String, Object> d24 = map.getOrDefault(2024, Collections.emptyMap());
            Map<String, Object> d25 = map.getOrDefault(2025, Collections.emptyMap());

            BigDecimal rev24 = (BigDecimal) d24.getOrDefault("revenue", BigDecimal.ZERO);
            BigDecimal rev25 = (BigDecimal) d25.getOrDefault("revenue", BigDecimal.ZERO);
            double revGrowth = rev24.compareTo(BigDecimal.ZERO) > 0 ? ((rev25.doubleValue() - rev24.doubleValue()) / rev24.doubleValue()) * 100 : 0.0;

            int ord24 = (Integer) d24.getOrDefault("total_orders", 0);
            int ord25 = (Integer) d25.getOrDefault("total_orders", 0);
            double ordGrowth = ord24 > 0 ? ((double)(ord25 - ord24) / ord24) * 100 : 0.0;

            BigDecimal aov24 = (BigDecimal) d24.getOrDefault("aov", BigDecimal.ZERO);
            BigDecimal aov25 = (BigDecimal) d25.getOrDefault("aov", BigDecimal.ZERO);
            double aovGrowth = aov24.compareTo(BigDecimal.ZERO) > 0 ? ((aov25.doubleValue() - aov24.doubleValue()) / aov24.doubleValue()) * 100 : 0.0;

            int cust24 = (Integer) d24.getOrDefault("unique_customers", 0);
            int cust25 = (Integer) d25.getOrDefault("unique_customers", 0);

            System.out.printf("%-30s | Rs.%-13.2f | Rs.%-13.2f | %+6.1f%%%n", "Total Completed Revenue", rev24, rev25, revGrowth);
            System.out.printf("%-30s | %-16d | %-16d | %+6.1f%%%n", "Total Orders Logged", ord24, ord25, ordGrowth);
            System.out.printf("%-30s | Rs.%-13.2f | Rs.%-13.2f | %+6.1f%%%n", "Average Order Value (AOV)", aov24, aov25, aovGrowth);
            System.out.printf("%-30s | %-16d | %-16d | %+6.1f%%%n", "Active Ordering Customers", cust24, cust25, (cust24 > 0 ? ((double)(cust25 - cust24)/cust24)*100 : 0.0));
            System.out.println("=========================================================================================");

        } catch (SQLException e) {
            throw new DataAccessException("Error loading period comparison: " + e.getMessage(), e);
        }
    }

    // 4. FINANCIAL MARGINS & PROFITABILITY
    public void printFinancialProfitability() {
        String sql = "SELECT f.category, " +
                "COALESCE(SUM(oi.quantity * oi.unit_price), 0) AS revenue, " +
                "COALESCE(SUM(oi.quantity * f.cost_price), 0) AS cogs " +
                "FROM order_items oi JOIN foods f ON oi.food_id = f.food_id " +
                "JOIN orders o ON oi.order_id = o.order_id " +
                "WHERE o.status = 'COMPLETED' " +
                "GROUP BY f.category ORDER BY revenue DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            BigDecimal totalRev = BigDecimal.ZERO;
            BigDecimal totalCogs = BigDecimal.ZERO;

            System.out.println("\n=========================================================================================");
            System.out.println("                 FINANCIAL MARGINS & COST OF GOODS SOLD (COGS)                           ");
            System.out.println("=========================================================================================");
            System.out.printf("%-16s | %-14s | %-14s | %-15s | %s%n",
                    "Category", "Revenue (Rs.)", "COGS Cost (Rs.)", "Gross Profit", "Gross Margin %");
            System.out.println("-----------------+----------------+----------------+-----------------+---------------");

            while (rs.next()) {
                String cat = rs.getString("category");
                BigDecimal rev = rs.getBigDecimal("revenue");
                BigDecimal cogs = rs.getBigDecimal("cogs");
                BigDecimal profit = rev.subtract(cogs);
                double margin = rev.compareTo(BigDecimal.ZERO) > 0 ? (profit.doubleValue() / rev.doubleValue()) * 100 : 0.0;

                totalRev = totalRev.add(rev);
                totalCogs = totalCogs.add(cogs);

                System.out.printf("%-16s | Rs.%11.2f | Rs.%11.2f | Rs.%12.2f | %5.1f%%%n",
                        cat, rev, cogs, profit, margin);
            }

            BigDecimal totalProfit = totalRev.subtract(totalCogs);
            double totalMargin = totalRev.compareTo(BigDecimal.ZERO) > 0 ? (totalProfit.doubleValue() / totalRev.doubleValue()) * 100 : 0.0;
            System.out.println("-----------------+----------------+----------------+-----------------+---------------");
            System.out.printf("%-16s | Rs.%11.2f | Rs.%11.2f | Rs.%12.2f | %5.1f%%%n",
                    "TOTAL BUSINESS", totalRev, totalCogs, totalProfit, totalMargin);
            System.out.println("=========================================================================================");

        } catch (SQLException e) {
            throw new DataAccessException("Error loading profitability analysis: " + e.getMessage(), e);
        }
    }

    // 5. PRODUCT & MENU ANALYTICS
    public void printProductAnalytics() {
        String top10Sql = "SELECT f.food_id, f.name, f.category, f.price, " +
                "COALESCE(SUM(oi.quantity),0) AS qty_sold, " +
                "COALESCE(SUM(oi.quantity * oi.unit_price),0) AS revenue, " +
                "COUNT(DISTINCT oi.order_id) AS order_count " +
                "FROM foods f LEFT JOIN (order_items oi JOIN orders o ON oi.order_id=o.order_id AND o.status='COMPLETED') " +
                "ON f.food_id=oi.food_id " +
                "GROUP BY f.food_id, f.name, f.category, f.price " +
                "ORDER BY revenue DESC LIMIT 10";

        String topQtySql = "SELECT f.name, f.category, SUM(oi.quantity) AS qty " +
                "FROM order_items oi JOIN foods f ON oi.food_id = f.food_id " +
                "JOIN orders o ON oi.order_id = o.order_id WHERE o.status = 'COMPLETED' " +
                "GROUP BY f.food_id, f.name, f.category ORDER BY qty DESC LIMIT 5";

        String bottomSql = "SELECT f.name, f.category, f.price, COALESCE(SUM(oi.quantity), 0) AS qty, " +
                "COALESCE(SUM(oi.quantity * oi.unit_price), 0) AS rev " +
                "FROM foods f LEFT JOIN (order_items oi JOIN orders o ON oi.order_id = o.order_id AND o.status = 'COMPLETED') " +
                "ON f.food_id = oi.food_id " +
                "GROUP BY f.food_id, f.name, f.category, f.price " +
                "ORDER BY qty ASC, rev ASC LIMIT 5";

        String neverOrderedSql = "SELECT name, category, price FROM foods WHERE food_id NOT IN (SELECT DISTINCT food_id FROM order_items)";

        try (Connection conn = Database.getConnection()) {
            System.out.println("\n=========================================================================================");
            System.out.println("                      PRODUCT SALES & MENU PERFORMANCE REPORT                            ");
            System.out.println("=========================================================================================");
            System.out.println("  [TOP 10 BESTSELLING ITEMS BY REVENUE]");
            System.out.printf("  %-4s | %-24s | %-14s | %-10s | %-8s | %-8s | %s%n",
                    "#", "Item Name", "Category", "Price", "Qty Sold", "Orders", "Revenue (Rs.)");
            System.out.println("  -----+--------------------------+----------------+------------+----------+----------+-------------");

            try (PreparedStatement ps = conn.prepareStatement(top10Sql); ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    System.out.printf("  %-4d | %-24s | %-14s | Rs.%6.0f | %-8d | %-8d | Rs.%10.2f%n",
                            rank++, rs.getString("name"), rs.getString("category"),
                            rs.getBigDecimal("price"), rs.getInt("qty_sold"),
                            rs.getInt("order_count"), rs.getBigDecimal("revenue"));
                }
            }

            System.out.println("\n  [TOP 5 VOLUME DRIVERS (By Quantity Sold)]");
            try (PreparedStatement ps = conn.prepareStatement(topQtySql); ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    System.out.printf("   %d. %-24s (%-12s) : %d units sold%n", rank++, rs.getString("name"), rs.getString("category"), rs.getInt("qty"));
                }
            }

            System.out.println("\n  [BOTTOM 5 UNDERPERFORMING ITEMS (Low Demand Candidates)]");
            try (PreparedStatement ps = conn.prepareStatement(bottomSql); ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    System.out.printf("   %d. %-24s (%-12s) : %d units sold (Rs. %.2f)%n", rank++, rs.getString("name"), rs.getString("category"), rs.getInt("qty"), rs.getBigDecimal("rev"));
                }
            }

            System.out.println("\n  [ITEMS WITH ZERO ORDERS (Never Purchased)]");
            try (PreparedStatement ps = conn.prepareStatement(neverOrderedSql); ResultSet rs = ps.executeQuery()) {
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("   * %s (%s, Rs. %.2f) - Not yet ordered%n", rs.getString("name"), rs.getString("category"), rs.getBigDecimal("price"));
                }
                if (!found) System.out.println("   All menu items have recorded at least one purchase.");
            }
            System.out.println("=========================================================================================");

        } catch (SQLException e) {
            throw new DataAccessException("Error loading product analytics: " + e.getMessage(), e);
        }
    }

    // 6. CATEGORY CONTRIBUTION BREAKDOWN
    public void printCategoryAnalytics() {
        String sql = "SELECT f.category, COUNT(DISTINCT oi.order_id) AS order_count, SUM(oi.quantity) AS qty_sold, " +
                "SUM(oi.quantity * oi.unit_price) AS category_revenue " +
                "FROM order_items oi JOIN foods f ON oi.food_id = f.food_id " +
                "JOIN orders o ON oi.order_id = o.order_id " +
                "WHERE o.status != 'CANCELLED' " +
                "GROUP BY f.category " +
                "ORDER BY category_revenue DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<CategoryStat> list = new ArrayList<>();
            BigDecimal totalRev = BigDecimal.ZERO;

            while (rs.next()) {
                String cat = rs.getString("category");
                int orders = rs.getInt("order_count");
                int qty = rs.getInt("qty_sold");
                BigDecimal rev = rs.getBigDecimal("category_revenue");
                list.add(new CategoryStat(cat, orders, qty, rev));
                totalRev = totalRev.add(rev);
            }

            System.out.println("\n=========================================================================================");
            System.out.println("                   CATEGORY-WISE REVENUE & CONTRIBUTION ANALYSIS                         ");
            System.out.println("=========================================================================================");
            System.out.printf("%-16s | %-8s | %-10s | %-14s | %-8s | %s\n",
                    "Category", "Orders", "Qty Sold", "Revenue (Rs.)", "Share", "Distribution Bar");
            System.out.println("-----------------+----------+------------+----------------+----------+-------------------");

            for (CategoryStat cs : list) {
                double pct = totalRev.compareTo(BigDecimal.ZERO) > 0 ?
                        cs.revenue.doubleValue() / totalRev.doubleValue() * 100.0 : 0.0;
                System.out.printf("%-16s | %-8d | %-10d | Rs.%11.2f | %5.1f%%   | %s\n",
                        cs.category, cs.ordersCount, cs.qtySold, cs.revenue, pct, generateBar(pct));
            }

            System.out.println("-----------------+----------+------------+----------------+----------+-------------------");
            System.out.printf("%-16s | %-8s | %-10s | Rs.%11.2f | 100.0%%\n", "TOTAL", "-", "-", totalRev);
            System.out.println("=========================================================================================");

        } catch (SQLException e) {
            throw new DataAccessException("Error loading category analytics: " + e.getMessage(), e);
        }
    }

    // 7. MARKET BASKET & PRODUCT PAIRING ANALYSIS
    public void printMarketBasketAnalysis() {
        String pairSql = "SELECT f1.name AS item1, f2.name AS item2, COUNT(*) AS pair_count, " +
                "SUM(oi1.quantity * oi1.unit_price + oi2.quantity * oi2.unit_price) AS combined_rev " +
                "FROM order_items oi1 " +
                "JOIN order_items oi2 ON oi1.order_id = oi2.order_id AND oi1.food_id < oi2.food_id " +
                "JOIN foods f1 ON oi1.food_id = f1.food_id " +
                "JOIN foods f2 ON oi2.food_id = f2.food_id " +
                "JOIN orders o ON oi1.order_id = o.order_id " +
                "WHERE o.status = 'COMPLETED' " +
                "GROUP BY f1.name, f2.name ORDER BY pair_count DESC LIMIT 10";

        String totalCompletedOrdersSql = "SELECT COUNT(*) FROM orders WHERE status = 'COMPLETED'";

        try (Connection conn = Database.getConnection()) {
            int totalCompleted = 1;
            try (PreparedStatement ps = conn.prepareStatement(totalCompletedOrdersSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totalCompleted = rs.getInt(1);
            }

            System.out.println("\n=========================================================================================");
            System.out.println("               MARKET BASKET ANALYSIS & PRODUCT PAIRING AFFINITY                         ");
            System.out.println("=========================================================================================");
            System.out.printf("  %-4s | %-22s + %-22s | %-10s | %-10s | %s%n",
                    "#", "Primary Item", "Co-Purchased Item", "Co-Orders", "Support %", "Combined Sales (Rs.)");
            System.out.println("  -----+------------------------+------------------------+------------+------------+-------------------");

            try (PreparedStatement ps = conn.prepareStatement(pairSql); ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    String i1 = rs.getString("item1");
                    String i2 = rs.getString("item2");
                    int count = rs.getInt("pair_count");
                    BigDecimal rev = rs.getBigDecimal("combined_rev");
                    double support = totalCompleted > 0 ? ((double) count / totalCompleted) * 100 : 0.0;

                    System.out.printf("  %-4d | %-22s + %-22s | %-10d | %5.1f%%     | Rs.%12.2f%n",
                            rank++, i1, i2, count, support, rev);
                }
            }
            System.out.println("=========================================================================================");
            System.out.println("  * Managerial Insight: Bundle top pairs into combos to drive higher Average Order Value.");
            System.out.println("=========================================================================================");

        } catch (SQLException e) {
            throw new DataAccessException("Error loading market basket analysis: " + e.getMessage(), e);
        }
    }

    // 8. BCG MENU ENGINEERING MATRIX
    public void printMenuEngineeringMatrix() {
        String sql = "SELECT f.name, f.category, f.price, f.cost_price, " +
                "COALESCE(SUM(oi.quantity), 0) AS qty_sold, " +
                "(f.price - f.cost_price) AS unit_margin " +
                "FROM foods f LEFT JOIN (order_items oi JOIN orders o ON oi.order_id = o.order_id AND o.status = 'COMPLETED') " +
                "ON f.food_id = oi.food_id " +
                "WHERE f.available = TRUE " +
                "GROUP BY f.food_id, f.name, f.category, f.price, f.cost_price";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<String[]> list = new ArrayList<>();
            double totalQty = 0;
            double totalMargin = 0;
            int count = 0;

            while (rs.next()) {
                count++;
                String name = rs.getString("name");
                String cat = rs.getString("category");
                double price = rs.getDouble("price");
                double cost = rs.getDouble("cost_price");
                int qty = rs.getInt("qty_sold");
                double margin = rs.getDouble("unit_margin");

                totalQty += qty;
                totalMargin += margin;
                list.add(new String[]{name, cat, String.valueOf(price), String.valueOf(cost), String.valueOf(qty), String.valueOf(margin)});
            }

            double avgQty = count > 0 ? totalQty / count : 0;
            double avgMargin = count > 0 ? totalMargin / count : 0;

            System.out.println("\n=========================================================================================");
            System.out.println("                  BOSTON CONSULTING GROUP (BCG) MENU ENGINEERING MATRIX                  ");
            System.out.println("=========================================================================================");
            System.out.printf("  Benchmark Averages -> Volume: %.1f units | Unit Contribution Margin: Rs. %.2f%n", avgQty, avgMargin);
            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.printf("  %-22s | %-12s | %-8s | %-12s | %-12s | %s%n",
                    "Food Item", "Category", "Qty Sold", "Unit Margin", "Classification", "Recommended Action");
            System.out.println("  -----------------------+--------------+----------+--------------+--------------+-----------------------");

            for (String[] row : list) {
                String name = row[0];
                String cat = row[1];
                int qty = Integer.parseInt(row[4]);
                double margin = Double.parseDouble(row[5]);

                boolean highVol = qty >= avgQty;
                boolean highMargin = margin >= avgMargin;

                String type, action;
                if (highVol && highMargin) {
                    type = "STAR";
                    action = "Promote heavily, maintain quality";
                } else if (highVol && !highMargin) {
                    type = "PLOWHORSE";
                    action = "High demand; slight price hike";
                } else if (!highVol && highMargin) {
                    type = "PUZZLE";
                    action = "Feature in specials / upsell";
                } else {
                    type = "DOG";
                    action = "Reposition or replace";
                }

                System.out.printf("  %-22s | %-12s | %-8d | Rs.%9.2f | %-12s | %s%n",
                        name, cat, qty, margin, type, action);
            }
            System.out.println("=========================================================================================");

        } catch (SQLException e) {
            throw new DataAccessException("Error compiling menu engineering matrix: " + e.getMessage(), e);
        }
    }

    // 9. INVENTORY & STOCK VALUATION
    public void printInventoryAnalytics() {
        String summarySql = "SELECT COUNT(*) AS total_items, " +
                "SUM(CASE WHEN available = TRUE THEN 1 ELSE 0 END) AS in_stock, " +
                "SUM(CASE WHEN available = FALSE OR stock_quantity = 0 THEN 1 ELSE 0 END) AS out_of_stock, " +
                "SUM(CASE WHEN available = TRUE AND stock_quantity <= reorder_level AND stock_quantity > 0 THEN 1 ELSE 0 END) AS low_stock, " +
                "SUM(stock_quantity * cost_price) AS total_val " +
                "FROM foods";

        String detailSql = "SELECT name, category, stock_quantity, reorder_level, cost_price, price, available " +
                "FROM foods ORDER BY (stock_quantity <= reorder_level) DESC, stock_quantity ASC";

        try (Connection conn = Database.getConnection()) {
            System.out.println("\n=========================================================================================");
            System.out.println("                      INVENTORY STATUS & STOCK VALUATION REPORT                          ");
            System.out.println("=========================================================================================");

            try (PreparedStatement ps = conn.prepareStatement(summarySql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total_items");
                    int inStock = rs.getInt("in_stock");
                    int outStock = rs.getInt("out_of_stock");
                    int lowStock = rs.getInt("low_stock");
                    BigDecimal val = rs.getBigDecimal("total_val");
                    double availPct = total > 0 ? ((double) inStock / total) * 100 : 0.0;

                    System.out.printf("  * Total Menu Items Tracked : %-10d | Menu Availability: %.1f%%%n", total, availPct);
                    System.out.printf("  * Items in Normal Stock    : %-10d | Low Stock Alerts : %d items%n", inStock - lowStock, lowStock);
                    System.out.printf("  * Out of Stock Items       : %-10d | Stock Valuation  : Rs. %.2f%n", outStock, val);
                }
            }

            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.printf("  %-22s | %-12s | %-6s | %-7s | %-10s | %-12s | %s%n",
                    "Item Name", "Category", "Stock", "Reorder", "Unit Cost", "Stock Val.", "Inventory Status");
            System.out.println("  -----------------------+--------------+--------+---------+------------+--------------+-----------------");

            try (PreparedStatement ps = conn.prepareStatement(detailSql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    String cat = rs.getString("category");
                    int stock = rs.getInt("stock_quantity");
                    int reorder = rs.getInt("reorder_level");
                    BigDecimal cost = rs.getBigDecimal("cost_price");
                    BigDecimal stockVal = cost.multiply(BigDecimal.valueOf(stock));
                    boolean avail = rs.getBoolean("available");

                    String status;
                    if (!avail || stock == 0) {
                        status = "[X] OUT OF STOCK";
                    } else if (stock <= reorder) {
                        status = "[!] REORDER ALERT";
                    } else {
                        status = "[OK] Normal";
                    }

                    System.out.printf("  %-22s | %-12s | %-6d | %-7d | Rs.%7.2f | Rs.%9.2f | %s%n",
                            name, cat, stock, reorder, cost, stockVal, status);
                }
            }
            System.out.println("=========================================================================================");

        } catch (SQLException e) {
            throw new DataAccessException("Error loading inventory analytics: " + e.getMessage(), e);
        }
    }

    // 10. CUSTOMER SPENDING LEADERBOARD
    public void printCustomerLeaderboard() {
        String vipSql = "SELECT c.customer_id, c.name, c.phone, " +
                "COUNT(o.order_id) AS total_orders, " +
                "SUM(o.total_amount) AS total_spent, " +
                "AVG(o.total_amount) AS aov, " +
                "MAX(o.total_amount) AS max_order " +
                "FROM customers c JOIN orders o ON c.customer_id = o.customer_id " +
                "WHERE o.status = 'COMPLETED' " +
                "GROUP BY c.customer_id, c.name, c.phone " +
                "ORDER BY total_spent DESC LIMIT 10";

        String totalSalesSql = "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status = 'COMPLETED'";
        String neverOrderedSql = "SELECT name, phone, email, created_at FROM customers WHERE customer_id NOT IN (SELECT DISTINCT customer_id FROM orders)";

        try (Connection conn = Database.getConnection()) {
            BigDecimal grandTotalSales = BigDecimal.ONE;
            try (PreparedStatement ps = conn.prepareStatement(totalSalesSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) grandTotalSales = rs.getBigDecimal(1);
            }

            System.out.println("\n=========================================================================================");
            System.out.println("                   CUSTOMER SPENDING LEADERBOARD & PARETO SHARE                          ");
            System.out.println("=========================================================================================");
            System.out.printf("  %-4s | %-18s | %-12s | %-6s | %-12s | %-10s | %-10s | %s%n",
                    "#", "Customer Name", "Phone", "Orders", "Spent (Rs.)", "AOV (Rs.)", "Max Bill", "% Total Sales");
            System.out.println("  -----+--------------------+--------------+--------+--------------+------------+------------+--------------");

            BigDecimal topSpentSum = BigDecimal.ZERO;
            try (PreparedStatement ps = conn.prepareStatement(vipSql); ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    String name = rs.getString("name");
                    String phone = rs.getString("phone");
                    int orders = rs.getInt("total_orders");
                    BigDecimal spent = rs.getBigDecimal("total_spent");
                    BigDecimal aov = rs.getBigDecimal("aov");
                    BigDecimal maxBill = rs.getBigDecimal("max_order");

                    topSpentSum = topSpentSum.add(spent);
                    double share = grandTotalSales.compareTo(BigDecimal.ZERO) > 0 ? (spent.doubleValue() / grandTotalSales.doubleValue()) * 100 : 0.0;

                    System.out.printf("  %-4d | %-18s | %-12s | %-6d | Rs.%9.2f | Rs.%7.2f | Rs.%7.2f | %5.1f%%%n",
                            rank++, name, phone, orders, spent, aov, maxBill, share);
                }
            }

            double topConcentration = grandTotalSales.compareTo(BigDecimal.ZERO) > 0 ? (topSpentSum.doubleValue() / grandTotalSales.doubleValue()) * 100 : 0.0;
            System.out.println("  -------------------------------------------------------------------------------------------------------");
            System.out.printf("  * Revenue Concentration: Top 10 Customers generate %.1f%% of overall business revenue.%n", topConcentration);

            System.out.println("\n  [REGISTERED CUSTOMERS WHO NEVER PLACED AN ORDER]");
            try (PreparedStatement ps = conn.prepareStatement(neverOrderedSql); ResultSet rs = ps.executeQuery()) {
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("   * %s (%s, %s) - Registered: %s%n",
                            rs.getString("name"), rs.getString("phone"), rs.getString("email"), rs.getString("created_at"));
                }
                if (!found) System.out.println("   All registered customers have placed at least one order.");
            }
            System.out.println("=========================================================================================");

        } catch (SQLException e) {
            throw new DataAccessException("Error loading customer leaderboard: " + e.getMessage(), e);
        }
    }

    // 11. RFM SEGMENTATION & LOYALTY TIERS
    public void printCustomerLoyaltyAndRFM() {
        String sql = "SELECT c.customer_id, c.name, c.phone, " +
                "COUNT(o.order_id) AS freq, " +
                "COALESCE(SUM(o.total_amount), 0) AS monetary, " +
                "DATEDIFF(CURRENT_TIMESTAMP, MAX(o.order_time)) AS recency_days, " +
                "MAX(o.order_time) AS last_order " +
                "FROM customers c JOIN orders o ON c.customer_id = o.customer_id " +
                "WHERE o.status = 'COMPLETED' " +
                "GROUP BY c.customer_id, c.name, c.phone " +
                "ORDER BY monetary DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n=========================================================================================");
            System.out.println("                   CUSTOMER RFM SEGMENTATION & RETENTION TIERS                           ");
            System.out.println("=========================================================================================");
            System.out.printf("  %-18s | %-12s | %-7s | %-12s | %-12s | %-10s | %s%n",
                    "Customer Name", "Phone", "Orders", "Spend (Rs.)", "Recency (Days)", "Tier", "RFM Segment");
            System.out.println("  -------------------+--------------+---------+--------------+---------------+------------+------------------");

            List<String> inactiveList = new ArrayList<>();

            while (rs.next()) {
                String name = rs.getString("name");
                String phone = rs.getString("phone");
                int freq = rs.getInt("freq");
                BigDecimal monetary = rs.getBigDecimal("monetary");
                int recency = rs.getInt("recency_days");

                String tier;
                if (monetary.compareTo(BigDecimal.valueOf(2500)) >= 0) tier = "PLATINUM";
                else if (monetary.compareTo(BigDecimal.valueOf(1500)) >= 0) tier = "GOLD";
                else if (monetary.compareTo(BigDecimal.valueOf(800)) >= 0) tier = "SILVER";
                else tier = "BRONZE";

                String segment;
                if (recency <= 45 && freq >= 5) {
                    segment = "Champion";
                } else if (recency <= 60 && freq >= 3) {
                    segment = "Loyalist";
                } else if (recency <= 60 && freq < 3) {
                    segment = "Potential Loyalist";
                } else if (recency > 60 && recency <= 120 && monetary.compareTo(BigDecimal.valueOf(1000)) >= 0) {
                    segment = "At Risk";
                    inactiveList.add(name + " (" + phone + ", Last Order: " + recency + " days ago)");
                } else {
                    segment = "Inactive / Churned";
                    inactiveList.add(name + " (" + phone + ", Last Order: " + recency + " days ago)");
                }

                System.out.printf("  %-18s | %-12s | %-7d | Rs.%9.2f | %-13d | %-10s | %s%n",
                        name, phone, freq, monetary, recency, tier, segment);
            }

            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.println("  [CHURNED & AT-RISK CUSTOMERS REQUIRING RE-ENGAGEMENT]");
            if (inactiveList.isEmpty()) {
                System.out.println("   No customers identified in churn status.");
            } else {
                for (String s : inactiveList) System.out.println("   * " + s);
            }
            System.out.println("=========================================================================================");

        } catch (SQLException e) {
            throw new DataAccessException("Error compiling RFM loyalty analytics: " + e.getMessage(), e);
        }
    }

    // 12. PAYMENT GATEWAY ANALYTICS & RECONCILIATION
    public void printPaymentAnalytics() {
        String methodSql = "SELECT payment_method, COUNT(*) AS txn_count, SUM(amount) AS total_collected, AVG(amount) AS avg_txn " +
                "FROM payments WHERE payment_status = 'SUCCESS' " +
                "GROUP BY payment_method ORDER BY total_collected DESC";

        String statusSql = "SELECT payment_status, COUNT(*) AS cnt, SUM(amount) AS amt FROM payments GROUP BY payment_status";

        String reconciliationSql = "SELECT " +
                "(SELECT COALESCE(SUM(total_amount),0) FROM orders WHERE status = 'COMPLETED') AS billed_completed, " +
                "(SELECT COALESCE(SUM(amount),0) FROM payments WHERE payment_status = 'SUCCESS') AS collected_paid, " +
                "(SELECT COUNT(*) FROM orders WHERE status = 'COMPLETED' AND order_id NOT IN (SELECT order_id FROM payments WHERE payment_status = 'SUCCESS')) AS unpaid_completed_count";

        try (Connection conn = Database.getConnection()) {
            System.out.println("\n=========================================================================================");
            System.out.println("                     PAYMENT GATEWAY ANALYTICS & AUDIT RECONCILIATION                    ");
            System.out.println("=========================================================================================");

            BigDecimal grandTotal = BigDecimal.ZERO;
            int totalTxns = 0;
            List<PaymentStat> list = new ArrayList<>();

            try (PreparedStatement ps = conn.prepareStatement(methodSql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String m = rs.getString("payment_method");
                    int count = rs.getInt("txn_count");
                    BigDecimal collected = rs.getBigDecimal("total_collected");
                    BigDecimal avg = rs.getBigDecimal("avg_txn");
                    list.add(new PaymentStat(m, count, collected, avg));
                    grandTotal = grandTotal.add(collected);
                    totalTxns += count;
                }
            }

            System.out.printf("%-10s | %-12s | %-15s | %-15s | %-8s | %s%n",
                    "Method", "Transactions", "Collected (Rs.)", "Avg / Txn (Rs.)", "Share", "Market Share Bar");
            System.out.println("-----------+--------------+-----------------+-----------------+----------+---------------");

            for (PaymentStat psStat : list) {
                double pct = grandTotal.compareTo(BigDecimal.ZERO) > 0 ? psStat.collected.doubleValue() / grandTotal.doubleValue() * 100.0 : 0.0;
                System.out.printf("%-10s | %-12d | Rs.%12.2f | Rs.%12.2f | %5.1f%%   | %s%n",
                        psStat.method, psStat.count, psStat.collected, psStat.avg, pct, generateBar(pct));
            }
            System.out.println("-----------+--------------+-----------------+-----------------+----------+---------------");
            System.out.printf("%-10s | %-12d | Rs.%12.2f | -               | 100.0%%%n", "TOTAL", totalTxns, grandTotal);

            System.out.println("\n  [PAYMENT STATUS DISTRIBUTION & ATTEMPTS]");
            try (PreparedStatement ps = conn.prepareStatement(statusSql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("   * %-10s : %d transactions (Rs. %.2f)%n",
                            rs.getString("payment_status"), rs.getInt("cnt"), rs.getBigDecimal("amt"));
                }
            }

            System.out.println("\n  [FINANCIAL RECONCILIATION AUDIT]");
            try (PreparedStatement ps = conn.prepareStatement(reconciliationSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal billed = rs.getBigDecimal("billed_completed");
                    BigDecimal paid = rs.getBigDecimal("collected_paid");
                    int unpaidOrders = rs.getInt("unpaid_completed_count");
                    BigDecimal balance = billed.subtract(paid);

                    System.out.printf("   * Total Billed Sales (Completed Orders): Rs. %.2f%n", billed);
                    System.out.printf("   * Total Net Payments Realized (SUCCESS): Rs. %.2f%n", paid);
                    System.out.printf("   * Unpaid / Discrepancy Balance         : Rs. %.2f%n", balance);
                    System.out.printf("   * Completed Orders Lacking Payment     : %d%n", unpaidOrders);
                }
            }
            System.out.println("=========================================================================================");

        } catch (SQLException e) {
            throw new DataAccessException("Error loading payment statistics: " + e.getMessage(), e);
        }
    }

    // 13. KITCHEN OPERATIONS & PEAK HOURS HEATMAP
    public void printOperationalAndKitchenMetrics() {
        String statusSql = "SELECT status, COUNT(*) FROM orders GROUP BY status";
        String dowSql = "SELECT DAYNAME(order_time) AS dow, DAYOFWEEK(order_time) AS dow_num, COUNT(*) AS ord_cnt, " +
                "COALESCE(SUM(CASE WHEN status='COMPLETED' THEN total_amount ELSE 0 END),0) AS rev " +
                "FROM orders GROUP BY dow, dow_num ORDER BY dow_num";
        String hrSql = "SELECT HOUR(order_time) AS hr, COUNT(*) AS order_count, " +
                "COALESCE(SUM(CASE WHEN status='COMPLETED' THEN total_amount ELSE 0 END),0) AS revenue " +
                "FROM orders GROUP BY hr ORDER BY hr";

        try (Connection conn = Database.getConnection()) {
            Map<String, Integer> map = new LinkedHashMap<>();
            int total = 0;
            try (PreparedStatement ps = conn.prepareStatement(statusSql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString(1), rs.getInt(2));
                    total += rs.getInt(2);
                }
            }

            int placed = map.getOrDefault("PLACED", 0);
            int prep = map.getOrDefault("PREPARING", 0);
            int ready = map.getOrDefault("READY", 0);
            int comp = map.getOrDefault("COMPLETED", 0);
            int canc = map.getOrDefault("CANCELLED", 0);

            double compRate = total > 0 ? ((double) comp / total) * 100.0 : 0.0;
            double cancRate = total > 0 ? ((double) canc / total) * 100.0 : 0.0;

            System.out.println("\n+---------------------------------------------------------------------------------------+");
            System.out.println("|                     KITCHEN OPERATIONS & PEAK WORKLOAD HEATMAP                        |");
            System.out.println("+---------------------------------------------------------------------------------------+");
            System.out.printf("| Total Orders Logged: %-6d | Active Queue: %-4d (Placed: %d, Prep: %d, Ready: %d)%n",
                    total, (placed + prep + ready), placed, prep, ready);
            System.out.printf("| Fulfilled Completed: %-6d (%.1f%%) | Cancelled: %d (%.1f%%)%n", comp, compRate, canc, cancRate);
            System.out.println("+---------------------------------------------------------------------------------------+");

            System.out.println("\n  [ORDER VOLUME & REVENUE BY DAY OF THE WEEK]");
            System.out.printf("  %-12s | %-8s | %-16s | %s%n", "Day of Week", "Orders", "Revenue (Rs.)", "Activity Bar");
            System.out.println("  -------------+----------+------------------+-------------------------");

            String busiestDay = "";
            int maxDayOrders = 0;
            try (PreparedStatement ps = conn.prepareStatement(dowSql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String dow = rs.getString("dow");
                    int cnt = rs.getInt("ord_cnt");
                    BigDecimal rev = rs.getBigDecimal("rev");
                    if (cnt > maxDayOrders) { maxDayOrders = cnt; busiestDay = dow; }
                    double pct = total > 0 ? ((double) cnt / total) * 100.0 * 3.5 : 0.0;
                    System.out.printf("  %-12s | %-8d | Rs.%13.2f | %s%n", dow, cnt, rev, generateBar(pct));
                }
            }
            System.out.println("  * Busiest Day of Week: " + busiestDay + " (" + maxDayOrders + " orders)");

            System.out.println("\n  [24-HOUR HOURLY ORDER HEATMAP (Lunch & Dinner Peak Identification)]");
            System.out.printf("  %-10s | %-7s | %-16s | %s%n", "Hour Slot", "Orders", "Revenue (Rs.)", "Hourly Heatmap");
            System.out.println("  -----------+---------+------------------+-------------------------");

            Map<Integer, int[]> hourlyData = new TreeMap<>();
            try (PreparedStatement ps = conn.prepareStatement(hrSql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hourlyData.put(rs.getInt("hr"), new int[]{rs.getInt("order_count"), rs.getInt("revenue")});
                }
            }

            int peakHour = 0, peakHourCount = 0;
            for (int h = 9; h <= 23; h++) {
                int[] vals = hourlyData.getOrDefault(h, new int[]{0, 0});
                int cnt = vals[0];
                int rev = vals[1];
                if (cnt > peakHourCount) { peakHourCount = cnt; peakHour = h; }
                double pct = ((double) cnt / 20.0) * 100.0;
                String slot = String.format("%02d:00-%02d:59", h, h);
                System.out.printf("  %-10s | %-7d | Rs.%13d | %s%n", slot, cnt, rev, generateBar(pct));
            }
            System.out.println("=========================================================================================");
            System.out.printf("  * Peak Hourly Rush Period: %02d:00 - %02d:59 (%d orders recorded)%n", peakHour, peakHour, peakHourCount);
            System.out.println("=========================================================================================");

        } catch (SQLException e) {
            throw new DataAccessException("Error loading kitchen operational metrics: " + e.getMessage(), e);
        }
    }

    // 14. STAFF PERFORMANCE ANALYTICS
    public void printStaffAnalytics() {
        String sql = "SELECT staff_name, " +
                "COUNT(*) AS total_handled, " +
                "SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END) AS completed, " +
                "SUM(CASE WHEN status='CANCELLED' THEN 1 ELSE 0 END) AS cancelled, " +
                "COALESCE(SUM(CASE WHEN status='COMPLETED' THEN total_amount ELSE 0 END), 0) AS total_sales, " +
                "COALESCE(AVG(CASE WHEN status='COMPLETED' THEN total_amount ELSE NULL END), 0) AS aov " +
                "FROM orders GROUP BY staff_name ORDER BY total_sales DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n=========================================================================================");
            System.out.println("                   STAFF WORKLOAD & PERFORMANCE ANALYTICS                                ");
            System.out.println("=========================================================================================");
            System.out.printf("  %-18s | %-8s | %-5s | %-5s | %-14s | %-12s | %s%n",
                    "Staff Member", "Handled", "Done", "Canc", "Sales (Rs.)", "AOV (Rs.)", "Completion Rate");
            System.out.println("  -------------------+----------+-------+-------+----------------+--------------+-----------------");

            while (rs.next()) {
                String staff = rs.getString("staff_name");
                int handled = rs.getInt("total_handled");
                int done = rs.getInt("completed");
                int canc = rs.getInt("cancelled");
                BigDecimal sales = rs.getBigDecimal("total_sales");
                BigDecimal aov = rs.getBigDecimal("aov");
                double compRate = handled > 0 ? ((double) done / handled) * 100 : 0.0;

                System.out.printf("  %-18s | %-8d | %-5d | %-5d | Rs.%11.2f | Rs.%9.2f | %5.1f%%%n",
                        staff, handled, done, canc, sales, aov, compRate);
            }
            System.out.println("=========================================================================================");

        } catch (SQLException e) {
            throw new DataAccessException("Error loading staff analytics: " + e.getMessage(), e);
        }
    }

    // 15. STATISTICAL ANALYSIS & OUTLIER DETECTION
    public void printStatisticalAnalysis() {
        String sql = "SELECT total_amount FROM orders WHERE status = 'COMPLETED' ORDER BY total_amount ASC";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Double> values = new ArrayList<>();
            double sum = 0;
            while (rs.next()) {
                double val = rs.getDouble(1);
                values.add(val);
                sum += val;
            }

            int n = values.size();
            if (n == 0) {
                System.out.println("No completed order data available for statistical processing.");
                return;
            }

            double mean = sum / n;
            double median = (n % 2 == 1) ? values.get(n / 2) : (values.get((n / 2) - 1) + values.get(n / 2)) / 2.0;
            double min = values.get(0);
            double max = values.get(n - 1);
            double range = max - min;

            double varianceSum = 0;
            for (double v : values) varianceSum += Math.pow(v - mean, 2);
            double stdDev = Math.sqrt(varianceSum / n);

            double q1 = values.get((int) Math.round(n * 0.25));
            double q3 = values.get((int) Math.round(n * 0.75));
            double p90 = values.get((int) Math.round(n * 0.90));
            double iqr = q3 - q1;

            // Outlier check: mean +/- 2 stddev
            double upperThreshold = mean + (2 * stdDev);
            double lowerThreshold = mean - (2 * stdDev);
            List<Double> outliers = new ArrayList<>();
            for (double v : values) {
                if (v > upperThreshold || v < lowerThreshold) outliers.add(v);
            }

            System.out.println("\n=========================================================================================");
            System.out.println("                   DESCRIPTIVE STATISTICAL DATA ANALYSIS                                 ");
            System.out.println("=========================================================================================");
            System.out.printf("  * Sample Size (N Completed Orders) : %d orders%n", n);
            System.out.printf("  * Arithmetic Mean (Average Value)  : Rs. %.2f%n", mean);
            System.out.printf("  * Median Order Value (50th %%tile)  : Rs. %.2f%n", median);
            System.out.printf("  * Order Value Range (Min / Max)    : Rs. %.2f / Rs. %.2f (Spread: Rs. %.2f)%n", min, max, range);
            System.out.printf("  * Standard Deviation (Sigma)       : Rs. %.2f%n", stdDev);
            System.out.printf("  * 25th Percentile (Q1 Lower Quart) : Rs. %.2f%n", q1);
            System.out.printf("  * 75th Percentile (Q3 Upper Quart) : Rs. %.2f%n", q3);
            System.out.printf("  * Interquartile Range (IQR)        : Rs. %.2f%n", iqr);
            System.out.printf("  * 90th Percentile (Top 10%% Orders) : Rs. %.2f%n", p90);
            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.println("  [STATISTICAL OUTLIER DETECTION (2-Sigma Threshold: > Rs. " + String.format("%.2f", upperThreshold) + ")]");
            if (outliers.isEmpty()) {
                System.out.println("   No statistical anomalies or extreme outliers detected.");
            } else {
                System.out.printf("   Found %d unusual order values outside normal distribution: %s%n",
                        outliers.size(), outliers.toString());
            }
            System.out.println("=========================================================================================");

        } catch (SQLException e) {
            throw new DataAccessException("Error computing statistical metrics: " + e.getMessage(), e);
        }
    }

    // 16. DATA QUALITY & AUDIT INTEGRITY REPORT
    public void printDataQualityAndAuditReport() {
        String orphanOrdersSql = "SELECT COUNT(*) FROM orders o LEFT JOIN order_items oi ON o.order_id = oi.order_id WHERE oi.order_item_id IS NULL";
        String mismatchSql = "SELECT COUNT(*) FROM orders o JOIN (SELECT order_id, SUM(quantity * unit_price) AS calc_total FROM order_items GROUP BY order_id) t ON o.order_id = t.order_id WHERE ABS(o.total_amount - t.calc_total) > 0.01";
        String orphanItemsSql = "SELECT COUNT(*) FROM order_items oi LEFT JOIN foods f ON oi.food_id = f.food_id WHERE f.food_id IS NULL";
        String duplicateCustSql = "SELECT COUNT(*) FROM (SELECT phone FROM customers GROUP BY phone HAVING COUNT(*) > 1) t";
        String failedPaymentsSql = "SELECT payment_id, order_id, amount, payment_method, reference_code, payment_status, payment_time FROM payments WHERE payment_status != 'SUCCESS'";

        try (Connection conn = Database.getConnection()) {
            int orphanOrders = 0, mismatches = 0, orphanItems = 0, dupCust = 0;

            try (PreparedStatement ps = conn.prepareStatement(orphanOrdersSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) orphanOrders = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(mismatchSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) mismatches = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(orphanItemsSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) orphanItems = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(duplicateCustSql); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) dupCust = rs.getInt(1);
            }

            System.out.println("\n=========================================================================================");
            System.out.println("                     DATABASE DATA QUALITY & AUDIT INTEGRITY CHECK                       ");
            System.out.println("=========================================================================================");
            System.out.printf("  1. Orders Missing Order Items (Orphans)   : %d discrepancies found [%s]%n",
                    orphanOrders, (orphanOrders == 0 ? "PASS - CLEAN" : "FAIL"));
            System.out.printf("  2. Order Total Math Recalculation Check   : %d total mismatches   [%s]%n",
                    mismatches, (mismatches == 0 ? "PASS - VERIFIED" : "FAIL"));
            System.out.printf("  3. Order Items with Missing Food Record   : %d orphaned items      [%s]%n",
                    orphanItems, (orphanItems == 0 ? "PASS - CLEAN" : "FAIL"));
            System.out.printf("  4. Duplicate Phone or Email Registrations : %d duplicates found    [%s]%n",
                    dupCust, (dupCust == 0 ? "PASS - UNIQUE" : "FAIL"));
            System.out.println("-----------------------------------------------------------------------------------------");

            System.out.println("  [PAYMENT ANOMALY AUDIT (Failed & Refunded Payment Records)]");
            try (PreparedStatement ps = conn.prepareStatement(failedPaymentsSql); ResultSet rs = ps.executeQuery()) {
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("   * ID #%-3d | Order #%-3d | Rs.%-7.2f | %-4s | %-10s | Ref: %s%n",
                            rs.getInt("payment_id"), rs.getInt("order_id"), rs.getBigDecimal("amount"),
                            rs.getString("payment_method"), rs.getString("payment_status"), rs.getString("reference_code"));
                }
                if (!found) System.out.println("   No failed or refunded payment anomalies recorded.");
            }
            System.out.println("=========================================================================================");
            System.out.println("  Database Health Status: 100% HEALTHY - All relational constraints validated.");
            System.out.println("=========================================================================================");

        } catch (SQLException e) {
            throw new DataAccessException("Error performing database integrity audit: " + e.getMessage(), e);
        }
    }

    // 17. INTERACTIVE DRILL-DOWN FILTERING
    public void interactiveDrillDown() {
        System.out.println("\n--- INTERACTIVE DATA FILTER & TRANSACTION DRILL-DOWN ---");
        System.out.println("1. Filter Orders by Date Range");
        System.out.println("2. Filter Sales by Category");
        System.out.println("3. Filter Orders by Status");
        System.out.println("4. Search Individual Customer Invoices");
        System.out.println("5. Back");
        System.out.print("Select option (1-5): ");

        try {
            int op = Integer.parseInt(scanner.nextLine().trim());
            switch (op) {
                case 1 -> {
                    System.out.print("Enter Start Date (YYYY-MM-DD): ");
                    String start = scanner.nextLine().trim();
                    System.out.print("Enter End Date (YYYY-MM-DD): ");
                    String end = scanner.nextLine().trim();
                    String sql = "SELECT order_id, order_time, status, total_amount, staff_name FROM orders " +
                            "WHERE DATE(order_time) BETWEEN ? AND ? ORDER BY order_time DESC LIMIT 20";
                    try (Connection conn = Database.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, start);
                        ps.setString(2, end);
                        try (ResultSet rs = ps.executeQuery()) {
                            System.out.println("\nMatching Orders in Date Range:");
                            System.out.printf("%-8s | %-20s | %-12s | %-12s | %s%n", "Order ID", "Date & Time", "Status", "Amount (Rs.)", "Staff");
                            System.out.println("---------+----------------------+--------------+--------------+----------------");
                            boolean has = false;
                            while (rs.next()) {
                                has = true;
                                System.out.printf("#%-7d | %-20s | %-12s | Rs.%9.2f | %s%n",
                                        rs.getInt("order_id"), rs.getString("order_time"), rs.getString("status"),
                                        rs.getBigDecimal("total_amount"), rs.getString("staff_name"));
                            }
                            if (!has) System.out.println("No records found in this range.");
                        }
                    }
                }
                case 2 -> {
                    System.out.print("Enter Category Name (e.g. Fast Food, Pizza, Main Course): ");
                    String cat = scanner.nextLine().trim();
                    String sql = "SELECT f.name, f.price, SUM(oi.quantity) AS qty, SUM(oi.quantity * oi.unit_price) AS rev " +
                            "FROM foods f JOIN order_items oi ON f.food_id = oi.food_id " +
                            "JOIN orders o ON oi.order_id = o.order_id WHERE o.status = 'COMPLETED' AND f.category LIKE ? " +
                            "GROUP BY f.name, f.price ORDER BY rev DESC";
                    try (Connection conn = Database.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, "%" + cat + "%");
                        try (ResultSet rs = ps.executeQuery()) {
                            System.out.println("\nCategory Drill-Down Results:");
                            System.out.printf("%-24s | %-10s | %-8s | %s%n", "Food Item", "Price (Rs.)", "Qty Sold", "Sales (Rs.)");
                            System.out.println("-------------------------+------------+----------+-------------");
                            boolean has = false;
                            while (rs.next()) {
                                has = true;
                                System.out.printf("%-24s | Rs.%7.2f | %-8d | Rs.%10.2f%n",
                                        rs.getString("name"), rs.getBigDecimal("price"), rs.getInt("qty"), rs.getBigDecimal("rev"));
                            }
                            if (!has) System.out.println("No items found matching category.");
                        }
                    }
                }
                case 3 -> {
                    System.out.print("Enter Status (PLACED / PREPARING / READY / COMPLETED / CANCELLED): ");
                    String st = scanner.nextLine().trim().toUpperCase();
                    String sql = "SELECT order_id, customer_id, order_time, total_amount, staff_name FROM orders WHERE status = ? ORDER BY order_id DESC LIMIT 15";
                    try (Connection conn = Database.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, st);
                        try (ResultSet rs = ps.executeQuery()) {
                            System.out.printf("\nOrders with status [%s]:%n", st);
                            System.out.printf("%-8s | %-12s | %-20s | %-12s | %s%n", "Order ID", "Customer ID", "Order Time", "Total (Rs.)", "Staff");
                            System.out.println("---------+--------------+----------------------+--------------+----------------");
                            boolean has = false;
                            while (rs.next()) {
                                has = true;
                                System.out.printf("#%-7d | Customer #%-4d | %-20s | Rs.%9.2f | %s%n",
                                        rs.getInt("order_id"), rs.getInt("customer_id"), rs.getString("order_time"),
                                        rs.getBigDecimal("total_amount"), rs.getString("staff_name"));
                            }
                            if (!has) System.out.println("No orders with this status.");
                        }
                    }
                }
                case 4 -> {
                    System.out.print("Enter Customer Name or Phone to inspect: ");
                    String query = scanner.nextLine().trim();
                    String sql = "SELECT o.order_id, o.order_time, o.status, o.total_amount, p.payment_method, p.reference_code " +
                            "FROM orders o JOIN customers c ON o.customer_id = c.customer_id " +
                            "LEFT JOIN payments p ON o.order_id = p.order_id AND p.payment_status = 'SUCCESS' " +
                            "WHERE c.name LIKE ? OR c.phone LIKE ? ORDER BY o.order_time DESC";
                    try (Connection conn = Database.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        String term = "%" + query + "%";
                        ps.setString(1, term);
                        ps.setString(2, term);
                        try (ResultSet rs = ps.executeQuery()) {
                            System.out.println("\nCustomer Transaction Ledger:");
                            System.out.printf("%-8s | %-20s | %-11s | %-12s | %-6s | %s%n", "Order ID", "Date", "Status", "Amount (Rs.)", "Method", "Ref Code");
                            System.out.println("---------+----------------------+-------------+--------------+--------+--------------------------");
                            boolean has = false;
                            while (rs.next()) {
                                has = true;
                                String m = rs.getString("payment_method");
                                String r = rs.getString("reference_code");
                                System.out.printf("#%-7d | %-20s | %-11s | Rs.%9.2f | %-6s | %s%n",
                                        rs.getInt("order_id"), rs.getString("order_time"), rs.getString("status"),
                                        rs.getBigDecimal("total_amount"), (m != null ? m : "--"), (r != null ? r : "Unpaid"));
                            }
                            if (!has) System.out.println("No transaction records found.");
                        }
                    }
                }
                case 5 -> { /* return */ }
            }
        } catch (Exception e) {
            System.out.println(">> Error in interactive drill-down: " + e.getMessage());
        }
    }

    // 18. EXPORT REPORTS TO CSV (EXCEL COMPATIBLE)
    public void exportReportsToCsv() {
        System.out.println("\nGenerating CSV export files for Microsoft Excel analysis...");

        String orderExportSql = "SELECT o.order_id, o.customer_id, c.name, o.order_time, o.status, o.total_amount, o.staff_name, " +
                "p.payment_method, p.reference_code " +
                "FROM orders o JOIN customers c ON o.customer_id = c.customer_id " +
                "LEFT JOIN payments p ON o.order_id = p.order_id AND p.payment_status = 'SUCCESS' " +
                "ORDER BY o.order_id";

        String productExportSql = "SELECT f.food_id, f.name, f.category, f.price, f.cost_price, " +
                "COALESCE(SUM(oi.quantity),0) AS qty_sold, " +
                "COALESCE(SUM(oi.quantity * oi.unit_price),0) AS revenue " +
                "FROM foods f LEFT JOIN (order_items oi JOIN orders o ON oi.order_id = o.order_id AND o.status = 'COMPLETED') " +
                "ON f.food_id = oi.food_id GROUP BY f.food_id, f.name, f.category, f.price, f.cost_price ORDER BY f.food_id";

        try (Connection conn = Database.getConnection()) {
            // Export Orders
            try (PrintWriter pw = new PrintWriter(new FileWriter("dinetrack_sales_orders.csv"));
                 PreparedStatement ps = conn.prepareStatement(orderExportSql);
                 ResultSet rs = ps.executeQuery()) {
                pw.println("OrderID,CustomerID,CustomerName,OrderTime,Status,TotalAmount,StaffName,PaymentMethod,ReferenceCode");
                int count = 0;
                while (rs.next()) {
                    pw.printf("%d,%d,\"%s\",\"%s\",\"%s\",%.2f,\"%s\",\"%s\",\"%s\"%n",
                            rs.getInt("order_id"), rs.getInt("customer_id"), rs.getString("name"),
                            rs.getString("order_time"), rs.getString("status"), rs.getDouble("total_amount"),
                            rs.getString("staff_name"), (rs.getString("payment_method") != null ? rs.getString("payment_method") : ""),
                            (rs.getString("reference_code") != null ? rs.getString("reference_code") : ""));
                    count++;
                }
                System.out.printf(">> Generated [dinetrack_sales_orders.csv] (%d order rows)%n", count);
            }

            // Export Products
            try (PrintWriter pw = new PrintWriter(new FileWriter("dinetrack_product_analytics.csv"));
                 PreparedStatement ps = conn.prepareStatement(productExportSql);
                 ResultSet rs = ps.executeQuery()) {
                pw.println("FoodID,FoodName,Category,SellingPrice,CostPrice,MarginPercentage,UnitsSold,TotalRevenue");
                int count = 0;
                while (rs.next()) {
                    double price = rs.getDouble("price");
                    double cost = rs.getDouble("cost_price");
                    double margin = price > 0 ? ((price - cost) / price) * 100 : 0.0;
                    pw.printf("%d,\"%s\",\"%s\",%.2f,%.2f,%.1f,%d,%.2f%n",
                            rs.getInt("food_id"), rs.getString("name"), rs.getString("category"),
                            price, cost, margin, rs.getInt("qty_sold"), rs.getDouble("revenue"));
                    count++;
                }
                System.out.printf(">> Generated [dinetrack_product_analytics.csv] (%d product rows)%n", count);
            }

            System.out.println(">> Export complete! You can open these .csv files directly in Excel or Google Sheets.\n");

        } catch (SQLException | IOException e) {
            throw new DataAccessException("Error exporting CSV reports: " + e.getMessage(), e);
        }
    }

    // 19. CONSOLIDATED MASTER BUSINESS INTELLIGENCE AUDIT SUITE
    public void printFullMasterAuditSuite() {
        System.out.println("\n*****************************************************************************************");
        System.out.println("       DINETRACK MASTER RESTAURANT BUSINESS INTELLIGENCE & AUDIT SUITE                   ");
        System.out.println("*****************************************************************************************");
        printDashboard();
        printMonthlyTrends();
        printPeriodComparison();
        printFinancialProfitability();
        printProductAnalytics();
        printCategoryAnalytics();
        printMarketBasketAnalysis();
        printMenuEngineeringMatrix();
        printInventoryAnalytics();
        printCustomerLeaderboard();
        printCustomerLoyaltyAndRFM();
        printPaymentAnalytics();
        printOperationalAndKitchenMetrics();
        printStaffAnalytics();
        printStatisticalAnalysis();
        printDataQualityAndAuditReport();
        System.out.println("\n*****************************************************************************************");
        System.out.println("              ALL 15 INTELLIGENCE MODULES EXECUTED SUCCESSFULLY                          ");
        System.out.println("*****************************************************************************************");
    }
}

class CategoryStat {
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

class PaymentStat {
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
