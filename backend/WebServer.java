import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.List;
import java.util.ArrayList;
import service.ProductService;
import service.OrderService;
import service.UserService;
import model.Product;
import model.Order;
import model.User;
import model.Admin;
import model.Customer;
import model.OrderItem;

public class WebServer {
    private static final int PORT = 8080;

    private static Path webRoot() {
        try {
            Path codeSource = Paths.get(WebServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return codeSource.getParent().resolve("frontend/dist").toAbsolutePath().normalize();
        } catch (Exception e) {
            return Paths.get("./frontend/dist").toAbsolutePath().normalize();
        }
    }

    public static void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Create context for static files
        server.createContext("/", new StaticFileHandler());

        // API endpoints (can be extended)
        server.createContext("/api/auth/login", new LoginHandler());
        server.createContext("/api/auth/register", new RegisterHandler());
        server.createContext("/api/products", new ProductHandler());
        server.createContext("/api/orders", new OrderHandler());
        server.createContext("/api/users", new UserHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("==================================");
        System.out.println("Shop Capstone Web Server Started");
        System.out.println("==================================");
        System.out.println("Server running at: http://localhost:" + PORT);
        System.out.println("Access the app at: http://localhost:" + PORT);
        System.out.println("React build directory: " + webRoot());
        System.out.println("\nTest Credentials:");
        System.out.println("Email: admin@example.com | Password: adminpass");
        System.out.println("Email: asha@example.com | Password: pass1234");
        System.out.println("==================================");
    }

    public static void main(String[] args) throws IOException {
        startServer();
    }

    // Static File Handler
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                sendReactShell(exchange, webRoot());
                return;
            }

            Path webRoot = webRoot();
            Path filePath = webRoot.resolve(path.substring(1)).normalize();

            if (!filePath.startsWith(webRoot)) {
                sendNotFound(exchange, path);
                return;
            }

            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                String contentType = getContentType(filePath.toString());
                byte[] fileContent = Files.readAllBytes(filePath);

                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, fileContent.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(fileContent);
                }
            } else if (!path.startsWith("/api/")) {
                sendReactShell(exchange, webRoot);
            } else {
                sendNotFound(exchange, path);
            }
        }

        private void sendReactShell(HttpExchange exchange, Path webRoot) throws IOException {
            Optional<Path> scriptPath = findAsset(webRoot, ".js");
            Optional<Path> stylePath = findAsset(webRoot, ".css");

            if (scriptPath.isEmpty()) {
                String message = "React app is not built. Run: cd frontend && npm install && npm run build";
                byte[] response = message.getBytes();

                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(500, response.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
                return;
            }

            String script = "/" + webRoot.relativize(scriptPath.get()).toString().replace("\\", "/");
            String styleLink = stylePath
                    .map(path -> "<link rel=\"stylesheet\" href=\"/" + webRoot.relativize(path).toString().replace("\\", "/") + "\">")
                    .orElse("");
            String html = "<!doctype html><html lang=\"en\"><head><meta charset=\"UTF-8\">" +
                    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                    "<title>BuyIt</title>" + styleLink + "</head><body><div id=\"root\"></div>" +
                    "<script type=\"module\" src=\"" + script + "\"></script></body></html>";
            byte[] response = html.getBytes();

            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, response.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }

        private Optional<Path> findAsset(Path webRoot, String extension) throws IOException {
            Path assets = webRoot.resolve("assets");
            if (!Files.isDirectory(assets)) {
                return Optional.empty();
            }

            try (Stream<Path> files = Files.list(assets)) {
                return files
                        .filter(path -> path.getFileName().toString().endsWith(extension))
                        .findFirst();
            }
        }

        private void sendNotFound(HttpExchange exchange, String path) throws IOException {
            String notFound = "<!DOCTYPE html><html><head><title>404</title></head>" +
                    "<body><h1>404 - File Not Found</h1><p>Resource: " + path + "</p></body></html>";
            byte[] response = notFound.getBytes();

            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(404, response.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }

        private String getContentType(String filePath) {
            if (filePath.endsWith(".html")) return "text/html";
            if (filePath.endsWith(".css")) return "text/css";
            if (filePath.endsWith(".js")) return "application/javascript";
            if (filePath.endsWith(".mjs")) return "application/javascript";
            if (filePath.endsWith(".json")) return "application/json";
            if (filePath.endsWith(".png")) return "image/png";
            if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return "image/jpeg";
            if (filePath.endsWith(".gif")) return "image/gif";
            return "application/octet-stream";
        }
    }

    // Login API Handler
    static class LoginHandler implements HttpHandler {
        private final UserService userService = new UserService();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;

            if (exchange.getRequestMethod().equals("POST")) {
                String body = readJsonBody(exchange);
                String email = extractJsonValue(body, "email");
                String password = extractJsonValue(body, "password");

                User user = userService.getAllUsers().stream()
                        .filter(u -> u.getEmail().equalsIgnoreCase(email) && u.getPassword().equals(password))
                        .findFirst()
                        .orElse(null);

                String response;
                if (user != null) {
                    response = String.format(
                        "{\"success\": true, \"message\": \"Login successful\", \"user\": {\"id\": %d, \"name\": \"%s\", \"email\": \"%s\", \"role\": \"%s\"}}",
                        user.getId(), escapeJson(user.getName()), escapeJson(user.getEmail()), user.getRole().name()
                    );
                } else {
                    response = "{\"success\": false, \"message\": \"Invalid email or password\"}";
                }

                respondJson(exchange, response);
            } else {
                sendMethodNotAllowed(exchange);
            }
        }
    }

    // Register API Handler
    static class RegisterHandler implements HttpHandler {
        private final UserService userService = new UserService();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;

            if (exchange.getRequestMethod().equals("POST")) {
                String body = readJsonBody(exchange);
                String name = extractJsonValue(body, "name");
                String email = extractJsonValue(body, "email");
                String password = extractJsonValue(body, "password");

                String response;
                if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    response = "{\"success\": false, \"message\": \"Name, email and password are required\"}";
                } else if (userService.getAllUsers().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email))) {
                    response = "{\"success\": false, \"message\": \"Email is already registered\"}";
                } else {
                    int id = userService.nextUserId();
                    userService.addUser(new Customer(id, name, email, password));
                    response = String.format(
                        "{\"success\": true, \"message\": \"Registration successful\", \"user\": {\"id\": %d, \"name\": \"%s\", \"email\": \"%s\", \"role\": \"%s\"}}",
                        id, escapeJson(name), escapeJson(email), "CUSTOMER"
                    );
                }

                respondJson(exchange, response);
            } else {
                sendMethodNotAllowed(exchange);
            }
        }
    }

    // Product API Handler
    static class ProductHandler implements HttpHandler {
        private final ProductService productService = new ProductService();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String idPart = path.endsWith("/products") ? "" : path.substring(path.lastIndexOf("/") + 1);

            if (method.equals("GET")) {
                List<Product> products = productService.getAllProducts();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < products.size(); i++) {
                    Product p = products.get(i);
                    json.append(String.format(
                        "{\"id\": %d, \"name\": \"%s\", \"price\": %.2f, \"quantity\": %d}",
                        p.getId(), escapeJson(p.getName()), p.getPrice(), p.getQuantity()
                    ));
                    if (i < products.size() - 1) json.append(",");
                }
                json.append("]");

                respondJson(exchange, json.toString());
            } else if (method.equals("POST") && idPart.isEmpty()) {
                String body = readJsonBody(exchange);
                String name = extractJsonValue(body, "name");
                double price = parseDoubleSafe(extractJsonValue(body, "price"));
                int quantity = parseIntSafe(extractJsonValue(body, "quantity"));
                int id = productService.getAllProducts().stream()
                        .mapToInt(Product::getId).max().orElse(0) + 1;

                if (name.isEmpty() || price < 0 || quantity < 0) {
                    respondJson(exchange, "{\"success\": false, \"message\": \"Invalid product data\"}");
                    return;
                }

                try {
                    productService.addProduct(new Product(id, name, price, quantity));
                    respondJson(exchange, String.format(
                        "{\"success\": true, \"message\": \"Product added\", \"id\": %d}", id));
                } catch (RuntimeException e) {
                    respondJson(exchange, "{\"success\": false, \"message\": \"" + escapeJson(e.getMessage()) + "\"}");
                }
            } else if (method.equals("PUT") && !idPart.isEmpty()) {
                int id = parseIntSafe(idPart);
                String body = readJsonBody(exchange);
                String name = extractJsonValue(body, "name");
                double price = parseDoubleSafe(extractJsonValue(body, "price"));
                int quantity = parseIntSafe(extractJsonValue(body, "quantity"));

                if (name.isEmpty() || price < 0 || quantity < 0) {
                    respondJson(exchange, "{\"success\": false, \"message\": \"Invalid product data\"}");
                    return;
                }

                boolean updated = productService.updateProduct(id, name, price, quantity);
                respondJson(exchange, updated
                    ? "{\"success\": true, \"message\": \"Product updated\"}"
                    : "{\"success\": false, \"message\": \"Product not found\"}");
            } else if (method.equals("DELETE") && !idPart.isEmpty()) {
                int id = parseIntSafe(idPart);
                boolean removed = productService.removeProductById(id);
                respondJson(exchange, removed
                    ? "{\"success\": true, \"message\": \"Product deleted\"}"
                    : "{\"success\": false, \"message\": \"Product not found\"}");
            } else {
                sendMethodNotAllowed(exchange);
            }
        }
    }

    // Order API Handler
    static class OrderHandler implements HttpHandler {
        private final OrderService orderService = new OrderService();
        private final ProductService productService = new ProductService();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;

            if (exchange.getRequestMethod().equals("GET")) {
                List<Order> orders = orderService.getAllOrders();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < orders.size(); i++) {
                    Order o = orders.get(i);
                    json.append(String.format(
                        "{\"id\": %d, \"customer_id\": %d, \"customer\": \"%s\", \"total\": %.2f, \"status\": \"%s\", \"items\": %d}",
                        o.getId(), o.getCustomer().getId(), escapeJson(o.getCustomer().getName()), o.getTotalAmount(),
                        escapeJson(o.getItems().isEmpty() ? "Pending" : "Processing"),
                        o.getItems().size()
                    ));
                    if (i < orders.size() - 1) json.append(",");
                }
                json.append("]");

                respondJson(exchange, json.toString());
            } else if (exchange.getRequestMethod().equals("POST")) {
                String body = readJsonBody(exchange);
                int customerId = parseIntSafe(extractJsonValue(body, "customer_id"));

                try {
                    Customer customer = findCustomer(customerId);
                    List<OrderItem> items = parseOrderItems(body);
                    if (customer == null) {
                        respondJson(exchange, "{\"success\": false, \"message\": \"Customer not found\"}");
                        return;
                    }
                    if (items.isEmpty()) {
                        respondJson(exchange, "{\"success\": false, \"message\": \"Order must contain at least one item\"}");
                        return;
                    }

                    Order order = orderService.createOrder(customer, items);
                    respondJson(exchange, String.format(
                        "{\"success\": true, \"message\": \"Order created\", \"id\": %d, \"total\": %.2f}",
                        order.getId(), order.getTotalAmount()));
                } catch (RuntimeException e) {
                    respondJson(exchange, "{\"success\": false, \"message\": \"" + escapeJson(e.getMessage()) + "\"}");
                }
            } else {
                sendMethodNotAllowed(exchange);
            }
        }

        private Customer findCustomer(int customerId) {
            return new UserService().getAllUsers().stream()
                    .filter(u -> u.getId() == customerId && u instanceof Customer)
                    .map(u -> (Customer) u)
                    .findFirst()
                    .orElse(null);
        }

        private List<OrderItem> parseOrderItems(String json) {
            List<OrderItem> items = new ArrayList<>();
            int idx = json.indexOf("\"items\"");
            if (idx == -1) return items;

            int start = json.indexOf("[", idx);
            int end = json.lastIndexOf("]");
            if (start == -1 || end == -1 || end <= start) return items;

            String array = json.substring(start + 1, end);
            for (String obj : splitJsonObjects(array)) {
                int productId = parseIntSafe(extractJsonValue(obj, "product_id"));
                int quantity = parseIntSafe(extractJsonValue(obj, "quantity"));
                if (productId <= 0 || quantity <= 0) continue;
                Product product = productService.getProductById(productId);
                if (product == null) continue;
                items.add(new OrderItem(product.getId(), product.getName(), product.getPrice(), quantity));
            }
            return items;
        }
    }

    // User API Handler
    static class UserHandler implements HttpHandler {
        private final UserService userService = new UserService();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String idPart = path.endsWith("/users") ? "" : path.substring(path.lastIndexOf("/") + 1);

            if (method.equals("GET")) {
                List<User> users = userService.getAllUsers();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < users.size(); i++) {
                    User u = users.get(i);
                    json.append(String.format(
                        "{\"id\": %d, \"name\": \"%s\", \"email\": \"%s\", \"role\": \"%s\"}",
                        u.getId(), escapeJson(u.getName()), escapeJson(u.getEmail()), u.getRole().name()
                    ));
                    if (i < users.size() - 1) json.append(",");
                }
                json.append("]");

                respondJson(exchange, json.toString());
            } else if (method.equals("POST") && idPart.isEmpty()) {
                String body = readJsonBody(exchange);
                String name = extractJsonValue(body, "name");
                String email = extractJsonValue(body, "email");
                String password = extractJsonValue(body, "password");
                String role = extractJsonValue(body, "role");

                if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    respondJson(exchange, "{\"success\": false, \"message\": \"Name, email and password are required\"}");
                    return;
                }
                if (userService.getAllUsers().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email))) {
                    respondJson(exchange, "{\"success\": false, \"message\": \"Email is already registered\"}");
                    return;
                }

                int id = userService.nextUserId();
                User user = "ADMIN".equalsIgnoreCase(role)
                        ? new Admin(id, name, email, password)
                        : new Customer(id, name, email, password);
                userService.addUser(user);
                respondJson(exchange, String.format(
                    "{\"success\": true, \"message\": \"User added\", \"id\": %d}", id));
            } else if (method.equals("DELETE") && !idPart.isEmpty()) {
                int id = parseIntSafe(idPart);
                boolean removed = userService.removeUserById(id);
                respondJson(exchange, removed
                    ? "{\"success\": true, \"message\": \"User deleted\"}"
                    : "{\"success\": false, \"message\": \"User not found\"}");
            } else {
                sendMethodNotAllowed(exchange);
            }
        }
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Shared helpers ---------------------------------------------------------

    private static String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        if (start >= json.length()) return "";
        if (json.charAt(start) == '"') {
            int end = json.indexOf("\"", start + 1);
            if (end == -1) return "";
            return json.substring(start + 1, end);
        }
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.'
                || json.charAt(end) == '-' || json.charAt(end) == '+')) {
            end++;
        }
        return json.substring(start, end);
    }

    private static String readJsonBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            return body.toString();
        }
    }

    private static void respondJson(HttpExchange exchange, String json) throws IOException {
        byte[] response = json.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private static boolean handleCors(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("OPTIONS")) return false;
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1);
        return true;
    }

    private static int parseIntSafe(String value) {
        try {
            return (int) Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static List<String> splitJsonObjects(String array) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < array.length(); i++) {
            char c = array.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    objects.add(array.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return objects;
    }

    // Send Method Not Allowed response
    static void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        String response = "Method Not Allowed";
        exchange.sendResponseHeaders(405, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}
