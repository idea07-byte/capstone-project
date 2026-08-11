import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WebServer {
    private static final int PORT = 8080;
    private static final String WEB_ROOT = "./web";

    public static void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Create context for static files
        server.createContext("/", new StaticFileHandler());

        // API endpoints (can be extended)
        server.createContext("/api/auth/login", new LoginHandler());
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
        System.out.println("\nTest Credentials:");
        System.out.println("Email: admin@shop.com | Password: admin123");
        System.out.println("Email: customer@shop.com | Password: customer123");
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
                path = "/index.html";
            }

            Path filePath = Paths.get(WEB_ROOT + path);

            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                String contentType = getContentType(filePath.toString());
                byte[] fileContent = Files.readAllBytes(filePath);

                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, fileContent.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(fileContent);
                }
            } else {
                String notFound = "<!DOCTYPE html><html><head><title>404</title></head>" +
                        "<body><h1>404 - File Not Found</h1><p>Resource: " + path + "</p></body></html>";
                byte[] response = notFound.getBytes();

                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(404, response.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            }
        }

        private String getContentType(String filePath) {
            if (filePath.endsWith(".html")) return "text/html";
            if (filePath.endsWith(".css")) return "text/css";
            if (filePath.endsWith(".js")) return "application/javascript";
            if (filePath.endsWith(".json")) return "application/json";
            if (filePath.endsWith(".png")) return "image/png";
            if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return "image/jpeg";
            if (filePath.endsWith(".gif")) return "image/gif";
            return "application/octet-stream";
        }
    }

    // Login API Handler
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if (exchange.getRequestMethod().equals("POST")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }

                // Mock authentication (integrate with UserService)
                String response = "{\"success\": true, \"message\": \"Login successful\", \"user\": {\"name\": \"Test User\", \"role\": \"Customer\"}}";

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, response.getBytes().length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                sendMethodNotAllowed(exchange);
            }
        }
    }

    // Product API Handler
    static class ProductHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equals("GET")) {
                String response = "[{\"id\": 1, \"name\": \"Product 1\", \"price\": 99.99}, {\"id\": 2, \"name\": \"Product 2\", \"price\": 149.99}]";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, response.getBytes().length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                sendMethodNotAllowed(exchange);
            }
        }
    }

    // Order API Handler
    static class OrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equals("GET")) {
                String response = "[{\"id\": 101, \"customer\": \"John Doe\", \"total\": 500.00}, {\"id\": 102, \"customer\": \"Jane Smith\", \"total\": 750.50}]";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, response.getBytes().length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                sendMethodNotAllowed(exchange);
            }
        }
    }

    // User API Handler
    static class UserHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equals("GET")) {
                String response = "[{\"id\": 1, \"name\": \"Admin User\", \"email\": \"admin@shop.com\", \"role\": \"Admin\"}, {\"id\": 2, \"name\": \"John Doe\", \"email\": \"customer@shop.com\", \"role\": \"Customer\"}]";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, response.getBytes().length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                sendMethodNotAllowed(exchange);
            }
        }
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
