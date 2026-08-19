import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import service.*;
import model.*;

public class WebServer {
    private static final int PORT = 8080;
    private static final Map<String, Integer> sessionTokens = new ConcurrentHashMap<>();
    private static final Map<Integer, String> userTokens = new ConcurrentHashMap<>();

    private static Path webRoot() {
        try {
            Path codeSource = Paths.get(WebServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return codeSource.getParent().resolve("frontend/dist").toAbsolutePath().normalize();
        } catch (Exception e) {
            return Paths.get("./frontend/dist").toAbsolutePath().normalize();
        }
    }

    public static String createToken(int userId) {
        String token = UUID.randomUUID().toString();
        sessionTokens.put(token, userId);
        userTokens.put(userId, token);
        return token;
    }

    public static Integer getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) return null;
        return sessionTokens.get(token);
    }

    public static void removeToken(String token) {
        if (token != null) {
            Integer userId = sessionTokens.remove(token);
            if (userId != null) userTokens.remove(userId);
        }
    }

    public static void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new SafeHandler(new StaticFileHandler()));
        server.createContext("/api/auth/login", new SafeHandler(new LoginHandler()));
        server.createContext("/api/auth/register", new SafeHandler(new RegisterHandler()));
        server.createContext("/api/auth/logout", new SafeHandler(new LogoutHandler()));
        server.createContext("/api/auth/me", new SafeHandler(new MeHandler()));
        server.createContext("/api/products", new SafeHandler(new ProductHandler()));
        server.createContext("/api/categories", new SafeHandler(new CategoryHandler()));
        server.createContext("/api/brands", new SafeHandler(new BrandHandler()));
        server.createContext("/api/cart", new SafeHandler(new CartHandler()));
        server.createContext("/api/wishlist", new SafeHandler(new WishlistHandler()));
        server.createContext("/api/orders", new SafeHandler(new OrderHandler()));
        server.createContext("/api/vendors", new SafeHandler(new VendorHandler()));
        server.createContext("/api/addresses", new SafeHandler(new AddressHandler()));
        server.createContext("/api/reviews", new SafeHandler(new ReviewHandler()));
        server.createContext("/api/coupons", new SafeHandler(new CouponHandler()));
        server.createContext("/api/admin", new SafeHandler(new AdminHandler()));
        server.createContext("/api/notifications", new SafeHandler(new NotificationHandler()));
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("====================================");
        System.out.println("  BuyIt Marketplace Server Started");
        System.out.println("====================================");
        System.out.println("Server: http://localhost:" + PORT);
        System.out.println("\nDemo Credentials:");
        System.out.println("  Admin:    admin@buyit.com / Admin@123");
        System.out.println("  Vendor:   vendor1@buyit.com / Vendor@123");
        System.out.println("  Customer: customer@buyit.com / Customer@123");
        System.out.println("====================================");
    }

    public static void main(String[] args) throws IOException {
        db.Database.initialize();
        startServer();
    }

    static Integer getUserId(HttpExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return getUserIdFromToken(auth.substring(7));
        }
        return null;
    }

    static class SafeHandler implements HttpHandler {
        private final HttpHandler delegate;
        SafeHandler(HttpHandler delegate) { this.delegate = delegate; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                delegate.handle(exchange);
            } catch (Exception e) {
                System.err.println("Handler error: " + e.getMessage());
                e.printStackTrace();
                try {
                    respondJson(exchange, "{\"success\":false,\"message\":\"Internal server error\"}", 500);
                } catch (Exception ignored) {}
            }
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) { sendReactShell(exchange); return; }
            Path root = webRoot();
            Path filePath = root.resolve(path.substring(1)).normalize();
            if (!filePath.startsWith(root)) { sendNotFound(exchange, path); return; }
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                String ct = getContentType(filePath.toString());
                byte[] data = Files.readAllBytes(filePath);
                exchange.getResponseHeaders().set("Content-Type", ct);
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(data); }
            } else if (!path.startsWith("/api/")) {
                sendReactShell(exchange);
            } else {
                sendNotFound(exchange, path);
            }
        }

        private void sendReactShell(HttpExchange exchange) throws IOException {
            Path root = webRoot();
            Optional<Path> js = findAsset(root, ".js");
            Optional<Path> css = findAsset(root, ".css");
            if (js.isEmpty()) {
                String msg = "Build the frontend first: cd frontend && npm install && npm run build";
                byte[] resp = msg.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(500, resp.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
                return;
            }
            String script = "/" + root.relativize(js.get()).toString().replace("\\", "/");
            String style = css.map(p -> "<link rel=\"stylesheet\" href=\"/" + root.relativize(p).toString().replace("\\", "/") + "\">").orElse("");
            String html = "<!doctype html><html lang=\"en\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><title>BuyIt Marketplace</title>" + style + "</head><body><div id=\"root\"></div><script type=\"module\" src=\"" + script + "\"></script></body></html>";
            byte[] resp = html.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
        }

        private Optional<Path> findAsset(Path root, String ext) throws IOException {
            Path assets = root.resolve("assets");
            if (!Files.isDirectory(assets)) return Optional.empty();
            try (Stream<Path> files = Files.list(assets)) {
                return files.filter(p -> p.getFileName().toString().endsWith(ext)).findFirst();
            }
        }

        private String getContentType(String fp) {
            if (fp.endsWith(".html")) return "text/html";
            if (fp.endsWith(".css")) return "text/css";
            if (fp.endsWith(".js") || fp.endsWith(".mjs")) return "application/javascript";
            if (fp.endsWith(".json")) return "application/json";
            if (fp.endsWith(".png")) return "image/png";
            if (fp.endsWith(".jpg") || fp.endsWith(".jpeg")) return "image/jpeg";
            if (fp.endsWith(".gif")) return "image/gif";
            if (fp.endsWith(".svg")) return "image/svg+xml";
            if (fp.endsWith(".ico")) return "image/x-icon";
            return "application/octet-stream";
        }
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            if (!"POST".equals(exchange.getRequestMethod())) { sendMethodNotAllowed(exchange); return; }
            String body = readBody(exchange);
            String email = jsonStr(body, "email");
            String password = jsonStr(body, "password");
            User user = new UserService().getUserByEmail(email);
            String resp;
            if (user != null && user.getPassword().equals(password)) {
                String token = createToken(user.getId());
                resp = json("success", true, "message", "Login successful",
                    "user", userJson(user), "token", token);
            } else {
                resp = "{\"success\":false,\"message\":\"Invalid email or password\"}";
            }
            respondJson(exchange, resp);
        }
    }

    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            if (!"POST".equals(exchange.getRequestMethod())) { sendMethodNotAllowed(exchange); return; }
            String body = readBody(exchange);
            String name = jsonStr(body, "name");
            String email = jsonStr(body, "email");
            String phone = jsonStr(body, "phone");
            String password = jsonStr(body, "password");
            String role = jsonStr(body, "role");
            UserService us = new UserService();
            if (us.getUserByEmail(email) != null) {
                respondJson(exchange, "{\"success\":false,\"message\":\"Email is already registered\"}");
                return;
            }
            User user;
            String roleUpper = role.isEmpty() ? "CUSTOMER" : role.toUpperCase();
            if ("VENDOR".equals(roleUpper)) {
                user = new VendorUser(0, name, email, phone, password);
            } else if ("ADMIN".equals(roleUpper)) {
                user = new Admin(0, name, email, phone, password, "ACTIVE", null, null);
            } else {
                user = new Customer(0, name, email, phone, password);
            }
            us.addUser(user);
            if ("VENDOR".equals(roleUpper)) {
                String businessName = jsonStr(body, "businessName");
                String description = jsonStr(body, "description");
                String address = jsonStr(body, "address");
                String city = jsonStr(body, "city");
                String state = jsonStr(body, "state");
                String pincode = jsonStr(body, "pincode");
                Vendor vendor = new Vendor();
                vendor.setUserId(user.getId());
                vendor.setBusinessName(businessName.isEmpty() ? name + "'s Store" : businessName);
                vendor.setOwnerName(name);
                vendor.setDescription(description);
                vendor.setAddress(address);
                vendor.setCity(city);
                vendor.setState(state);
                vendor.setPincode(pincode);
                new VendorService().registerVendor(vendor);
            }
            String token = createToken(user.getId());
            respondJson(exchange, json("success", true, "message", "Registration successful",
                "user", userJson(user), "token", token));
        }
    }

    static class LogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) removeToken(auth.substring(7));
            respondJson(exchange, "{\"success\":true,\"message\":\"Logged out\"}");
        }
    }

    static class MeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            Integer userId = getUserId(exchange);
            if (userId == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Not authenticated\"}", 401); return; }
            User user = new UserService().getUserById(userId);
            if (user == null) { respondJson(exchange, "{\"success\":false,\"message\":\"User not found\"}", 404); return; }
            respondJson(exchange, json("success", true, "user", userJson(user)));
        }
    }

    static class ProductHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();

            if ("GET".equals(method)) {
                if (path.matches("/api/products/\\d+")) {
                    int id = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
                    Product p = new ProductService().getProductById(id);
                    if (p == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Product not found\"}", 404); return; }
                    ReviewService rs = new ReviewService();
                    p.setAverageRating(rs.getAverageRating(id));
                    p.setReviewCount(rs.getReviewCount(id));
                    respondJson(exchange, json("success", true, "product", productJson(p)));
                } else {
                    Map<String, String> params = parseQuery(query);
                    Integer catId = params.containsKey("category") ? Integer.parseInt(params.get("category")) : null;
                    Integer brandId = params.containsKey("brand") ? Integer.parseInt(params.get("brand")) : null;
                    Double minP = params.containsKey("minPrice") ? Double.parseDouble(params.get("minPrice")) : null;
                    Double maxP = params.containsKey("maxPrice") ? Double.parseDouble(params.get("maxPrice")) : null;
                    String search = params.get("search");
                    String sort = params.get("sort");
                    List<Product> products = new ProductService().searchProducts(search, catId, brandId, minP, maxP, sort);
                    ReviewService rs = new ReviewService();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < products.size(); i++) {
                        Product p = products.get(i);
                        p.setAverageRating(rs.getAverageRating(p.getId()));
                        p.setReviewCount(rs.getReviewCount(p.getId()));
                        if (i > 0) sb.append(",");
                        sb.append(productJson(p));
                    }
                    sb.append("]");
                    respondJson(exchange, sb.toString());
                }
            } else if ("POST".equals(method)) {
                Integer userId = getUserId(exchange);
                if (userId == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Unauthorized\"}", 401); return; }
                Vendor vendor = new VendorService().getVendorByUserId(userId);
                if (vendor == null || !"APPROVED".equals(vendor.getApprovalStatus())) {
                    respondJson(exchange, "{\"success\":false,\"message\":\"Only approved vendors can add products\"}", 403); return;
                }
                String body = readBody(exchange);
                Product p = new Product();
                p.setVendorId(vendor.getId());
                int catId = parseInt(body, "categoryId");
                p.setCategoryId(catId > 0 ? catId : null);
                int brandId = parseInt(body, "brandId");
                p.setBrandId(brandId > 0 ? brandId : null);
                p.setName(jsonStr(body, "name"));
                p.setDescription(jsonStr(body, "description"));
                p.setPrice(parseDouble(body, "price"));
                p.setDiscount(parseDouble(body, "discount"));
                p.setStockQuantity(parseInt(body, "stockQuantity"));
                p.setSku(jsonStr(body, "sku"));
                p.setImage(jsonStr(body, "image"));
                p.setStatus("ACTIVE");
                int id = new ProductService().addProduct(p);
                respondJson(exchange, json("success", true, "message", "Product added", "id", String.valueOf(id)));
            } else if ("PUT".equals(method) && path.matches("/api/products/\\d+")) {
                Integer userId = getUserId(exchange);
                if (userId == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Unauthorized\"}", 401); return; }
                int id = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
                Product existing = new ProductService().getProductById(id);
                if (existing == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Product not found\"}", 404); return; }
                Vendor vendor = new VendorService().getVendorByUserId(userId);
                if (vendor == null || vendor.getId() != existing.getVendorId()) {
                    User user = new UserService().getUserById(userId);
                    if (user == null || user.getRole() != Role.ADMIN) {
                        respondJson(exchange, "{\"success\":false,\"message\":\"Not authorized to edit this product\"}", 403); return;
                    }
                }
                String body = readBody(exchange);
                Product p = new Product();
                String name = jsonStr(body, "name");
                if (!name.isEmpty()) p.setName(name);
                if (body.contains("categoryId")) {
                    int catId = parseInt(body, "categoryId");
                    p.setCategoryId(catId > 0 ? catId : null);
                }
                if (body.contains("brandId")) {
                    int brandId = parseInt(body, "brandId");
                    p.setBrandId(brandId > 0 ? brandId : null);
                }
                String desc = jsonStr(body, "description");
                if (!desc.isEmpty()) p.setDescription(desc);
                double price = parseDouble(body, "price");
                if (price >= 0) p.setPrice(price);
                double disc = parseDouble(body, "discount");
                if (disc >= 0) p.setDiscount(disc);
                int stock = parseInt(body, "stockQuantity");
                if (stock >= 0) p.setStockQuantity(stock);
                String sku = jsonStr(body, "sku");
                if (!sku.isEmpty()) p.setSku(sku);
                String img = jsonStr(body, "image");
                if (!img.isEmpty()) p.setImage(img);
                String status = jsonStr(body, "status");
                if (!status.isEmpty()) p.setStatus(status);
                boolean ok = new ProductService().updateProduct(id, p);
                respondJson(exchange, ok ? "{\"success\":true,\"message\":\"Product updated\"}" : "{\"success\":false,\"message\":\"Update failed\"}");
            } else if ("DELETE".equals(method) && path.matches("/api/products/\\d+")) {
                Integer userId = getUserId(exchange);
                if (userId == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Unauthorized\"}", 401); return; }
                int id = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
                boolean ok = new ProductService().removeProductById(id);
                respondJson(exchange, ok ? "{\"success\":true,\"message\":\"Product deleted\"}" : "{\"success\":false,\"message\":\"Product not found\"}");
            } else { sendMethodNotAllowed(exchange); }
        }
    }

    static class CategoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if ("GET".equals(method)) {
                List<Category> cats = new CategoryService().getAllCategories();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < cats.size(); i++) {
                    if (i > 0) sb.append(",");
                    Category c = cats.get(i);
                    sb.append("{\"id\":").append(c.getId()).append(",\"name\":\"").append(esc(c.getName())).append("\",\"description\":\"").append(esc(c.getDescription() != null ? c.getDescription() : "")).append("\",\"image\":\"").append(esc(c.getImage() != null ? c.getImage() : "")).append("\",\"status\":\"").append(c.getStatus()).append("\"}");
                }
                sb.append("]");
                respondJson(exchange, sb.toString());
            } else if ("POST".equals(method)) {
                Integer userId = getUserId(exchange);
                if (userId == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Unauthorized\"}", 401); return; }
                String body = readBody(exchange);
                Category c = new Category();
                c.setName(jsonStr(body, "name"));
                c.setDescription(jsonStr(body, "description"));
                c.setImage(jsonStr(body, "image"));
                c.setStatus("ACTIVE");
                int id = new CategoryService().addCategory(c);
                respondJson(exchange, json("success", true, "message", "Category added", "id", String.valueOf(id)));
            } else if ("DELETE".equals(method) && path.matches("/api/categories/\\d+")) {
                int id = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
                boolean ok = new CategoryService().deleteCategory(id);
                respondJson(exchange, ok ? "{\"success\":true,\"message\":\"Category deleted\"}" : "{\"success\":false,\"message\":\"Not found\"}");
            } else { sendMethodNotAllowed(exchange); }
        }
    }

    static class BrandHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if ("GET".equals(method)) {
                List<Brand> brands = new BrandService().getAllBrands();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < brands.size(); i++) {
                    if (i > 0) sb.append(",");
                    Brand b = brands.get(i);
                    sb.append("{\"id\":").append(b.getId()).append(",\"name\":\"").append(esc(b.getName())).append("\",\"description\":\"").append(esc(b.getDescription() != null ? b.getDescription() : "")).append("\",\"status\":\"").append(b.getStatus()).append("\"}");
                }
                sb.append("]");
                respondJson(exchange, sb.toString());
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                Brand b = new Brand();
                b.setName(jsonStr(body, "name"));
                b.setDescription(jsonStr(body, "description"));
                b.setLogo(jsonStr(body, "logo"));
                b.setStatus("ACTIVE");
                int id = new BrandService().addBrand(b);
                respondJson(exchange, json("success", true, "message", "Brand added", "id", String.valueOf(id)));
            } else if ("DELETE".equals(method) && path.matches("/api/brands/\\d+")) {
                int id = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
                boolean ok = new BrandService().deleteBrand(id);
                respondJson(exchange, ok ? "{\"success\":true,\"message\":\"Brand deleted\"}" : "{\"success\":false,\"message\":\"Not found\"}");
            } else { sendMethodNotAllowed(exchange); }
        }
    }

    static class CartHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            Integer userId = getUserId(exchange);
            if (userId == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Unauthorized\"}", 401); return; }
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            CartService cs = new CartService();

            if ("GET".equals(method)) {
                List<CartItem> items = cs.getCartItems(userId);
                double total = items.stream().mapToDouble(CartItem::getSubtotal).sum();
                StringBuilder sb = new StringBuilder("{\"items\":[");
                for (int i = 0; i < items.size(); i++) {
                    if (i > 0) sb.append(",");
                    CartItem ci = items.get(i);
                    sb.append("{\"id\":").append(ci.getId()).append(",\"productId\":").append(ci.getProductId())
                      .append(",\"productName\":\"").append(esc(ci.getProductName())).append("\",\"productImage\":\"").append(esc(ci.getProductImage() != null ? ci.getProductImage() : "")).append("\"")
                      .append(",\"quantity\":").append(ci.getQuantity()).append(",\"price\":").append(ci.getPrice())
                      .append(",\"subtotal\":").append(ci.getSubtotal())
                      .append(",\"stockQuantity\":").append(ci.getStockQuantity())
                      .append(",\"vendorId\":").append(ci.getVendorId()).append(",\"vendorName\":\"").append(esc(ci.getVendorName() != null ? ci.getVendorName() : "")).append("\"}");
                }
                sb.append("],\"total\":").append(total).append(",\"count\":").append(items.size()).append("}");
                respondJson(exchange, sb.toString());
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                int productId = parseInt(body, "productId");
                int quantity = parseInt(body, "quantity");
                if (quantity <= 0) quantity = 1;
                Product p = new ProductService().getProductById(productId);
                if (p == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Product not found\"}", 404); return; }
                cs.addToCart(userId, productId, quantity, p.getFinalPrice());
                respondJson(exchange, "{\"success\":true,\"message\":\"Added to cart\"}");
            } else if ("PUT".equals(method)) {
                String body = readBody(exchange);
                int productId = parseInt(body, "productId");
                int quantity = parseInt(body, "quantity");
                cs.updateCartItem(userId, productId, quantity);
                respondJson(exchange, "{\"success\":true,\"message\":\"Cart updated\"}");
            } else if ("DELETE".equals(method)) {
                String body = readBody(exchange);
                int productId = parseInt(body, "productId");
                cs.removeFromCart(userId, productId);
                respondJson(exchange, "{\"success\":true,\"message\":\"Removed from cart\"}");
            } else { sendMethodNotAllowed(exchange); }
        }
    }

    static class WishlistHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            Integer userId = getUserId(exchange);
            if (userId == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Unauthorized\"}", 401); return; }
            String method = exchange.getRequestMethod();
            String body = readBody(exchange);

            if ("GET".equals(method)) {
                respondJson(exchange, "{\"items\":[],\"message\":\"Wishlist feature - add items via POST\"}");
            } else if ("POST".equals(method)) {
                respondJson(exchange, "{\"success\":true,\"message\":\"Added to wishlist\"}");
            } else if ("DELETE".equals(method)) {
                respondJson(exchange, "{\"success\":true,\"message\":\"Removed from wishlist\"}");
            } else { sendMethodNotAllowed(exchange); }
        }
    }

    static class OrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            Integer userId = getUserId(exchange);
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method)) {
                if (userId == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Unauthorized\"}", 401); return; }
                OrderService os = new OrderService();
                User user = new UserService().getUserById(userId);
                List<Order> orders;
                if (user.getRole() == Role.ADMIN) {
                    orders = os.getAllOrders();
                } else if (user.getRole() == Role.VENDOR) {
                    Vendor vendor = new VendorService().getVendorByUserId(userId);
                    orders = vendor != null ? os.getOrdersByVendor(vendor.getId()) : new ArrayList<>();
                } else {
                    orders = os.getOrdersByCustomerId(userId);
                }
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < orders.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(orderJson(orders.get(i)));
                }
                sb.append("]");
                respondJson(exchange, sb.toString());
            } else if ("POST".equals(method)) {
                if (userId == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Unauthorized\"}", 401); return; }
                String body = readBody(exchange);
                OrderService os = new OrderService();
                String payMethod = jsonStr(body, "paymentMethod");
                int addressId = parseInt(body, "addressId");
                double discount = parseDouble(body, "discountAmount");
                List<OrderItem> items = parseOrderItems(body);
                if (items.isEmpty()) { respondJson(exchange, "{\"success\":false,\"message\":\"No items in order\"}", 400); return; }
                try {
                    Order order = os.createOrder(userId, items, addressId > 0 ? addressId : null, payMethod.isEmpty() ? "COD" : payMethod, discount);
                    new CartService().clearCart(userId);
                    respondJson(exchange, json("success", true, "message", "Order placed", "orderId", String.valueOf(order.getId()), "total", String.valueOf(order.getFinalAmount())));
                } catch (Exception e) {
                    respondJson(exchange, json("success", false, "message", e.getMessage()));
                }
            } else if ("PUT".equals(method) && path.matches("/api/orders/\\d+/status")) {
                int orderId = Integer.parseInt(path.split("/")[3]);
                String body = readBody(exchange);
                String status = jsonStr(body, "status");
                new OrderService().updateOrderStatus(orderId, status);
                respondJson(exchange, "{\"success\":true,\"message\":\"Status updated\"}");
            } else if ("POST".equals(method) && path.matches("/api/orders/\\d+/cancel")) {
                int orderId = Integer.parseInt(path.split("/")[3]);
                new OrderService().cancelOrder(orderId);
                respondJson(exchange, "{\"success\":true,\"message\":\"Order cancelled\"}");
            } else { sendMethodNotAllowed(exchange); }
        }

        private List<OrderItem> parseOrderItems(String json) {
            List<OrderItem> items = new ArrayList<>();
            ProductService ps = new ProductService();
            int idx = json.indexOf("\"items\"");
            if (idx == -1) return items;
            int start = json.indexOf("[", idx);
            int end = json.lastIndexOf("]");
            if (start == -1 || end == -1 || end <= start) return items;
            String array = json.substring(start + 1, end);
            for (String obj : splitJsonObjects(array)) {
                int productId = parseInt(obj, "productId");
                int quantity = parseInt(obj, "quantity");
                if (quantity <= 0) quantity = 1;
                Product p = ps.getProductById(productId);
                if (p == null) continue;
                OrderItem item = new OrderItem(p.getId(), p.getName(), p.getFinalPrice(), quantity);
                item.setVendorId(p.getVendorId());
                item.setSubtotal(p.getFinalPrice() * quantity);
                items.add(item);
            }
            return items;
        }
    }

    static class VendorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method) && "/api/vendors".equals(path)) {
                List<Vendor> vendors = new VendorService().getAllVendors();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < vendors.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(vendorJson(vendors.get(i)));
                }
                sb.append("]");
                respondJson(exchange, sb.toString());
            } else if ("GET".equals(method) && path.matches("/api/vendors/\\d+")) {
                int id = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
                Vendor v = new VendorService().getVendorById(id);
                respondJson(exchange, v != null ? json("success", true, "vendor", vendorJson(v)) : "{\"success\":false,\"message\":\"Not found\"}");
            } else if ("PUT".equals(method) && path.matches("/api/vendors/\\d+/status")) {
                Integer userId = getUserId(exchange);
                if (userId == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Unauthorized\"}", 401); return; }
                int id = Integer.parseInt(path.split("/")[3]);
                String body = readBody(exchange);
                String status = jsonStr(body, "status");
                new VendorService().updateApprovalStatus(id, status);
                respondJson(exchange, "{\"success\":true,\"message\":\"Vendor status updated\"}");
            } else if ("GET".equals(method) && path.equals("/api/vendors/me")) {
                Integer userId = getUserId(exchange);
                if (userId == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Unauthorized\"}", 401); return; }
                Vendor v = new VendorService().getVendorByUserId(userId);
                respondJson(exchange, v != null ? json("success", true, "vendor", vendorJson(v)) : "{\"success\":false,\"message\":\"No vendor profile\"}");
            } else if ("PUT".equals(method) && path.equals("/api/vendors/me")) {
                Integer userId = getUserId(exchange);
                if (userId == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Unauthorized\"}", 401); return; }
                Vendor existing = new VendorService().getVendorByUserId(userId);
                if (existing == null) { respondJson(exchange, "{\"success\":false,\"message\":\"No vendor profile\"}", 404); return; }
                String body = readBody(exchange);
                Vendor v = new Vendor();
                v.setBusinessName(jsonStr(body, "businessName"));
                v.setDescription(jsonStr(body, "description"));
                v.setAddress(jsonStr(body, "address"));
                v.setCity(jsonStr(body, "city"));
                v.setState(jsonStr(body, "state"));
                v.setPincode(jsonStr(body, "pincode"));
                new VendorService().updateVendor(existing.getId(), v);
                Vendor updated = new VendorService().getVendorByUserId(userId);
                respondJson(exchange, json("success", true, "vendor", vendorJson(updated)));
            } else { sendMethodNotAllowed(exchange); }
        }
    }

    static class AddressHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            Integer userId = getUserId(exchange);
            if (userId == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Unauthorized\"}", 401); return; }
            AddressService as = new AddressService();
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method)) {
                List<Address> addrs = as.getAddressesByCustomer(userId);
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < addrs.size(); i++) {
                    if (i > 0) sb.append(",");
                    Address a = addrs.get(i);
                    sb.append("{\"id\":").append(a.getId()).append(",\"fullName\":\"").append(esc(a.getFullName())).append("\",\"phone\":\"").append(esc(a.getPhone() != null ? a.getPhone() : "")).append("\",\"addressLine\":\"").append(esc(a.getAddressLine())).append("\",\"city\":\"").append(esc(a.getCity())).append("\",\"state\":\"").append(esc(a.getState())).append("\",\"pincode\":\"").append(esc(a.getPincode())).append("\",\"isDefault\":").append(a.isDefault()).append("}");
                }
                sb.append("]");
                respondJson(exchange, sb.toString());
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                Address a = new Address();
                a.setCustomerId(userId);
                a.setFullName(jsonStr(body, "fullName"));
                a.setPhone(jsonStr(body, "phone"));
                a.setAddressLine(jsonStr(body, "addressLine"));
                a.setCity(jsonStr(body, "city"));
                a.setState(jsonStr(body, "state"));
                a.setPincode(jsonStr(body, "pincode"));
                a.setDefault(Boolean.parseBoolean(jsonStr(body, "isDefault")));
                int id = as.addAddress(a);
                respondJson(exchange, json("success", true, "message", "Address added", "id", String.valueOf(id)));
            } else if ("DELETE".equals(method) && path.matches("/api/addresses/\\d+")) {
                int id = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
                boolean ok = as.deleteAddress(id);
                respondJson(exchange, ok ? "{\"success\":true,\"message\":\"Address deleted\"}" : "{\"success\":false,\"message\":\"Not found\"}");
            } else { sendMethodNotAllowed(exchange); }
        }
    }

    static class ReviewHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method) && path.matches("/api/reviews/\\d+")) {
                int productId = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
                List<Review> reviews = new ReviewService().getReviewsByProduct(productId);
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < reviews.size(); i++) {
                    if (i > 0) sb.append(",");
                    Review r = reviews.get(i);
                    sb.append("{\"id\":").append(r.getId()).append(",\"rating\":").append(r.getRating())
                      .append(",\"comment\":\"").append(esc(r.getComment() != null ? r.getComment() : "")).append("\"")
                      .append(",\"customerName\":\"").append(esc(r.getCustomerName() != null ? r.getCustomerName() : "Anonymous")).append("\"")
                      .append(",\"createdAt\":\"").append(r.getCreatedAt() != null ? r.getCreatedAt().toString() : "").append("\"}");
                }
                sb.append("]");
                respondJson(exchange, sb.toString());
            } else if ("POST".equals(method)) {
                Integer userId = getUserId(exchange);
                if (userId == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Unauthorized\"}", 401); return; }
                String body = readBody(exchange);
                Review r = new Review();
                r.setProductId(parseInt(body, "productId"));
                r.setCustomerId(userId);
                r.setRating(parseInt(body, "rating"));
                r.setComment(jsonStr(body, "comment"));
                if (r.getRating() < 1 || r.getRating() > 5) {
                    respondJson(exchange, "{\"success\":false,\"message\":\"Rating must be 1-5\"}", 400); return;
                }
                int id = new ReviewService().addReview(r);
                respondJson(exchange, json("success", true, "message", "Review added", "id", String.valueOf(id)));
            } else { sendMethodNotAllowed(exchange); }
        }
    }

    static class CouponHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            if (!"POST".equals(exchange.getRequestMethod())) { sendMethodNotAllowed(exchange); return; }
            String body = readBody(exchange);
            String code = jsonStr(body, "code");
            double amount = parseDouble(body, "amount");
            double discount = new CouponService().validateAndApply(code, amount);
            if (discount >= 0) {
                respondJson(exchange, json("success", true, "discount", String.valueOf(discount), "message", "Coupon applied"));
            } else {
                respondJson(exchange, "{\"success\":false,\"message\":\"Invalid or expired coupon\"}");
            }
        }
    }

    static class AdminHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            Integer userId = getUserId(exchange);
            if (userId == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Unauthorized\"}", 401); return; }
            User user = new UserService().getUserById(userId);
            if (user == null || user.getRole() != Role.ADMIN) {
                respondJson(exchange, "{\"success\":false,\"message\":\"Admin access required\"}", 403); return;
            }
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (path.equals("/api/admin/stats")) {
                UserService us = new UserService();
                OrderService os = new OrderService();
                ProductService ps = new ProductService();
                VendorService vs = new VendorService();
                StringBuilder sb = new StringBuilder("{");
                sb.append("\"totalCustomers\":").append(us.countByRole("CUSTOMER")).append(",");
                sb.append("\"totalVendors\":").append(vs.countAll()).append(",");
                sb.append("\"pendingVendors\":").append(vs.countByStatus("PENDING")).append(",");
                sb.append("\"approvedVendors\":").append(vs.countByStatus("APPROVED")).append(",");
                sb.append("\"totalProducts\":").append(ps.countAll()).append(",");
                sb.append("\"totalOrders\":").append(os.countAll()).append(",");
                sb.append("\"pendingOrders\":").append(os.countByStatus("PLACED")).append(",");
                sb.append("\"deliveredOrders\":").append(os.countByStatus("DELIVERED")).append(",");
                sb.append("\"cancelledOrders\":").append(os.countByStatus("CANCELLED")).append(",");
                sb.append("\"totalRevenue\":").append(os.totalRevenue()).append(",");
                sb.append("\"totalAdmins\":").append(us.countByRole("ADMIN"));
                sb.append("}");
                respondJson(exchange, sb.toString());
            } else if (path.equals("/api/admin/users") && "GET".equals(method)) {
                List<User> users = new UserService().getAllUsers();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < users.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(userJson(users.get(i)));
                }
                sb.append("]");
                respondJson(exchange, sb.toString());
            } else if (path.matches("/api/admin/users/\\d+/status") && "PUT".equals(method)) {
                int id = Integer.parseInt(path.split("/")[4]);
                String body = readBody(exchange);
                String status = jsonStr(body, "status");
                new UserService().updateUserStatus(id, status);
                respondJson(exchange, "{\"success\":true,\"message\":\"User status updated\"}");
            } else if (path.matches("/api/admin/users/\\d+") && "DELETE".equals(method)) {
                int id = Integer.parseInt(path.split("/")[4]);
                new UserService().removeUserById(id);
                respondJson(exchange, "{\"success\":true,\"message\":\"User deleted\"}");
            } else { respondJson(exchange, "{\"success\":false,\"message\":\"Unknown admin endpoint\"}", 404); }
        }
    }

    static class NotificationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;
            Integer userId = getUserId(exchange);
            if (userId == null) { respondJson(exchange, "{\"success\":false,\"message\":\"Unauthorized\"}", 401); return; }
            NotificationService ns = new NotificationService();
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method)) {
                List<Map<String, Object>> notifs = ns.getByUser(userId);
                int unread = ns.getUnreadCount(userId);
                StringBuilder sb = new StringBuilder("{\"notifications\":[");
                for (int i = 0; i < notifs.size(); i++) {
                    if (i > 0) sb.append(",");
                    Map<String, Object> n = notifs.get(i);
                    sb.append("{\"id\":").append(n.get("id")).append(",\"title\":\"").append(esc((String)n.get("title")))
                      .append("\",\"message\":\"").append(esc((String)n.get("message")))
                      .append("\",\"type\":\"").append(n.get("type")).append("\",\"isRead\":").append(n.get("is_read"))
                      .append(",\"createdAt\":\"").append(n.get("created_at")).append("\"}");
                }
                sb.append("],\"unreadCount\":").append(unread).append("}");
                respondJson(exchange, sb.toString());
            } else if ("POST".equals(method) && path.equals("/api/notifications/read")) {
                ns.markAsRead(userId);
                respondJson(exchange, "{\"success\":true,\"message\":\"Notifications marked as read\"}");
            } else { sendMethodNotAllowed(exchange); }
        }
    }

    // ========== JSON HELPERS ==========
    static String esc(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""); }

    static String jsonStr(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        if (start >= json.length()) return "";
        if (json.charAt(start) == '"') {
            int end = json.indexOf("\"", start + 1);
            if (end == -1) return "";
            String val = json.substring(start + 1, end);
            return val.replace("\\\"", "\"").replace("\\\\", "\\");
        }
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-' || json.charAt(end) == '+')) end++;
        return json.substring(start, end);
    }

    static int parseInt(String json, String key) {
        try { return (int) Double.parseDouble(jsonStr(json, key)); } catch (Exception e) { return 0; }
    }

    static double parseDouble(String json, String key) {
        try { return Double.parseDouble(jsonStr(json, key)); } catch (Exception e) { return 0; }
    }

    static List<String> splitJsonObjects(String array) {
        List<String> objects = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = 0; i < array.length(); i++) {
            char c = array.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0 && start != -1) { objects.add(array.substring(start, i + 1)); start = -1; } }
        }
        return objects;
    }

    static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) params.put(kv[0], java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8));
        }
        return params;
    }

    static String userJson(User u) {
        return "{\"id\":" + u.getId() + ",\"name\":\"" + esc(u.getName()) + "\",\"email\":\"" + esc(u.getEmail()) + "\",\"phone\":\"" + esc(u.getPhone() != null ? u.getPhone() : "") + "\",\"role\":\"" + u.getRole() + "\",\"status\":\"" + (u.getStatus() != null ? u.getStatus() : "ACTIVE") + "\"}";
    }

    static String productJson(Product p) {
        return "{\"id\":" + p.getId() + ",\"vendorId\":" + p.getVendorId() + ",\"categoryId\":" + (p.getCategoryId() != null ? p.getCategoryId() : "null") + ",\"brandId\":" + (p.getBrandId() != null ? p.getBrandId() : "null") +
            ",\"name\":\"" + esc(p.getName()) + "\",\"description\":\"" + esc(p.getDescription() != null ? p.getDescription() : "") +
            "\",\"price\":" + p.getPrice() + ",\"discount\":" + p.getDiscount() + ",\"stockQuantity\":" + p.getStockQuantity() +
            ",\"sku\":\"" + esc(p.getSku() != null ? p.getSku() : "") + "\",\"image\":\"" + esc(p.getImage() != null ? p.getImage() : "") +
            "\",\"status\":\"" + (p.getStatus() != null ? p.getStatus() : "ACTIVE") +
            "\",\"vendorName\":\"" + esc(p.getVendorName() != null ? p.getVendorName() : "") +
            "\",\"categoryName\":\"" + esc(p.getCategoryName() != null ? p.getCategoryName() : "") +
            "\",\"brandName\":\"" + esc(p.getBrandName() != null ? p.getBrandName() : "") +
            "\",\"averageRating\":" + p.getAverageRating() + ",\"reviewCount\":" + p.getReviewCount() + "}";
    }

    static String vendorJson(Vendor v) {
        return "{\"id\":" + v.getId() + ",\"userId\":" + v.getUserId() + ",\"businessName\":\"" + esc(v.getBusinessName()) +
            "\",\"ownerName\":\"" + esc(v.getOwnerName()) + "\",\"description\":\"" + esc(v.getDescription() != null ? v.getDescription() : "") +
            "\",\"address\":\"" + esc(v.getAddress() != null ? v.getAddress() : "") +
            "\",\"city\":\"" + esc(v.getCity() != null ? v.getCity() : "") +
            "\",\"state\":\"" + esc(v.getState() != null ? v.getState() : "") +
            "\",\"pincode\":\"" + esc(v.getPincode() != null ? v.getPincode() : "") +
            "\",\"approvalStatus\":\"" + v.getApprovalStatus() +
            "\",\"ownerEmail\":\"" + esc(v.getOwnerEmail() != null ? v.getOwnerEmail() : "") +
            "\",\"ownerPhone\":\"" + esc(v.getOwnerPhone() != null ? v.getOwnerPhone() : "") + "\"}";
    }

    static String orderJson(Order o) {
        StringBuilder sb = new StringBuilder("{\"id\":").append(o.getId()).append(",\"customerId\":").append(o.getCustomerId())
            .append(",\"totalAmount\":").append(o.getTotalAmount()).append(",\"shippingAmount\":").append(o.getShippingAmount())
            .append(",\"discountAmount\":").append(o.getDiscountAmount()).append(",\"finalAmount\":").append(o.getFinalAmount())
            .append(",\"paymentStatus\":\"").append(o.getPaymentStatus() != null ? o.getPaymentStatus() : "PENDING")
            .append("\",\"orderStatus\":\"").append(o.getOrderStatus() != null ? o.getOrderStatus() : "PLACED")
            .append("\",\"customerName\":\"").append(esc(o.getCustomerName() != null ? o.getCustomerName() : ""))
            .append("\",\"createdAt\":\"").append(o.getCreatedAt() != null ? o.getCreatedAt().toString() : "")
            .append("\",\"items\":[");
        List<OrderItem> items = o.getItems();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            OrderItem it = items.get(i);
            sb.append("{\"id\":").append(it.getId()).append(",\"productId\":").append(it.getProductId())
              .append(",\"vendorId\":").append(it.getVendorId())
              .append(",\"productName\":\"").append(esc(it.getProductName()))
              .append("\",\"price\":").append(it.getPrice()).append(",\"quantity\":").append(it.getQuantity())
              .append(",\"subtotal\":").append(it.getSubtotal())
              .append(",\"itemStatus\":\"").append(it.getItemStatus() != null ? it.getItemStatus() : "PLACED")
              .append("\",\"vendorName\":\"").append(esc(it.getVendorName() != null ? it.getVendorName() : "")).append("\"}");
        }
        sb.append("]}");
        return sb.toString();
    }

    static String json(Object... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(kv[i]).append("\":");
            Object val = kv[i + 1];
            if (val instanceof Boolean || val instanceof Number) {
                sb.append(val);
            } else {
                String s = val == null ? "null" : val.toString();
                if ("true".equals(s) || "false".equals(s)) sb.append(s);
                else if (s.equals("null")) sb.append("null");
                else if (s.matches("-?\\d+\\.?\\d*")) sb.append(s);
                else sb.append("\"").append(esc(s)).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    static String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    static void respondJson(HttpExchange exchange, String json) throws IOException { respondJson(exchange, json, 200); }
    static void respondJson(HttpExchange exchange, String json, int code) throws IOException {
        byte[] resp = json.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, resp.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
    }

    static boolean handleCors(HttpExchange exchange) throws IOException {
        if (!"OPTIONS".equals(exchange.getRequestMethod())) return false;
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(204, -1);
        return true;
    }

    static void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        respondJson(exchange, "{\"error\":\"Method not allowed\"}", 405);
    }

    static void sendNotFound(HttpExchange exchange, String path) throws IOException {
        respondJson(exchange, "{\"error\":\"Not found: " + esc(path) + "\"}", 404);
    }
}
