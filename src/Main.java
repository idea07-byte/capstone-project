import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import model.Admin;
import model.Customer;
import model.Order;
import model.OrderItem;
import model.Product;
import service.OrderService;
import service.ProductService;
import service.UserService;

public class Main {
    private static final ProductService productService = new ProductService();
    private static final UserService userService = new UserService();
    private static final OrderService orderService = new OrderService();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        seedData();
        System.out.println("=== Shop Capstone ===");

        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("1. View products");
            System.out.println("2. Add product");
            System.out.println("3. Find product by ID");
            System.out.println("4. Remove product by ID");
            System.out.println("5. View users");
            System.out.println("6. Add user");
            System.out.println("7. Find user by ID");
            System.out.println("8. Create order");
            System.out.println("9. View orders");
            System.out.println("10. Find order by ID");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");

            String input = scanner.nextLine().trim();
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
                case "0" -> running = false;
                default -> System.out.println("Please enter a valid option.");
            }
        }

        System.out.println("Goodbye.");
    }

    private static void seedData() {
        productService.addProduct(new Product(101, "Wireless Mouse", 799.0, 12));
        productService.addProduct(new Product(102, "USB-C Cable", 299.0, 25));
        productService.addProduct(new Product(103, "Mechanical Keyboard", 2499.0, 8));

        userService.addUser(new Customer(1, "Asha", "asha@example.com", "pass1234"));
        userService.addUser(new Admin(900, "Admin", "admin@example.com", "adminpass"));
    }

    private static void showProducts() {
        var products = productService.getAllProducts();
        if (products.isEmpty()) {
            System.out.println("No products available.");
            return;
        }
        System.out.println("Available products:");
        products.forEach(System.out::println);
    }

    private static void addProduct() {
        try {
            System.out.print("Product ID: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Name: ");
            String name = scanner.nextLine().trim();
            System.out.print("Price: ");
            double price = Double.parseDouble(scanner.nextLine().trim());
            System.out.print("Quantity: ");
            int quantity = Integer.parseInt(scanner.nextLine().trim());

            productService.addProduct(new Product(id, name, price, quantity));
            System.out.println("Product added.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a valid number.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void findProduct() {
        try {
            System.out.print("Product ID: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            var product = productService.getProductById(id);
            if (product == null) {
                System.out.println("Product not found.");
            } else {
                System.out.println(product);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID.");
        }
    }

    private static void removeProduct() {
        try {
            System.out.print("Product ID: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            if (productService.removeProductById(id)) {
                System.out.println("Product removed.");
            } else {
                System.out.println("Product not found.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID.");
        }
    }

    private static void addUser() {
        try {
            System.out.print("User ID: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Name: ");
            String name = scanner.nextLine().trim();
            System.out.print("Email: ");
            String email = scanner.nextLine().trim();
            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            userService.addUser(new Customer(id, name, email, password));
            System.out.println("User added.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a valid number.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void findUser() {
        try {
            System.out.print("User ID: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            var user = userService.getUserById(id);
            if (user == null) {
                System.out.println("User not found.");
            } else {
                System.out.println(user);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID.");
        }
    }

    private static void showUsers() {
        var users = userService.getAllUsers();
        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }
        System.out.println("Users:");
        users.forEach(System.out::println);
    }

    private static void createOrder() {
        try {
            System.out.print("Customer ID: ");
            int customerId = Integer.parseInt(scanner.nextLine().trim());
            var user = userService.getUserById(customerId);
            if (!(user instanceof Customer customer)) {
                System.out.println("Order must be created by an existing customer.");
                return;
            }

            List<OrderItem> items = new ArrayList<>();
            while (true) {
                System.out.print("Product ID: ");
                int productId = Integer.parseInt(scanner.nextLine().trim());
                var product = productService.getProductById(productId);
                if (product == null) {
                    System.out.println("Product not found.");
                    continue;
                }
                System.out.print("Quantity: ");
                int quantity = Integer.parseInt(scanner.nextLine().trim());
                if (quantity <= 0) {
                    System.out.println("Quantity must be greater than zero.");
                    continue;
                }
                if (!productService.hasSufficientStock(productId, quantity)) {
                    System.out.println("Not enough stock available.");
                    continue;
                }

                items.add(new OrderItem(productId, product.getName(), product.getPrice(), quantity));
                System.out.print("Add another item? (y/n): ");
                String more = scanner.nextLine().trim().toLowerCase();
                if (!more.equals("y")) {
                    break;
                }
            }

            Order order = orderService.createOrder(customer, items);
            items.forEach(item -> productService.reduceProductQuantity(item.getProductId(), item.getQuantity()));
            System.out.println("Order created: " + order);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a valid number.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void showOrders() {
        var orders = orderService.getAllOrders();
        if (orders.isEmpty()) {
            System.out.println("No orders placed yet.");
            return;
        }
        System.out.println("Orders:");
        orders.forEach(System.out::println);
    }

    private static void findOrder() {
        try {
            System.out.print("Order ID: ");
            int id = Integer.parseInt(scanner.nextLine().trim());
            var order = orderService.getOrderById(id);
            if (order == null) {
                System.out.println("Order not found.");
            } else {
                System.out.println(order);
                order.getItems().forEach(System.out::println);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID.");
        }
    }
}
