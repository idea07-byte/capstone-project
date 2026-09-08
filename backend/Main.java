import db.Database;
import model.*;
import service.*;

import java.io.IOException;
import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final ProductService productService = new ProductService();
    private static final UserService userService = new UserService();
    private static final OrderService orderService = new OrderService();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("     BuyIt Multi-Vendor E-Commerce Marketplace     ");
        System.out.println("==================================================");
        
        System.out.println("Initializing database connection...");
        Database.initialize();

        System.out.println("Starting web server on port 8080...");
        try {
            WebServer.startServer();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        openBrowser("http://localhost:8080");

        boolean forceCli = false;
        for (String arg : args) {
            if ("--cli".equalsIgnoreCase(arg) || "-c".equalsIgnoreCase(arg) || "console".equalsIgnoreCase(arg)) {
                forceCli = true;
                break;
            }
        }

        // Run interactive CLI loop if requested or if interactive console is attached
        if (forceCli || System.console() != null) {
            runCliLoop();
        } else {
            System.out.println("\n[Server running] Press Enter to open the CLI interactive menu, or Ctrl+C to stop.");
            try {
                if (scanner.hasNextLine()) {
                    scanner.nextLine();
                    runCliLoop();
                } else {
                    // Running in non-interactive background
                    Thread.currentThread().join();
                }
            } catch (Exception e) {
                try { Thread.currentThread().join(); } catch (InterruptedException ignored) {}
            }
        }
    }

    private static void runCliLoop() {
        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Select an option (0-10): ");
            String input;
            try {
                if (!scanner.hasNextLine()) break;
                input = scanner.nextLine().trim();
            } catch (Exception e) {
                break;
            }

            switch (input) {
                case "1" -> showProducts();
                case "2" -> addProduct();
                case "3" -> findProduct();
                case "4" -> removeProduct();
                case "5" -> showUsers();
                case "6" -> addUser();
                case "7" -> findUser();
                case "8" -> createOrder();
                case "9" -> showOrders();
                case "10" -> findOrder();
                case "0" -> {
                    System.out.println("Exiting CLI. Web server continues running in background. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid option. Please choose 0 through 10.");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n--------------------------------------------------");
        System.out.println("              BUYIT CLI MANAGEMENT MENU            ");
        System.out.println("--------------------------------------------------");
        System.out.println(" 1. View available products");
        System.out.println(" 2. Add a product");
        System.out.println(" 3. Find a product by ID");
        System.out.println(" 4. Remove a product by ID");
        System.out.println(" 5. View users");
        System.out.println(" 6. Add a user");
        System.out.println(" 7. Find a user by ID");
        System.out.println(" 8. Create a customer order with multiple items");
        System.out.println(" 9. View order history");
        System.out.println(" 10. Find an order by ID");
        System.out.println(" 0. Exit CLI");
        System.out.println("--------------------------------------------------");
    }

    private static void showProducts() {
        List<Product> products = productService.getAllProducts();
        if (products.isEmpty()) {
            System.out.println("No products available in catalog.");
            return;
        }
        System.out.println("\n--- Available Products (showing first 25 of " + products.size() + ") ---");
        int limit = Math.min(products.size(), 25);
        for (int i = 0; i < limit; i++) {
            Product p = products.get(i);
            System.out.printf("[%d] %s | Price: ₹%.2f | Stock: %d | Vendor: %s%n",
                    p.getId(), p.getName(), p.getFinalPrice(), p.getStockQuantity(),
                    p.getVendorName() != null ? p.getVendorName() : "N/A");
        }
        if (products.size() > 25) {
            System.out.println("... and " + (products.size() - 25) + " more. View all in the web storefront.");
        }
    }

    private static void addProduct() {
        try {
            System.out.print("Vendor ID (default 1): ");
            String vInput = scanner.nextLine().trim();
            int vendorId = vInput.isEmpty() ? 1 : Integer.parseInt(vInput);

            System.out.print("Product Name: ");
            String name = scanner.nextLine().trim();
            if (name.isEmpty()) { System.out.println("Product name cannot be empty."); return; }

            System.out.print("Price (₹): ");
            double price = Double.parseDouble(scanner.nextLine().trim());

            System.out.print("Stock Quantity: ");
            int quantity = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("SKU (optional): ");
            String sku = scanner.nextLine().trim();

            Product p = new Product();
            p.setVendorId(vendorId);
            p.setName(name);
            p.setDescription("Added via CLI");
            p.setPrice(price);
            p.setDiscount(0.0);
            p.setStockQuantity(quantity);
            p.setSku(sku.isEmpty() ? "CLI-" + System.currentTimeMillis() : sku);
            p.setStatus("ACTIVE");

            int id = productService.addProduct(p);
            System.out.println("✓ Product successfully added with ID: " + id);
        } catch (NumberFormatException e) {
            System.out.println("Invalid numeric input. Please enter valid numbers for price/quantity.");
        } catch (Exception e) {
            System.out.println("Failed to add product: " + e.getMessage());
        }
    }

    private static void findProduct() {
        try {
            System.out.print("Enter Product ID: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            Product p = productService.getProductById(id);
            if (p == null) {
                System.out.println("Product #" + id + " not found.");
            } else {
                System.out.println("\n--- Product Details ---");
                System.out.println("ID:          " + p.getId());
                System.out.println("Name:        " + p.getName());
                System.out.println("Price:       ₹" + String.format("%.2f", p.getPrice()));
                System.out.println("Discount:    " + p.getDiscount() + "%");
                System.out.println("Final Price: ₹" + String.format("%.2f", p.getFinalPrice()));
                System.out.println("Stock:       " + p.getStockQuantity());
                System.out.println("Vendor:      " + (p.getVendorName() != null ? p.getVendorName() : "N/A"));
                System.out.println("Category:    " + (p.getCategoryName() != null ? p.getCategoryName() : "N/A"));
                System.out.println("Brand:       " + (p.getBrandName() != null ? p.getBrandName() : "N/A"));
                System.out.println("Rating:      " + String.format("%.1f ★ (%d reviews)", p.getAverageRating(), p.getReviewCount()));
                System.out.println("Status:      " + p.getStatus());
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format.");
        }
    }

    private static void removeProduct() {
        try {
            System.out.print("Enter Product ID to remove: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            if (productService.removeProductById(id)) {
                System.out.println("✓ Product #" + id + " removed successfully.");
            } else {
                System.out.println("Product #" + id + " not found or could not be removed.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format.");
        }
    }

    private static void showUsers() {
        List<User> users = userService.getAllUsers();
        if (users.isEmpty()) {
            System.out.println("No users found in database.");
            return;
        }
        System.out.println("\n--- System Users (" + users.size() + ") ---");
        for (User u : users) {
            System.out.printf("[%d] %-25s | %-25s | Role: %-8s | Status: %s%n",
                    u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getStatus());
        }
    }

    private static void addUser() {
        try {
            System.out.print("Role (1: Customer, 2: Vendor, 3: Admin) [default 1]: ");
            String rChoice = scanner.nextLine().trim();
            Role role = switch (rChoice) {
                case "2" -> Role.VENDOR;
                case "3" -> Role.ADMIN;
                default -> Role.CUSTOMER;
            };

            System.out.print("Full Name: ");
            String name = scanner.nextLine().trim();
            System.out.print("Email: ");
            String email = scanner.nextLine().trim();
            System.out.print("Phone: ");
            String phone = scanner.nextLine().trim();
            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                System.out.println("Name, email, and password are required.");
                return;
            }

            User user;
            if (role == Role.ADMIN) {
                user = new Admin(0, name, email, phone, password, "ACTIVE", null, null);
            } else if (role == Role.VENDOR) {
                user = new VendorUser(0, name, email, phone, password);
            } else {
                user = new Customer(0, name, email, phone, password);
            }

            userService.addUser(user);
            System.out.println("✓ User added successfully with ID: " + user.getId() + " (" + role + ")");
        } catch (Exception e) {
            System.out.println("Failed to add user: " + e.getMessage());
        }
    }

    private static void findUser() {
        try {
            System.out.print("Enter User ID: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            User user = userService.getUserById(id);
            if (user == null) {
                System.out.println("User #" + id + " not found.");
            } else {
                System.out.println("\n--- User Details ---");
                System.out.println("ID:     " + user.getId());
                System.out.println("Name:   " + user.getName());
                System.out.println("Email:  " + user.getEmail());
                System.out.println("Phone:  " + (user.getPhone() != null ? user.getPhone() : "N/A"));
                System.out.println("Role:   " + user.getRole());
                System.out.println("Status: " + user.getStatus());
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format.");
        }
    }

    private static void createOrder() {
        try {
            System.out.print("Customer User ID: ");
            int customerId = Integer.parseInt(scanner.nextLine().trim());
            User user = userService.getUserById(customerId);
            if (user == null || user.getRole() != Role.CUSTOMER) {
                System.out.println("Invalid customer. Order must be placed by an active customer.");
                return;
            }

            List<OrderItem> items = new ArrayList<>();
            while (true) {
                System.out.print("Product ID: ");
                int productId = Integer.parseInt(scanner.nextLine().trim());
                Product product = productService.getProductById(productId);
                if (product == null) {
                    System.out.println("Product #" + productId + " not found. Try again.");
                    continue;
                }

                System.out.print("Quantity: ");
                int quantity = Integer.parseInt(scanner.nextLine().trim());
                if (quantity <= 0) {
                    System.out.println("Quantity must be greater than zero.");
                    continue;
                }
                if (!productService.hasSufficientStock(productId, quantity)) {
                    System.out.println("Insufficient stock! Available: " + product.getStockQuantity());
                    continue;
                }

                OrderItem item = new OrderItem(productId, product.getName(), product.getFinalPrice(), quantity);
                item.setVendorId(product.getVendorId());
                item.setSubtotal(product.getFinalPrice() * quantity);
                items.add(item);
                System.out.printf("Added: %s (Qty: %d, Subtotal: ₹%.2f)%n", product.getName(), quantity, item.getSubtotal());

                System.out.print("Add another item? (y/n): ");
                String more = scanner.nextLine().trim().toLowerCase();
                if (!more.equals("y")) break;
            }

            if (items.isEmpty()) {
                System.out.println("No items in order. Cancelled.");
                return;
            }

            Order order = orderService.createOrder(customerId, items);
            System.out.println("\n✓ Order placed successfully!");
            System.out.println("Order ID:     #" + order.getId());
            System.out.println("Total Amount: ₹" + String.format("%.2f", order.getFinalAmount()));
            System.out.println("Status:       " + order.getOrderStatus());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
        } catch (Exception e) {
            System.out.println("Order placement failed: " + e.getMessage());
        }
    }

    private static void showOrders() {
        List<Order> orders = orderService.getAllOrders();
        if (orders.isEmpty()) {
            System.out.println("No orders placed yet.");
            return;
        }
        System.out.println("\n--- Order History (" + orders.size() + ") ---");
        for (Order o : orders) {
            System.out.printf("Order #%-4d | Customer: %-20s | Total: ₹%-8.2f | Status: %-10s | Items: %d%n",
                    o.getId(),
                    (o.getCustomerName() != null ? o.getCustomerName() : "User #" + o.getCustomerId()),
                    o.getFinalAmount(),
                    o.getOrderStatus(),
                    o.getItems() != null ? o.getItems().size() : 0);
        }
    }

    private static void findOrder() {
        try {
            System.out.print("Enter Order ID: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            Order order = orderService.getOrderById(id);
            if (order == null) {
                System.out.println("Order #" + id + " not found.");
            } else {
                System.out.println("\n--- Order #" + order.getId() + " Details ---");
                System.out.println("Customer:   " + (order.getCustomerName() != null ? order.getCustomerName() : "ID #" + order.getCustomerId()) +
                        (order.getCustomerEmail() != null ? " (" + order.getCustomerEmail() + ")" : ""));
                System.out.println("Total:      ₹" + String.format("%.2f", order.getFinalAmount()));
                System.out.println("Status:     " + order.getOrderStatus());
                System.out.println("Payment:    " + (order.getPaymentStatus() != null ? order.getPaymentStatus() : "N/A"));
                System.out.println("Placed At:  " + order.getCreatedAt());
                System.out.println("Items:");
                if (order.getItems() != null) {
                    for (OrderItem it : order.getItems()) {
                        System.out.printf("  - %s (Qty: %d, Unit Price: ₹%.2f, Subtotal: ₹%.2f)%n",
                                it.getProductName(), it.getQuantity(), it.getPrice(), it.getSubtotal());
                    }
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format.");
        }
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("Opening browser at: " + url);
            } else {
                System.out.println("Visit the web application at: " + url);
            }
        } catch (IOException | URISyntaxException e) {
            System.out.println("Visit the web application at: " + url);
        }
    }
}
