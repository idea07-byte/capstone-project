package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Database {
    private static String URL;
    private static String USER;
    private static String PASSWORD;

    static {
        loadProperties();
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found. Add postgresql jar to classpath.");
            e.printStackTrace();
        }
    }

    private static void loadProperties() {
        Properties props = new Properties();
        try (InputStream input = Database.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) {
                System.err.println("Unable to find database.properties. Using defaults.");
                URL = "jdbc:postgresql://localhost:5432/postgres?sslmode=require";
                USER = "postgres";
                PASSWORD = "";
                return;
            }
            props.load(input);
            URL = props.getProperty("db.url");
            USER = props.getProperty("db.user");
            PASSWORD = props.getProperty("db.password", "");
        } catch (IOException e) {
            System.err.println("Failed to load database.properties: " + e.getMessage());
            URL = "jdbc:postgresql://localhost:5432/postgres?sslmode=require";
            USER = "postgres";
            PASSWORD = "";
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void close(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                System.err.println("Failed to close resource: " + e.getMessage());
            }
        }
    }

    public static void initialize() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL,
                    role VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER' CHECK (role IN ('CUSTOMER', 'ADMIN')),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_users_email ON users (email)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_users_role ON users (role)");

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS products (
                    id INT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
                    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_products_name ON products (name)");

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS orders (
                    id INT PRIMARY KEY,
                    customer_id INT NOT NULL,
                    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                    status VARCHAR(50) NOT NULL DEFAULT 'Pending',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders (customer_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders (created_at)");

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS order_items (
                    id INT PRIMARY KEY,
                    order_id INT NOT NULL,
                    product_id INT NOT NULL,
                    product_name VARCHAR(255) NOT NULL,
                    unit_price DECIMAL(10, 2) NOT NULL,
                    quantity INT NOT NULL CHECK (quantity > 0),
                    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
                )
                """);
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items (product_id)");

            seedData(stmt);

            System.out.println("Database initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void seedData(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
            INSERT INTO users (id, name, email, password, role) VALUES
            (900, 'Admin', 'admin@example.com', 'adminpass', 'ADMIN')
            ON CONFLICT (id) DO NOTHING
            """);

        stmt.executeUpdate("""
            INSERT INTO users (id, name, email, password, role) VALUES
            (1, 'Asha', 'asha@example.com', 'pass1234', 'CUSTOMER')
            ON CONFLICT (id) DO NOTHING
            """);

        stmt.executeUpdate("""
            INSERT INTO products (id, name, price, quantity) VALUES
            (101, 'Wireless Mouse', 799.00, 12),
            (102, 'USB-C Cable', 299.00, 25),
            (103, 'Mechanical Keyboard', 2499.00, 8)
            ON CONFLICT (id) DO NOTHING
            """);

        stmt.executeUpdate("""
            INSERT INTO orders (id, customer_id, total_amount, status) VALUES
            (1, 1, 0.00, 'Pending')
            ON CONFLICT (id) DO NOTHING
            """);

        System.out.println("Seed data checked/inserted.");
    }
}
