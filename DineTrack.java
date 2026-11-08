import dinetrack.db.Database;
import dinetrack.exception.DataAccessException;
import dinetrack.exception.InvalidOrderException;
import dinetrack.model.*;
import dinetrack.payment.*;
import dinetrack.dao.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class DineTrack {
    private static final Scanner scanner = new Scanner(System.in);
    private static final CustomerDAO customerDAO = new CustomerDAO();
    private static final FoodDAO foodDAO = new FoodDAO();
    private static final OrderDAO orderDAO = new OrderDAO();
    private static final PaymentDAO paymentDAO = new PaymentDAO();
    private static final ReportDAO reportDAO = new ReportDAO();

    public static void main(String[] args) {
        if (args.length > 0 && "--cli".equalsIgnoreCase(args[0])) {
            runCli();
            return;
        }
        System.out.println("Starting DineTrack Web GUI Application...");
        System.out.println("(To run in console terminal mode instead, use: java DineTrack --cli)\n");
        dinetrack.web.DineTrackWebServer.start(dinetrack.web.DineTrackWebServer.DEFAULT_PORT);
    }

    private static void runCli() {
        printBanner();
        ensureDatabaseConnection();

        boolean running = true;
        while (running) {
            try {
                printMainMenu();
                int choice = promptInt("Enter your choice (1-9): ", 1, 9);
                switch (choice) {
                    case 1 -> customerMenu();
                    case 2 -> foodMenu();
                    case 3 -> createOrderWorkflow();
                    case 4 -> viewOrdersWorkflow();
                    case 5 -> updateOrderStatusWorkflow();
                    case 6 -> paymentWorkflow();
                    case 7 -> reportsMenu();
                    case 8 -> staffMenu();
                    case 9 -> {
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
        String[] candidatePasswords = { "ROOTP2P", "root", "", "admin", "1234", "12345678", "root123", "password" };
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
        System.out.println("  1. Customer Management       (Add, View, Update, Deactivate, Search)");
        System.out.println("  2. Food Menu Management      (Add, View, Update, Stock Status)");
        System.out.println("  3. Create New Order          (Select items and place order)");
        System.out.println("  4. View Orders & Invoices    (Order history and detailed bill)");
        System.out.println("  5. Update Order Status       (Placed, Preparing, Ready, Completed)");
        System.out.println("  6. Payment Records           (Cash entry; card/UPI demo; refunds)");
        System.out.println("  7. Reports & Statistics      (Dashboard & Top selling foods)");
        System.out.println("  8. Staff Management");
        System.out.println("  9. Exit");
        System.out.println("==========================================================================");
    }

    private static void staffMenu() {
        while (true) {
            System.out.println("\n--- STAFF MANAGEMENT ---");
            System.out.println("1. List active staff");
            System.out.println("2. Add staff member");
            System.out.println("3. Deactivate staff member");
            System.out.println("4. Back");
            int choice = promptInt("Choice (1-4): ", 1, 4);
            if (choice == 1) {
                for (String name : StaffDAO.activeNames())
                    System.out.println("- " + name);
            } else if (choice == 2) {
                StaffDAO.add(promptNonEmptyString("Staff name: "));
                System.out.println(">> Staff member added.");
            } else if (choice == 3) {
                List<String> staff = StaffDAO.activeNames();
                if (staff.isEmpty()) {
                    System.out.println("No active staff members.");
                    continue;
                }
                for (int i = 0; i < staff.size(); i++)
                    System.out.println((i + 1) + ". " + staff.get(i));
                String name = staff.get(promptInt("Select staff member: ", 1, staff.size()) - 1);
                StaffDAO.deactivate(name);
                System.out.println(">> Staff member deactivated.");
            } else
                return;
        }
    }

    private static void customerMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- CUSTOMER MANAGEMENT ---");
            System.out.println("1. Add New Customer");
            System.out.println("2. View All Customers");
            System.out.println("3. Search Customer (by Name / Phone / Email)");
            System.out.println("4. Update Customer Information");
            System.out.println("5. Deactivate Customer (preserve order history)");
            System.out.println("6. Back to Main Menu");
            int choice = promptInt("Choice (1-6): ", 1, 6);

            switch (choice) {
                case 1 -> {
                    System.out.println("\n[Register New Customer]");
                    String name = promptNonEmptyString("Enter Customer Name: ");
                    String phone = promptOptionalRegexString("Enter Phone (optional): ", "^[0-9+() -]{7,15}$",
                            "Invalid phone format.");
                    String email = promptOptionalRegexString("Enter Email (optional): ",
                            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", "Invalid email format.");
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
                    if (!n.isEmpty())
                        existing.setName(n);

                    System.out.print("Enter New Phone (leave blank to keep '" + existing.getPhone() + "'): ");
                    String p = scanner.nextLine().trim();
                    if (!p.isEmpty())
                        existing.setPhone(p);

                    System.out.print("Enter New Email (leave blank to keep '" + existing.getEmail() + "'): ");
                    String e = scanner.nextLine().trim();
                    if (!e.isEmpty())
                        existing.setEmail(e);

                    if (customerDAO.update(existing)) {
                        System.out.println(">> Success: Customer details updated!");
                    } else {
                        System.out.println(">> Could not update customer.");
                    }
                }
                case 5 -> {
                    int id = promptInt("Enter Customer ID to delete: ");
                    System.out.print("Deactivate customer #" + id + "? (yes/no): ");
                    if (scanner.nextLine().trim().equalsIgnoreCase("yes")) {
                        if (customerDAO.deactivate(id)) {
                            System.out.println(">> Customer deactivated. Historical orders are preserved.");
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
        System.out
                .println("------+------------------------+------------------+----------------------------+-----------");
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
            System.out.println("5. Receive Stock or Record Waste / Correction");
            System.out.println("6. Toggle Menu Availability");
            System.out.println("7. Delete Food Item");
            System.out.println("8. Back to Main Menu");
            int choice = promptInt("Choice (1-8): ", 1, 8);

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
                    boolean avail = availStr.isEmpty() || availStr.equalsIgnoreCase("yes")
                            || availStr.equalsIgnoreCase("y");

                    BigDecimal cost = promptNonNegativeBigDecimal("Unit production cost (Rs.): ");
                    int stock = promptInt("Starting stock quantity: ", 0, 100000);
                    int reorder = promptInt("Low-stock reorder threshold: ", 0, 100000);
                    Food f = new Food(0, name, category, price, avail);
                    int id = foodDAO.add(f, cost, stock, reorder);
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
                    if (!n.isEmpty())
                        f.setName(n);

                    System.out.print("Enter New Category (leave blank to keep '" + f.getCategory() + "'): ");
                    String c = scanner.nextLine().trim();
                    if (!c.isEmpty())
                        f.setCategory(c);

                    System.out.print("Enter New Price (leave blank to keep Rs. " + f.getPrice() + "): ");
                    String pStr = scanner.nextLine().trim();
                    if (!pStr.isEmpty()) {
                        f.setPrice(new BigDecimal(pStr));
                    }

                    System.out.print(
                            "Available? (yes/no, leave blank to keep " + (f.isAvailable() ? "yes" : "no") + "): ");
                    String aStr = scanner.nextLine().trim();
                    if (!aStr.isEmpty()) {
                        f.setAvailable(aStr.equalsIgnoreCase("yes") || aStr.equalsIgnoreCase("y"));
                    }

                    BigDecimal cost = promptNonNegativeBigDecimal("Unit production cost in Rs.: ");
                    int reorder = promptInt("Reorder threshold: ", 0, 100000);
                    foodDAO.updateInventorySettings(id, cost, reorder);

                    if (foodDAO.update(f)) {
                        System.out.println(">> Success: Menu item updated!");
                    } else {
                        System.out.println(">> Failed to update menu item.");
                    }
                }
                case 5 -> {
                    int id = promptInt("Enter Food ID: ");
                    int direction = promptInt("1. Receive stock  2. Record waste / downward correction: ", 1, 2);
                    int quantity = promptInt("Quantity: ", 1, 100000);
                    boolean changed = direction == 1 ? foodDAO.receiveStock(id, quantity)
                            : foodDAO.adjustStock(id, -quantity);
                    System.out.println(changed ? ">> Stock updated."
                            : ">> Item not found or adjustment would make stock negative.");
                }
                case 6 -> {
                    int id = promptInt("Enter Food ID to toggle availability: ");
                    Food f = foodDAO.getById(id);
                    if (f == null) {
                        System.out.println(">> Food item not found with ID #" + id);
                        break;
                    }
                    if (foodDAO.toggleAvailability(id)) {
                        System.out.println(">> Success: Availability changed to: "
                                + (!f.isAvailable() ? "AVAILABLE" : "OUT OF STOCK"));
                    }
                }
                case 7 -> {
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
                case 8 -> back = true;
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
        int customerId = promptInt("Customer ID (0 for walk-in without registration): ", 0, Integer.MAX_VALUE);
        Customer customer = customerId > 0 ? customerDAO.getById(customerId) : null;
        if (customerId > 0 && customer == null) {
            System.out.println(">> Customer #" + customerId + " not found!");
            System.out.print("Would you like to quickly register this customer? (yes/no): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("yes")) {
                String name = promptNonEmptyString("Customer Name: ");
                String phone = promptOptionalRegexString("Phone (optional): ", "^[0-9+() -]{7,15}$",
                        "Invalid phone format.");
                String email = promptOptionalRegexString("Email (optional): ",
                        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", "Invalid email format.");
                customerId = customerDAO.add(new Customer(0, name, phone, email));
                customer = customerDAO.getById(customerId);
                System.out.println(
                        ">> Registered! Proceeding with Customer #" + customerId + " (" + customer.getName() + ")");
            } else {
                return;
            }
        } else if (customer != null) {
            System.out.println("Customer Selected: " + customer.getName() + " (" + customer.getPhone() + ")");
        }

        int typeChoice = promptInt("Order type: 1 Dine-in, 2 Takeaway, 3 Delivery: ", 1, 3);
        String orderType = typeChoice == 1 ? "DINE_IN" : typeChoice == 2 ? "TAKEAWAY" : "DELIVERY";
        String tableNumber = orderType.equals("DINE_IN") ? promptNonEmptyString("Table number: ") : null;
        List<String> staffOptions = StaffDAO.activeNames();
        if (staffOptions.isEmpty())
            throw new DataAccessException("No active staff records exist. Add staff to the staff table first.", null);
        System.out.println("Active staff:");
        for (int i = 0; i < staffOptions.size(); i++)
            System.out.println((i + 1) + ". " + staffOptions.get(i));
        String staffName = staffOptions.get(promptInt("Staff member: ", 1, staffOptions.size()) - 1);
        System.out.println("\nAvailable Menu Items:");
        displayFoodList(foodDAO.getAll(true));

        Map<Integer, Integer> cart = new LinkedHashMap<>();
        BigDecimal estimatedTotal = BigDecimal.ZERO;

        while (true) {
            System.out.print("\nEnter Food ID to add (or 0 to finish selection): ");
            int foodId = promptInt("");
            if (foodId == 0)
                break;

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

        BigDecimal discount = promptNonNegativeBigDecimal("Discount amount in Rs. (0 if none): ");
        if (discount.compareTo(estimatedTotal) > 0)
            discount = estimatedTotal;
        BigDecimal taxRate = promptNonNegativeBigDecimal("Tax percentage (0 if not applicable): ");
        BigDecimal tax = estimatedTotal.subtract(discount).multiply(taxRate).divide(BigDecimal.valueOf(100), 2,
                RoundingMode.HALF_UP);
        BigDecimal amountDue = estimatedTotal.subtract(discount).add(tax);
        System.out.printf("Subtotal Rs. %.2f | Discount Rs. %.2f | Tax Rs. %.2f | Total Rs. %.2f%n", estimatedTotal,
                discount, tax, amountDue);
        System.out.print("\nConfirm order placement for Rs. " + amountDue + "? (yes/no) [yes]: ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.isEmpty() && !confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) {
            System.out.println(">> Order discarded.");
            return;
        }

        int orderId = orderDAO.createOrder(customerId, cart, orderType, tableNumber, staffName, discount, taxRate);
        System.out.println("\n==========================================================================");
        System.out.println(">> ORDER PLACED SUCCESSFULLY! Order ID: #" + orderId);
        System.out.println(">> Status: PLACED | Total Amount: Rs. " + amountDue);
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
            System.out.println(
                    "\n--------------------------------------------------------------------------------------------");
            System.out.printf("%-8s | %-20s | %-19s | %-12s | %-12s | %s\n",
                    "Order ID", "Customer Name", "Order Date/Time", "Status", "Total (Rs.)", "Payment");
            System.out.println(
                    "---------+----------------------+---------------------+--------------+--------------+-------------");
            for (Order o : orders) {
                Payment p = paymentDAO.getPaymentByOrderId(o.getOrderId());
                String paymentBadge = p == null ? "PENDING"
                        : "SUCCESS".equals(p.getPaymentStatus()) ? "PAID (" + p.getPaymentMethod() + ")"
                                : p.getPaymentStatus();
                String timeStr = o.getOrderTime() != null ? o.getOrderTime().toString().substring(0, 19) : "N/A";
                System.out.printf("#%-7d | %-20s | %-19s | %-12s | Rs.%9.2f | %s\n",
                        o.getOrderId(), o.getCustomerName(), timeStr, o.getStatus(), o.getTotalAmount(), paymentBadge);
            }
            System.out.println(
                    "--------------------------------------------------------------------------------------------");
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
        System.out.printf("Customer    : %s (Phone: %s)\n", order.getCustomerName(),
                (cust != null ? cust.getPhone() : "N/A"));
        Map<String, Object> billing = orderDAO.getBillingSummary(orderId);
        System.out.printf("Order type  : %s%s | Staff: %s%n", billing.get("order_type"),
                billing.get("table_number") == null ? "" : " | Table " + billing.get("table_number"),
                billing.get("staff_name"));
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("%-4s | %-26s | %-8s | %-10s | %-12s\n", "#", "Item", "Qty", "Unit Price", "Subtotal");
        System.out.println("-----+----------------------------+----------+------------+---------------");

        int idx = 1;
        for (OrderItem item : order.getItems()) {
            System.out.printf("%-4d | %-26s | %-8d | Rs.%7.2f | Rs.%9.2f\n",
                    idx++, item.getFoodName(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal());
        }
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("Subtotal         :                                         Rs.%10.2f%n",
                billing.get("subtotal"));
        System.out.printf("Discount         :                                         Rs.%10.2f%n",
                billing.get("discount_amount"));
        System.out.printf("Tax (%.2f%%)       :                                         Rs.%10.2f%n",
                billing.get("tax_rate"), billing.get("tax_amount"));
        System.out.printf("TOTAL AMOUNT DUE :                                         Rs.%10.2f%n",
                order.getTotalAmount());
        System.out.println("--------------------------------------------------------------------------");

        if (payment != null) {
            System.out.println("PAYMENT STATUS   : " + payment.getPaymentStatus());
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

        System.out.println(
                "Order #" + orderId + " for " + order.getCustomerName() + " | Current Status: " + order.getStatus());
        String current = order.getStatus();
        if (current.equals("COMPLETED") || current.equals("CANCELLED")) {
            System.out.println(">> This order is closed and its status cannot be changed.");
            return;
        }
        String[] nextStates = current.equals("PLACED") ? new String[] { "PREPARING", "CANCELLED" }
                : current.equals("PREPARING") ? new String[] { "READY", "CANCELLED" }
                        : new String[] { "COMPLETED", "CANCELLED" };
        System.out.println("Allowed next states:");
        for (int i = 0; i < nextStates.length; i++)
            System.out.println((i + 1) + ". " + nextStates[i]);
        int stChoice = promptInt("Choice: ", 1, nextStates.length);
        String newStatus = nextStates[stChoice - 1];

        if (orderDAO.updateStatus(orderId, newStatus)) {
            System.out.println(">> Success: Status for Order #" + orderId + " changed to " + newStatus);
        } else {
            System.out.println(">> Could not update status.");
        }
    }

    private static void paymentWorkflow() {
        System.out.println("\n--- PAYMENT PROCESSING ---");
        System.out.println("1. Record payment (demo simulation for card/UPI)");
        System.out.println("2. View payment records");
        System.out.println("3. Record a full refund");
        int ch = promptInt("Choice (1-3): ", 1, 3);

        if (ch == 1) {
            int orderId = promptInt("Enter Order ID to process payment: ");
            processPaymentForOrder(orderId);
        } else if (ch == 3) {
            int paymentId = promptInt("Payment ID to refund: ");
            System.out.print("Confirm full refund for payment #" + paymentId + "? (yes/no): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("yes")) {
                if (paymentDAO.refundPayment(paymentId))
                    System.out.println(">> Refund recorded.");
                else
                    System.out.println(">> Payment not found or already refunded.");
            }
        } else {
            List<Payment> list = paymentDAO.getAllPayments();
            if (list.isEmpty()) {
                System.out.println("\nNo payment records found.");
                return;
            }
            System.out.println(
                    "\n---------------------------------------------------------------------------------------------------------");
            System.out.printf("%-6s | %-8s | %-12s | %-8s | %-18s | %-24s | %s\n",
                    "Pay ID", "Order ID", "Amount", "Method", "Status", "Reference Code", "Time");
            System.out.println(
                    "-------+----------+--------------+----------+--------------------+--------------------------+-------------------------");
            for (Payment p : list) {
                System.out.printf("%-6d | #%-7d | Rs.%9.2f | %-8s | %-18s | %-24s | %s\n",
                        p.getPaymentId(), p.getOrderId(), p.getAmount(),
                        p.getPaymentMethod(), p.getPaymentStatus(), p.getReferenceCode(), p.getPaymentTime());
            }
            System.out.println(
                    "---------------------------------------------------------------------------------------------------------");
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
        if (existing != null && "SUCCESS".equals(existing.getPaymentStatus())) {
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
                System.out.println("\n[DEMO ONLY] No card gateway is connected; no card details are collected.");
                paymentMethod = new CardPayment("0000", "SIMULATED PAYMENT");
            }
            case 3 -> {
                System.out.println("\n[DEMO ONLY] No UPI provider is connected; this will not move money.");
                paymentMethod = new UpiPayment("demo@bank");
            }
            default -> throw new IllegalStateException("Invalid choice");
        }

        paymentMethod.processPayment(bill);
        int payId = paymentDAO.recordPayment(orderId, bill, paymentMethod);

        System.out.println("\n==========================================================================");
        System.out.println(">> PAYMENT RECORDED (cash entry or simulated card/UPI; no gateway confirmation).");
        System.out.println("   Payment ID     : #" + payId);
        System.out.println("   Method         : " + paymentMethod.getMethodName());
        System.out.println("   Reference Code : " + paymentMethod.getReferenceCode());
        System.out.println("   Amount Paid    : Rs. " + bill);
        System.out.println("   Order Status   : " + order.getStatus() + " (kitchen status unchanged)");
        System.out.println("==========================================================================");
    }

    private static void reportsMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- OWNER REPORTS ---");
            System.out.println("1. Business dashboard");
            System.out.println("2. Sales for a date range");
            System.out.println("3. Best-selling menu items");
            System.out.println("4. Payment summary (simulated methods)");
            System.out.println("5. Low stock and stock on hand");
            System.out.println("6. Back");
            int ch = promptInt("Choice (1-6): ", 1, 6);
            switch (ch) {
                case 1 -> reportDAO.printDashboard();
                case 2 -> reportDAO.printSalesDateRange();
                case 3 -> reportDAO.printProductAnalytics();
                case 4 -> reportDAO.printPaymentAnalytics();
                case 5 -> reportDAO.printInventoryAnalytics();
                case 6 -> back = true;
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
            if (val >= min && val <= max)
                return val;
            System.out.printf(">> Please enter a number between %d and %d.\n", min, max);
        }
    }

    private static int promptInt(String prompt, int min, int max, int defaultVal) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (line.isEmpty())
                return defaultVal;
            try {
                int val = Integer.parseInt(line);
                if (val >= min && val <= max)
                    return val;
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

    private static BigDecimal promptNonNegativeBigDecimal(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                BigDecimal value = new BigDecimal(scanner.nextLine().trim()).setScale(2, RoundingMode.HALF_UP);
                if (value.compareTo(BigDecimal.ZERO) >= 0)
                    return value;
            } catch (NumberFormatException ignored) {
            }
            System.out.println(">> Enter a valid amount of zero or more.");
        }
    }

    private static String promptNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty())
                return input;
            System.out.println(">> Field cannot be left empty.");
        }
    }

    private static String promptRegexString(String prompt, String regex, String errorMsg) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.matches(regex))
                return input;
            System.out.println(">> " + errorMsg);
        }
    }

    private static String promptOptionalRegexString(String prompt, String regex, String errorMsg) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty() || input.matches(regex))
                return input.isEmpty() ? null : input;
            System.out.println(">> " + errorMsg);
        }
    }

}

