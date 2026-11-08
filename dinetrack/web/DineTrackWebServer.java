package dinetrack.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dinetrack.dao.*;
import dinetrack.db.Database;
import dinetrack.exception.DataAccessException;
import dinetrack.exception.InvalidOrderException;
import dinetrack.model.*;
import dinetrack.payment.*;

import java.awt.Desktop;
import java.io.*;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executors;

public class DineTrackWebServer {
    public static final int DEFAULT_PORT = 8080;

    private static final CustomerDAO customerDAO = new CustomerDAO();
    private static final FoodDAO foodDAO = new FoodDAO();
    private static final OrderDAO orderDAO = new OrderDAO();
    private static final PaymentDAO paymentDAO = new PaymentDAO();
    private static final ReportDAO reportDAO = new ReportDAO();

    public static void start(int port) {
        ensureDatabaseConnection();

        HttpServer server = null;
        int activePort = port;
        try {
            server = HttpServer.create(new InetSocketAddress(activePort), 0);
        } catch (IOException e) {
            // If port 8080 is in use, try alternate ports
            for (int p = 8081; p <= 8090; p++) {
                try {
                    server = HttpServer.create(new InetSocketAddress(p), 0);
                    activePort = p;
                    break;
                } catch (IOException ignored) {}
            }
        }

        if (server == null) {
            System.err.println("[Error] Failed to bind DineTrack web server to any port in range 8080-8090.");
            return;
        }

        server.setExecutor(Executors.newCachedThreadPool());

        // Contexts
        server.createContext("/", new StaticHandler());
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/customers", new CustomerHandler());
        server.createContext("/api/foods", new FoodHandler());
        server.createContext("/api/foods/toggle", new FoodToggleHandler());
        server.createContext("/api/foods/stock", new FoodStockHandler());
        server.createContext("/api/orders", new OrderHandler());
        server.createContext("/api/orders/details", new OrderDetailsHandler());
        server.createContext("/api/orders/status", new OrderStatusHandler());
        server.createContext("/api/payments", new PaymentHandler());
        server.createContext("/api/payments/refund", new PaymentRefundHandler());
        server.createContext("/api/staff", new StaffHandler());
        server.createContext("/api/reports/summary", new ReportsSummaryHandler());

        server.start();

        String appUrl = "http://localhost:" + activePort;
        System.out.println("==========================================================================");
        System.out.println("          DINETRACK RESTAURANT MANAGEMENT SYSTEM (WEB APP)               ");
        System.out.println("==========================================================================");
        System.out.println("[Ready] Web GUI is running at: " + appUrl);
        System.out.println("[Ready] Press Ctrl+C in this terminal window to stop the server.");
        System.out.println("==========================================================================\n");

        // Open browser automatically
        openBrowser(appUrl);
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
        System.out.println("[Notice] Attempting default connection to MySQL database: " + Database.getDatabaseName());
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec(new String[] { "cmd", "/c", "start", url });
                } else if (os.contains("mac")) {
                    Runtime.getRuntime().exec(new String[] { "open", url });
                } else {
                    Runtime.getRuntime().exec(new String[] { "xdg-open", url });
                }
            }
        } catch (Exception ignored) {
            // Silently ignore if headless or browser auto-launch unsupported
        }
    }

    // Static Web UI Handler
    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.equals("/index.html")) {
                byte[] bytes = WebUI.getHtmlBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                sendNotFound(exchange);
            }
        }
    }

    // Status Handler
    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String json = "{\"success\":true,\"database\":\"" + escape(Database.getDatabaseName()) + "\",\"connected\":true}";
            writeJson(exchange, 200, json);
        }
    }

    // Customer Handler
    static class CustomerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod().toUpperCase();
            try {
                if ("GET".equals(method)) {
                    List<Customer> list = customerDAO.getAll();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        Customer c = list.get(i);
                        if (i > 0) sb.append(",");
                        sb.append("{\"id\":").append(c.getId())
                          .append(",\"name\":\"").append(escape(c.getName()))
                          .append("\",\"phone\":\"").append(escape(c.getPhone() != null ? c.getPhone() : ""))
                          .append("\",\"email\":\"").append(escape(c.getEmail() != null ? c.getEmail() : ""))
                          .append("\"}");
                    }
                    sb.append("]");
                    writeJson(exchange, 200, sb.toString());
                } else if ("POST".equals(method)) {
                    String body = readBody(exchange);
                    Map<String, String> map = parseJson(body);
                    String name = map.get("name");
                    String phone = map.get("phone");
                    String email = map.get("email");
                    int newId = customerDAO.add(new Customer(0, name, phone, email));
                    writeJson(exchange, 200, "{\"success\":true,\"id\":" + newId + "}");
                } else if ("PUT".equals(method)) {
                    String body = readBody(exchange);
                    Map<String, String> map = parseJson(body);
                    int id = Integer.parseInt(map.getOrDefault("id", "0"));
                    String name = map.get("name");
                    String phone = map.get("phone");
                    String email = map.get("email");
                    boolean ok = customerDAO.update(new Customer(id, name, phone, email));
                    writeJson(exchange, 200, "{\"success\":" + ok + "}");
                } else if ("DELETE".equals(method)) {
                    Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
                    int id = Integer.parseInt(params.getOrDefault("id", "0"));
                    boolean ok;
                    try {
                        ok = customerDAO.delete(id);
                    } catch (DataAccessException dae) {
                        ok = customerDAO.deactivate(id);
                    }
                    writeJson(exchange, 200, "{\"success\":" + ok + "}");
                } else {
                    sendMethodNotAllowed(exchange);
                }
            } catch (Exception ex) {
                writeError(exchange, 400, ex.getMessage());
            }
        }
    }

    // Food Menu Handler
    static class FoodHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod().toUpperCase();
            try {
                if ("GET".equals(method)) {
                    String sql = "SELECT food_id, name, category, price, cost_price, stock_quantity, reorder_level, available FROM foods ORDER BY category, name";
                    StringBuilder sb = new StringBuilder("[");
                    try (Connection conn = Database.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql);
                         ResultSet rs = ps.executeQuery()) {
                        boolean first = true;
                        while (rs.next()) {
                            if (!first) sb.append(",");
                            first = false;
                            sb.append("{\"id\":").append(rs.getInt("food_id"))
                              .append(",\"name\":\"").append(escape(rs.getString("name")))
                              .append("\",\"category\":\"").append(escape(rs.getString("category")))
                              .append("\",\"price\":").append(rs.getBigDecimal("price"))
                              .append(",\"costPrice\":").append(rs.getBigDecimal("cost_price"))
                              .append(",\"stock\":").append(rs.getInt("stock_quantity"))
                              .append(",\"reorderLevel\":").append(rs.getInt("reorder_level"))
                              .append(",\"available\":").append(rs.getBoolean("available"))
                              .append("}");
                        }
                    }
                    sb.append("]");
                    writeJson(exchange, 200, sb.toString());
                } else if ("POST".equals(method)) {
                    String body = readBody(exchange);
                    Map<String, String> map = parseJson(body);
                    String name = map.get("name");
                    String category = map.get("category");
                    BigDecimal price = new BigDecimal(map.get("price"));
                    BigDecimal cost = new BigDecimal(map.getOrDefault("cost", "0"));
                    int stock = Integer.parseInt(map.getOrDefault("stock", "50"));
                    int reorder = Integer.parseInt(map.getOrDefault("reorderLevel", "10"));
                    boolean available = !"false".equalsIgnoreCase(map.get("available"));

                    Food f = new Food(0, name, category, price, available);
                    int newId = foodDAO.add(f, cost, stock, reorder);
                    writeJson(exchange, 200, "{\"success\":true,\"id\":" + newId + "}");
                } else if ("PUT".equals(method)) {
                    String body = readBody(exchange);
                    Map<String, String> map = parseJson(body);
                    int id = Integer.parseInt(map.get("id"));
                    String name = map.get("name");
                    String category = map.get("category");
                    BigDecimal price = new BigDecimal(map.get("price"));
                    boolean available = !"false".equalsIgnoreCase(map.get("available"));

                    Food f = new Food(id, name, category, price, available);
                    boolean ok = foodDAO.update(f);
                    if (map.containsKey("cost") || map.containsKey("reorderLevel")) {
                        BigDecimal cost = new BigDecimal(map.getOrDefault("cost", "0"));
                        int reorder = Integer.parseInt(map.getOrDefault("reorderLevel", "10"));
                        foodDAO.updateInventorySettings(id, cost, reorder);
                    }
                    writeJson(exchange, 200, "{\"success\":" + ok + "}");
                } else if ("DELETE".equals(method)) {
                    Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
                    int id = Integer.parseInt(params.getOrDefault("id", "0"));
                    boolean ok = foodDAO.delete(id);
                    writeJson(exchange, 200, "{\"success\":" + ok + "}");
                } else {
                    sendMethodNotAllowed(exchange);
                }
            } catch (Exception ex) {
                writeError(exchange, 400, ex.getMessage());
            }
        }
    }

    // Food Toggle Availability Handler
    static class FoodToggleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
                int id = Integer.parseInt(params.getOrDefault("id", "0"));
                boolean ok = foodDAO.toggleAvailability(id);
                writeJson(exchange, 200, "{\"success\":" + ok + "}");
            } catch (Exception ex) {
                writeError(exchange, 400, ex.getMessage());
            }
        }
    }

    // Food Stock Handler
    static class FoodStockHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String body = readBody(exchange);
                Map<String, String> map = parseJson(body);
                int foodId = Integer.parseInt(map.get("foodId"));
                int qty = Integer.parseInt(map.get("quantity"));
                boolean ok = foodDAO.receiveStock(foodId, qty);
                writeJson(exchange, 200, "{\"success\":" + ok + "}");
            } catch (Exception ex) {
                writeError(exchange, 400, ex.getMessage());
            }
        }
    }

    // Orders Handler
    static class OrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod().toUpperCase();
            try {
                if ("GET".equals(method)) {
                    List<Order> orders = orderDAO.getAllOrders();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < orders.size(); i++) {
                        Order o = orders.get(i);
                        if (i > 0) sb.append(",");
                        Payment p = paymentDAO.getPaymentByOrderId(o.getOrderId());
                        boolean isPaid = p != null && "SUCCESS".equals(p.getPaymentStatus());
                        String payMethod = p != null ? p.getPaymentMethod() : "";

                        sb.append("{\"orderId\":").append(o.getOrderId())
                          .append(",\"customerId\":").append(o.getCustomerId())
                          .append(",\"customerName\":\"").append(escape(o.getCustomerName()))
                          .append("\",\"orderTime\":\"").append(o.getOrderTime() != null ? o.getOrderTime().toString() : "")
                          .append("\",\"status\":\"").append(escape(o.getStatus()))
                          .append("\",\"totalAmount\":").append(o.getTotalAmount())
                          .append(",\"isPaid\":").append(isPaid)
                          .append(",\"paymentMethod\":\"").append(escape(payMethod))
                          .append("\"}");
                    }
                    sb.append("]");
                    writeJson(exchange, 200, sb.toString());
                } else if ("POST".equals(method)) {
                    String body = readBody(exchange);
                    Map<String, String> map = parseJson(body);

                    int customerId = Integer.parseInt(map.getOrDefault("customerId", "0"));
                    String orderType = map.getOrDefault("orderType", "TAKEAWAY");
                    String tableNumber = map.get("tableNumber");
                    String staffName = map.get("staffName");
                    BigDecimal discount = new BigDecimal(map.getOrDefault("discount", "0"));
                    BigDecimal taxRate = new BigDecimal(map.getOrDefault("taxRate", "5"));

                    String itemsJson = map.get("items");
                    Map<String, String> rawItems = parseJson(itemsJson);
                    Map<Integer, Integer> items = new HashMap<>();
                    for (Map.Entry<String, String> e : rawItems.entrySet()) {
                        items.put(Integer.parseInt(e.getKey()), Integer.parseInt(e.getValue()));
                    }

                    int newOrderId = orderDAO.createOrder(customerId, items, orderType, tableNumber, staffName, discount, taxRate);
                    writeJson(exchange, 200, "{\"success\":true,\"orderId\":" + newOrderId + "}");
                } else {
                    sendMethodNotAllowed(exchange);
                }
            } catch (InvalidOrderException | DataAccessException e) {
                writeError(exchange, 400, e.getMessage());
            } catch (Exception ex) {
                writeError(exchange, 500, ex.getMessage());
            }
        }
    }

    // Order Details Handler
    static class OrderDetailsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
                int orderId = Integer.parseInt(params.getOrDefault("id", "0"));
                Order order = orderDAO.getOrderDetails(orderId);
                if (order == null) {
                    writeError(exchange, 404, "Order not found");
                    return;
                }

                Map<String, Object> billing = orderDAO.getBillingSummary(orderId);
                Customer customer = order.getCustomerId() > 0 ? customerDAO.getById(order.getCustomerId()) : null;
                Payment payment = paymentDAO.getPaymentByOrderId(orderId);

                StringBuilder sb = new StringBuilder("{");
                sb.append("\"success\":true");

                // Order details
                sb.append(",\"order\":{")
                  .append("\"orderId\":").append(order.getOrderId())
                  .append(",\"customerId\":").append(order.getCustomerId())
                  .append(",\"customerName\":\"").append(escape(order.getCustomerName()))
                  .append("\",\"orderTime\":\"").append(order.getOrderTime() != null ? order.getOrderTime().toString() : "")
                  .append("\",\"status\":\"").append(escape(order.getStatus()))
                  .append("\",\"totalAmount\":").append(order.getTotalAmount())
                  .append(",\"items\":[");
                List<OrderItem> items = order.getItems();
                for (int i = 0; i < items.size(); i++) {
                    OrderItem it = items.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{\"foodId\":").append(it.getFoodId())
                      .append(",\"foodName\":\"").append(escape(it.getFoodName()))
                      .append("\",\"quantity\":").append(it.getQuantity())
                      .append(",\"unitPrice\":").append(it.getUnitPrice())
                      .append(",\"subtotal\":").append(it.getSubtotal())
                      .append("}");
                }
                sb.append("]}");

                // Billing info
                sb.append(",\"billing\":{")
                  .append("\"order_type\":\"").append(escape(String.valueOf(billing.getOrDefault("order_type", "TAKEAWAY")))).append("\"")
                  .append(",\"table_number\":\"").append(escape(String.valueOf(billing.getOrDefault("table_number", "N/A")))).append("\"")
                  .append(",\"staff_name\":\"").append(escape(String.valueOf(billing.getOrDefault("staff_name", "Staff")))).append("\"")
                  .append(",\"subtotal\":").append(billing.getOrDefault("subtotal", order.getTotalAmount()))
                  .append(",\"discount_amount\":").append(billing.getOrDefault("discount_amount", 0))
                  .append(",\"tax_rate\":").append(billing.getOrDefault("tax_rate", 0))
                  .append(",\"tax_amount\":").append(billing.getOrDefault("tax_amount", 0))
                  .append("}");

                // Customer info
                if (customer != null) {
                    sb.append(",\"customer\":{")
                      .append("\"id\":").append(customer.getId())
                      .append(",\"name\":\"").append(escape(customer.getName()))
                      .append("\",\"phone\":\"").append(escape(customer.getPhone()))
                      .append("\",\"email\":\"").append(escape(customer.getEmail()))
                      .append("\"}");
                } else {
                    sb.append(",\"customer\":null");
                }

                // Payment info
                if (payment != null) {
                    sb.append(",\"payment\":{")
                      .append("\"paymentId\":").append(payment.getPaymentId())
                      .append(",\"amount\":").append(payment.getAmount())
                      .append(",\"paymentMethod\":\"").append(escape(payment.getPaymentMethod()))
                      .append("\",\"referenceCode\":\"").append(escape(payment.getReferenceCode()))
                      .append("\",\"paymentStatus\":\"").append(escape(payment.getPaymentStatus()))
                      .append("\",\"paymentTime\":\"").append(payment.getPaymentTime() != null ? payment.getPaymentTime().toString() : "")
                      .append("\"}");
                } else {
                    sb.append(",\"payment\":null");
                }

                sb.append("}");
                writeJson(exchange, 200, sb.toString());
            } catch (Exception ex) {
                writeError(exchange, 400, ex.getMessage());
            }
        }
    }

    // Order Status Handler
    static class OrderStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String body = readBody(exchange);
                Map<String, String> map = parseJson(body);
                int orderId = Integer.parseInt(map.get("orderId"));
                String status = map.get("status");
                boolean ok = orderDAO.updateStatus(orderId, status);
                writeJson(exchange, 200, "{\"success\":" + ok + "}");
            } catch (Exception ex) {
                writeError(exchange, 400, ex.getMessage());
            }
        }
    }

    // Payments Handler
    static class PaymentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod().toUpperCase();
            try {
                if ("GET".equals(method)) {
                    List<Payment> list = paymentDAO.getAllPayments();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        Payment p = list.get(i);
                        if (i > 0) sb.append(",");
                        sb.append("{\"paymentId\":").append(p.getPaymentId())
                          .append(",\"orderId\":").append(p.getOrderId())
                          .append(",\"amount\":").append(p.getAmount())
                          .append(",\"paymentMethod\":\"").append(escape(p.getPaymentMethod()))
                          .append("\",\"referenceCode\":\"").append(escape(p.getReferenceCode()))
                          .append("\",\"paymentStatus\":\"").append(escape(p.getPaymentStatus()))
                          .append("\",\"paymentTime\":\"").append(p.getPaymentTime() != null ? p.getPaymentTime().toString() : "")
                          .append("\"}");
                    }
                    sb.append("]");
                    writeJson(exchange, 200, sb.toString());
                } else if ("POST".equals(method)) {
                    String body = readBody(exchange);
                    Map<String, String> map = parseJson(body);
                    int orderId = Integer.parseInt(map.get("orderId"));
                    String payMethod = map.getOrDefault("method", "CASH");

                    Order order = orderDAO.getOrderDetails(orderId);
                    if (order == null) {
                        writeError(exchange, 404, "Order not found");
                        return;
                    }

                    PaymentMethod pm;
                    if ("CASH".equalsIgnoreCase(payMethod)) {
                        BigDecimal tendered = new BigDecimal(map.getOrDefault("tendered", order.getTotalAmount().toString()));
                        pm = new CashPayment(order.getTotalAmount(), tendered);
                    } else if ("CARD".equalsIgnoreCase(payMethod)) {
                        String cardNum = map.getOrDefault("cardNumber", "0000");
                        String cardHolder = map.getOrDefault("cardHolder", "Customer");
                        pm = new CardPayment(cardNum, cardHolder);
                    } else {
                        String upiId = map.getOrDefault("upiId", "dinetrack@bank");
                        pm = new UpiPayment(upiId);
                    }

                    int paymentId = paymentDAO.recordPayment(orderId, order.getTotalAmount(), pm);
                    writeJson(exchange, 200, "{\"success\":true,\"paymentId\":" + paymentId + ",\"referenceCode\":\"" + escape(pm.getReferenceCode()) + "\"}");
                } else {
                    sendMethodNotAllowed(exchange);
                }
            } catch (Exception ex) {
                writeError(exchange, 400, ex.getMessage());
            }
        }
    }

    // Payment Refund Handler
    static class PaymentRefundHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String body = readBody(exchange);
                Map<String, String> map = parseJson(body);
                int paymentId = Integer.parseInt(map.get("paymentId"));
                boolean ok = paymentDAO.refundPayment(paymentId);
                writeJson(exchange, 200, "{\"success\":" + ok + "}");
            } catch (Exception ex) {
                writeError(exchange, 400, ex.getMessage());
            }
        }
    }

    // Staff Handler
    static class StaffHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod().toUpperCase();
            try {
                if ("GET".equals(method)) {
                    List<String> list = StaffDAO.activeNames();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append("\"").append(escape(list.get(i))).append("\"");
                    }
                    sb.append("]");
                    writeJson(exchange, 200, sb.toString());
                } else if ("POST".equals(method)) {
                    String body = readBody(exchange);
                    Map<String, String> map = parseJson(body);
                    String name = map.get("name");
                    StaffDAO.add(name);
                    writeJson(exchange, 200, "{\"success\":true}");
                } else if ("DELETE".equals(method)) {
                    Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
                    String name = params.get("name");
                    StaffDAO.deactivate(name);
                    writeJson(exchange, 200, "{\"success\":true}");
                } else {
                    sendMethodNotAllowed(exchange);
                }
            } catch (Exception ex) {
                writeError(exchange, 400, ex.getMessage());
            }
        }
    }

    // Reports Summary Handler
    static class ReportsSummaryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (Connection conn = Database.getConnection()) {
                BigDecimal revenue = BigDecimal.ZERO;
                int totalOrders = 0;
                int activeOrders = 0;
                int completedOrders = 0;
                int totalFoods = 0;
                int totalCustomers = 0;

                // Revenue and orders counts
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                         "SELECT " +
                         "COALESCE(SUM(CASE WHEN status='COMPLETED' THEN total_amount ELSE 0 END), 0) as revenue, " +
                         "COUNT(*) as total_orders, " +
                         "SUM(CASE WHEN status IN ('PLACED','PREPARING','READY') THEN 1 ELSE 0 END) as active_orders, " +
                         "SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END) as completed_orders " +
                         "FROM orders")) {
                    if (rs.next()) {
                        revenue = rs.getBigDecimal("revenue");
                        totalOrders = rs.getInt("total_orders");
                        activeOrders = rs.getInt("active_orders");
                        completedOrders = rs.getInt("completed_orders");
                    }
                }

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM foods WHERE available=TRUE")) {
                    if (rs.next()) totalFoods = rs.getInt(1);
                }

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers WHERE active=TRUE")) {
                    if (rs.next()) totalCustomers = rs.getInt(1);
                }

                StringBuilder sb = new StringBuilder("{");
                sb.append("\"success\":true")
                  .append(",\"revenue\":").append(revenue)
                  .append(",\"totalOrders\":").append(totalOrders)
                  .append(",\"activeOrders\":").append(activeOrders)
                  .append(",\"completedOrders\":").append(completedOrders)
                  .append(",\"totalFoods\":").append(totalFoods)
                  .append(",\"totalCustomers\":").append(totalCustomers);

                // Top Foods
                sb.append(",\"topFoods\":[");
                String topSql = "SELECT f.name, SUM(oi.quantity) as qty, SUM(oi.quantity * oi.unit_price) as rev " +
                                "FROM order_items oi JOIN foods f ON oi.food_id = f.food_id " +
                                "JOIN orders o ON oi.order_id = o.order_id " +
                                "WHERE o.status != 'CANCELLED' " +
                                "GROUP BY f.food_id, f.name ORDER BY qty DESC LIMIT 5";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(topSql)) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;
                        sb.append("{\"name\":\"").append(escape(rs.getString("name")))
                          .append("\",\"qty\":").append(rs.getInt("qty"))
                          .append(",\"revenue\":").append(rs.getBigDecimal("rev"))
                          .append("}");
                    }
                }
                sb.append("]");

                // Categories breakdown
                sb.append(",\"categories\":[");
                String catSql = "SELECT f.category, SUM(oi.quantity) as qty, SUM(oi.quantity * oi.unit_price) as rev " +
                                "FROM order_items oi JOIN foods f ON oi.food_id = f.food_id " +
                                "JOIN orders o ON oi.order_id = o.order_id " +
                                "WHERE o.status != 'CANCELLED' " +
                                "GROUP BY f.category ORDER BY rev DESC";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(catSql)) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;
                        sb.append("{\"category\":\"").append(escape(rs.getString("category")))
                          .append("\",\"itemsSold\":").append(rs.getInt("qty"))
                          .append(",\"revenue\":").append(rs.getBigDecimal("rev"))
                          .append("}");
                    }
                }
                sb.append("]");

                // Payment Methods
                sb.append(",\"paymentMethods\":[");
                String paySql = "SELECT payment_method, COUNT(*) as cnt, SUM(amount) as tot " +
                                "FROM payments WHERE payment_status='SUCCESS' " +
                                "GROUP BY payment_method";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(paySql)) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        first = false;
                        sb.append("{\"method\":\"").append(escape(rs.getString("payment_method")))
                          .append("\",\"count\":").append(rs.getInt("cnt"))
                          .append(",\"total\":").append(rs.getBigDecimal("tot"))
                          .append("}");
                    }
                }
                sb.append("]");

                sb.append("}");
                writeJson(exchange, 200, sb.toString());
            } catch (Exception ex) {
                writeError(exchange, 500, ex.getMessage());
            }
        }
    }

    // JSON and HTTP Utilities
    private static void writeJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void writeError(HttpExchange exchange, int status, String errorMsg) throws IOException {
        String json = "{\"success\":false,\"error\":\"" + escape(errorMsg != null ? errorMsg : "Unknown error") + "\"}";
        writeJson(exchange, status, json);
    }

    private static void sendNotFound(HttpExchange exchange) throws IOException {
        String msg = "404 Not Found";
        exchange.sendResponseHeaders(404, msg.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(msg.getBytes());
        }
    }

    private static void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        String msg = "405 Method Not Allowed";
        exchange.sendResponseHeaders(405, msg.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(msg.getBytes());
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf("=");
            if (idx != -1) {
                try {
                    String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                    String v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                    map.put(k, v);
                } catch (Exception ignored) {}
            }
        }
        return map;
    }

    private static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
        }
        boolean inQuotes = false;
        StringBuilder cur = new StringBuilder();
        List<String> tokens = new ArrayList<>();
        int braceDepth = 0;
        int bracketDepth = 0;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
                cur.append(c);
            } else if (!inQuotes && c == '{') {
                braceDepth++;
                cur.append(c);
            } else if (!inQuotes && c == '}') {
                braceDepth--;
                cur.append(c);
            } else if (!inQuotes && c == '[') {
                bracketDepth++;
                cur.append(c);
            } else if (!inQuotes && c == ']') {
                bracketDepth--;
                cur.append(c);
            } else if (!inQuotes && braceDepth == 0 && bracketDepth == 0 && c == ',') {
                tokens.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) {
            tokens.add(cur.toString().trim());
        }
        for (String token : tokens) {
            int colonIdx = -1;
            boolean q = false;
            for (int i = 0; i < token.length(); i++) {
                char ch = token.charAt(i);
                if (ch == '\"' && (i == 0 || token.charAt(i - 1) != '\\')) q = !q;
                else if (!q && ch == ':') { colonIdx = i; break; }
            }
            if (colonIdx != -1) {
                String k = token.substring(0, colonIdx).trim();
                String v = token.substring(colonIdx + 1).trim();
                if (k.startsWith("\"") && k.endsWith("\"")) k = k.substring(1, k.length() - 1);
                if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
                map.put(k, v);
            }
        }
        return map;
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < ' ') {
                        String hex = Integer.toHexString(ch);
                        sb.append("\\u");
                        for (int k = 0; k < 4 - hex.length(); k++) sb.append('0');
                        sb.append(hex);
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }
}
