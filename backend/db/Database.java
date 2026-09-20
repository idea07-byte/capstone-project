package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Database {
    private static String URL;
    private static String USER;
    private static String PASSWORD;

    private static final int MAX_POOL_SIZE = 12;
    private static final BlockingQueue<Connection> pool = new ArrayBlockingQueue<>(MAX_POOL_SIZE);
    private static int activeCount = 0;

    static {
        loadProperties();
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found.");
            e.printStackTrace();
        }
    }

    private static void loadProperties() {
        Properties props = new Properties();
        try (InputStream input = Database.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input != null) {
                props.load(input);
                URL = props.getProperty("db.url");
                USER = props.getProperty("db.user");
                PASSWORD = props.getProperty("db.password", "");
            }
        } catch (IOException e) {
            System.err.println("Note: database.properties not loaded from classpath: " + e.getMessage());
        }

        // 12-Factor Cloud & Railway Environment Variable Support
        String envDbUrl = System.getenv("JDBC_DATABASE_URL");
        if (envDbUrl == null || envDbUrl.trim().isEmpty()) envDbUrl = System.getenv("DATABASE_URL");
        if (envDbUrl == null || envDbUrl.trim().isEmpty()) envDbUrl = System.getenv("DATABASE_PUBLIC_URL");
        if (envDbUrl == null || envDbUrl.trim().isEmpty()) envDbUrl = System.getenv("DATABASE_PRIVATE_URL");
        if (envDbUrl == null || envDbUrl.trim().isEmpty()) envDbUrl = System.getenv("DATABASE_URL_UNPOOLED");
        if (envDbUrl == null || envDbUrl.trim().isEmpty()) envDbUrl = System.getenv("DB_URL");

        if (envDbUrl != null && !envDbUrl.trim().isEmpty()) {
            envDbUrl = envDbUrl.trim();
            if (envDbUrl.startsWith("postgres://") || envDbUrl.startsWith("postgresql://")) {
                try {
                    java.net.URI uri = new java.net.URI(envDbUrl);
                    String host = uri.getHost();
                    int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                    String path = uri.getPath();
                    String dbName = (path != null && path.length() > 1) ? path.substring(1) : "postgres";
                    String userInfo = uri.getUserInfo();
                    if (userInfo != null && !userInfo.isEmpty()) {
                        String[] parts = userInfo.split(":", 2);
                        USER = java.net.URLDecoder.decode(parts[0], java.nio.charset.StandardCharsets.UTF_8);
                        if (parts.length > 1) {
                            PASSWORD = java.net.URLDecoder.decode(parts[1], java.nio.charset.StandardCharsets.UTF_8);
                        }
                    }
                    String query = uri.getQuery();
                    URL = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
                    if (query != null && !query.isEmpty()) {
                        URL += "?" + query;
                    } else {
                        String sslModeEnv = System.getenv("PGSSLMODE");
                        if (sslModeEnv == null || sslModeEnv.trim().isEmpty()) sslModeEnv = System.getenv("DB_SSLMODE");
                        if (sslModeEnv != null && !sslModeEnv.trim().isEmpty()) {
                            URL += "?sslmode=" + sslModeEnv.trim() + "&connectTimeout=10&socketTimeout=30";
                        } else if (host != null && (host.contains(".railway.internal") || host.equals("localhost") || host.equals("127.0.0.1") || host.equals("postgres"))) {
                            URL += "?sslmode=prefer&connectTimeout=10&socketTimeout=30";
                        } else {
                            URL += "?sslmode=require&connectTimeout=10&socketTimeout=30";
                        }
                    }
                } catch (Exception ex) {
                    if (!envDbUrl.startsWith("jdbc:")) {
                        URL = "jdbc:" + envDbUrl;
                    } else {
                        URL = envDbUrl;
                    }
                }
            } else if (envDbUrl.startsWith("jdbc:")) {
                URL = envDbUrl;
            } else {
                URL = "jdbc:postgresql://" + envDbUrl;
            }
        }

        // Check individual Railway / Postgres environment variables if URL not set
        if (URL == null || URL.trim().isEmpty()) {
            String pghost = System.getenv("PGHOST");
            if (pghost == null || pghost.trim().isEmpty()) pghost = System.getenv("POSTGRES_HOST");
            if (pghost != null && !pghost.trim().isEmpty()) {
                String pgport = System.getenv("PGPORT");
                if (pgport == null || pgport.trim().isEmpty()) pgport = System.getenv("POSTGRES_PORT");
                if (pgport == null || pgport.trim().isEmpty()) pgport = "5432";
                String pgdb = System.getenv("PGDATABASE");
                if (pgdb == null || pgdb.trim().isEmpty()) pgdb = System.getenv("POSTGRES_DB");
                if (pgdb == null || pgdb.trim().isEmpty()) pgdb = "railway";
                URL = "jdbc:postgresql://" + pghost.trim() + ":" + pgport.trim() + "/" + pgdb.trim() + "?sslmode=prefer&connectTimeout=10&socketTimeout=30";
            }
        }

        String envUser = System.getenv("DB_USER");
        if (envUser == null || envUser.trim().isEmpty()) envUser = System.getenv("PGUSER");
        if (envUser == null || envUser.trim().isEmpty()) envUser = System.getenv("POSTGRES_USER");
        if (envUser != null && !envUser.trim().isEmpty()) {
            USER = envUser.trim();
        }

        String envPassword = System.getenv("DB_PASSWORD");
        if (envPassword == null) envPassword = System.getenv("PGPASSWORD");
        if (envPassword == null) envPassword = System.getenv("POSTGRES_PASSWORD");
        if (envPassword != null) {
            PASSWORD = envPassword.trim();
        }

        if (URL == null || URL.trim().isEmpty()) {
            URL = "jdbc:postgresql://db.wcoivrmtfvlcpwerhjwn.supabase.co:5432/postgres?sslmode=require&connectTimeout=10&socketTimeout=30&tcpKeepAlive=true&loginTimeout=10";
            if (USER == null || USER.trim().isEmpty()) USER = "postgres";
            if (PASSWORD == null) PASSWORD = "Shyam@2007ronaldo";
        }

        System.out.println("Database configured: " + maskUrl(URL) + " (user: " + USER + ")");
    }

    private static String maskUrl(String rawUrl) {
        if (rawUrl == null) return "null";
        return rawUrl.replaceAll(":[^/@:]+@", ":***@");
    }

    private static Connection createRealConnection() throws SQLException {
        SQLException lastEx = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                Connection c = DriverManager.getConnection(URL, USER, PASSWORD);
                if (c != null && !c.isClosed()) {
                    return c;
                }
            } catch (SQLException e) {
                lastEx = e;
                if (attempt < 3) {
                    try { Thread.sleep(300L * attempt); } catch (InterruptedException ignored) {}
                }
            }
        }
        throw lastEx != null ? lastEx : new SQLException("Failed to establish DB connection after 3 attempts");
    }

    public static Connection getConnection() throws SQLException {
        Connection realConn = null;
        while (true) {
            realConn = pool.poll();
            if (realConn == null) {
                synchronized (Database.class) {
                    if (activeCount < MAX_POOL_SIZE) {
                        activeCount++;
                        try {
                            realConn = createRealConnection();
                            break;
                        } catch (SQLException e) {
                            activeCount = Math.max(0, activeCount - 1);
                            throw e;
                        }
                    }
                }
                try {
                    realConn = pool.poll(2, TimeUnit.SECONDS);
                    if (realConn == null) {
                        synchronized (Database.class) {
                            activeCount++;
                        }
                        try {
                            realConn = createRealConnection();
                            break;
                        } catch (SQLException e) {
                            synchronized (Database.class) {
                                activeCount = Math.max(0, activeCount - 1);
                            }
                            throw e;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interrupted waiting for DB connection", e);
                }
            }

            try {
                if (realConn != null && !realConn.isClosed() && realConn.isValid(2)) {
                    break;
                }
            } catch (Exception ignored) {}

            if (realConn != null) {
                try { realConn.close(); } catch (Exception ignored) {}
                synchronized (Database.class) { activeCount = Math.max(0, activeCount - 1); }
            }
        }

        final Connection finalRealConn = realConn;
        return (Connection) Proxy.newProxyInstance(
            Database.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            new InvocationHandler() {
                private boolean closed = false;

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if ("close".equals(method.getName())) {
                        if (!closed) {
                            closed = true;
                            boolean valid = false;
                            try {
                                valid = !finalRealConn.isClosed() && finalRealConn.isValid(1);
                            } catch (Exception ignored) {}
                            if (valid) {
                                if (!pool.offer(finalRealConn)) {
                                    try { finalRealConn.close(); } catch (Exception ignored) {}
                                    synchronized (Database.class) { activeCount = Math.max(0, activeCount - 1); }
                                }
                            } else {
                                try { finalRealConn.close(); } catch (Exception ignored) {}
                                synchronized (Database.class) { activeCount = Math.max(0, activeCount - 1); }
                            }
                        }
                        return null;
                    }
                    if ("isClosed".equals(method.getName())) {
                        return closed || finalRealConn.isClosed();
                    }
                    if (closed) {
                        throw new SQLException("Connection has already been closed.");
                    }
                    try {
                        return method.invoke(finalRealConn, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
            }
        );
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

            boolean hasProducts = false;
            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT count(id) FROM products")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    hasProducts = true;
                }
            } catch (SQLException ignored) {}

            if (hasProducts) {
                int count = 0;
                try (java.sql.ResultSet rs = stmt.executeQuery("SELECT count(id) FROM products")) {
                    if (rs.next()) count = rs.getInt(1);
                } catch (SQLException ignored) {}
                System.out.println("Database already initialized with " + count + " products. Preserving catalog data.");
                return;
            }

            stmt.executeUpdate("DROP TABLE IF EXISTS notifications CASCADE");
            stmt.executeUpdate("DROP TABLE IF EXISTS reviews CASCADE");
            stmt.executeUpdate("DROP TABLE IF EXISTS wishlist_items CASCADE");
            stmt.executeUpdate("DROP TABLE IF EXISTS wishlist CASCADE");
            stmt.executeUpdate("DROP TABLE IF EXISTS cart_items CASCADE");
            stmt.executeUpdate("DROP TABLE IF EXISTS cart CASCADE");
            stmt.executeUpdate("DROP TABLE IF EXISTS order_items CASCADE");
            stmt.executeUpdate("DROP TABLE IF EXISTS orders CASCADE");
            stmt.executeUpdate("DROP TABLE IF EXISTS payments CASCADE");
            stmt.executeUpdate("DROP TABLE IF EXISTS coupons CASCADE");
            stmt.executeUpdate("DROP TABLE IF EXISTS addresses CASCADE");
            stmt.executeUpdate("DROP TABLE IF EXISTS product_images CASCADE");
            stmt.executeUpdate("DROP TABLE IF EXISTS products CASCADE");
            stmt.executeUpdate("DROP TABLE IF EXISTS brands CASCADE");
            stmt.executeUpdate("DROP TABLE IF EXISTS categories CASCADE");
            stmt.executeUpdate("DROP TABLE IF EXISTS vendors CASCADE");
            stmt.executeUpdate("DROP TABLE IF EXISTS users CASCADE");

            stmt.executeUpdate("""
                CREATE TABLE users (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) NOT NULL UNIQUE,
                    phone VARCHAR(20),
                    password VARCHAR(255) NOT NULL,
                    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER' CHECK (role IN ('CUSTOMER', 'VENDOR', 'ADMIN')),
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'BANNED')),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_users_email ON users (email)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_users_role ON users (role)");

            stmt.executeUpdate("""
                CREATE TABLE vendors (
                    id SERIAL PRIMARY KEY,
                    user_id INT NOT NULL UNIQUE,
                    business_name VARCHAR(200) NOT NULL,
                    owner_name VARCHAR(100) NOT NULL,
                    description TEXT,
                    address TEXT,
                    city VARCHAR(100),
                    state VARCHAR(100),
                    pincode VARCHAR(10),
                    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED')),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_vendors_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_vendors_user_id ON vendors (user_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_vendors_status ON vendors (approval_status)");

            stmt.executeUpdate("""
                CREATE TABLE categories (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    description TEXT,
                    image VARCHAR(500),
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            stmt.executeUpdate("""
                CREATE TABLE brands (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    description TEXT,
                    logo VARCHAR(500),
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            stmt.executeUpdate("""
                CREATE TABLE products (
                    id SERIAL PRIMARY KEY,
                    vendor_id INT NOT NULL,
                    category_id INT,
                    brand_id INT,
                    name VARCHAR(255) NOT NULL,
                    description TEXT,
                    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
                    discount DECIMAL(5, 2) DEFAULT 0 CHECK (discount >= 0 AND discount <= 100),
                    stock_quantity INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
                    sku VARCHAR(50),
                    image VARCHAR(500),
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'OUT_OF_STOCK')),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_products_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id) ON DELETE CASCADE,
                    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
                    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands(id) ON DELETE SET NULL
                )
                """);
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_products_name ON products (name)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_products_vendor ON products (vendor_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_products_category ON products (category_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_products_brand ON products (brand_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_products_status ON products (status)");

            stmt.executeUpdate("""
                CREATE TABLE product_images (
                    id SERIAL PRIMARY KEY,
                    product_id INT NOT NULL,
                    image_url VARCHAR(500) NOT NULL,
                    is_primary BOOLEAN DEFAULT FALSE,
                    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
                )
                """);

            stmt.executeUpdate("""
                CREATE TABLE addresses (
                    id SERIAL PRIMARY KEY,
                    customer_id INT NOT NULL,
                    full_name VARCHAR(100) NOT NULL,
                    phone VARCHAR(20),
                    address_line TEXT NOT NULL,
                    city VARCHAR(100) NOT NULL,
                    state VARCHAR(100) NOT NULL,
                    pincode VARCHAR(10) NOT NULL,
                    is_default BOOLEAN DEFAULT FALSE,
                    CONSTRAINT fk_addresses_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);

            stmt.executeUpdate("""
                CREATE TABLE cart (
                    id SERIAL PRIMARY KEY,
                    customer_id INT NOT NULL UNIQUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_cart_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);

            stmt.executeUpdate("""
                CREATE TABLE cart_items (
                    id SERIAL PRIMARY KEY,
                    cart_id INT NOT NULL,
                    product_id INT NOT NULL,
                    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
                    price DECIMAL(10, 2) NOT NULL,
                    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
                    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
                    UNIQUE(cart_id, product_id)
                )
                """);

            stmt.executeUpdate("""
                CREATE TABLE wishlist (
                    id SERIAL PRIMARY KEY,
                    customer_id INT NOT NULL UNIQUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_wishlist_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);

            stmt.executeUpdate("""
                CREATE TABLE wishlist_items (
                    id SERIAL PRIMARY KEY,
                    wishlist_id INT NOT NULL,
                    product_id INT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_wishlist_items_wishlist FOREIGN KEY (wishlist_id) REFERENCES wishlist(id) ON DELETE CASCADE,
                    CONSTRAINT fk_wishlist_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
                    UNIQUE(wishlist_id, product_id)
                )
                """);

            stmt.executeUpdate("""
                CREATE TABLE orders (
                    id SERIAL PRIMARY KEY,
                    customer_id INT NOT NULL,
                    address_id INT,
                    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                    shipping_amount DECIMAL(10, 2) DEFAULT 0.00,
                    discount_amount DECIMAL(10, 2) DEFAULT 0.00,
                    final_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
                    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED')),
                    order_status VARCHAR(30) NOT NULL DEFAULT 'PLACED' CHECK (order_status IN ('PLACED', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED')),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
                    CONSTRAINT fk_orders_address FOREIGN KEY (address_id) REFERENCES addresses(id) ON DELETE SET NULL
                )
                """);
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders (customer_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (order_status)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders (created_at)");

            stmt.executeUpdate("""
                CREATE TABLE order_items (
                    id SERIAL PRIMARY KEY,
                    order_id INT NOT NULL,
                    product_id INT NOT NULL,
                    vendor_id INT NOT NULL,
                    product_name VARCHAR(255) NOT NULL,
                    price DECIMAL(10, 2) NOT NULL,
                    quantity INT NOT NULL CHECK (quantity > 0),
                    subtotal DECIMAL(10, 2) NOT NULL,
                    item_status VARCHAR(30) NOT NULL DEFAULT 'PLACED' CHECK (item_status IN ('PLACED', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
                    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
                    CONSTRAINT fk_order_items_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id) ON DELETE RESTRICT
                )
                """);
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items (product_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_order_items_vendor_id ON order_items (vendor_id)");

            stmt.executeUpdate("""
                CREATE TABLE payments (
                    id SERIAL PRIMARY KEY,
                    order_id INT NOT NULL,
                    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('COD', 'UPI', 'CARD', 'NET_BANKING')),
                    transaction_id VARCHAR(100),
                    amount DECIMAL(10, 2) NOT NULL,
                    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED')),
                    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
                )
                """);

            stmt.executeUpdate("""
                CREATE TABLE reviews (
                    id SERIAL PRIMARY KEY,
                    product_id INT NOT NULL,
                    customer_id INT NOT NULL,
                    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
                    comment TEXT,
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'HIDDEN', 'DELETED')),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
                    CONSTRAINT fk_reviews_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_reviews_product ON reviews (product_id)");

            stmt.executeUpdate("""
                CREATE TABLE coupons (
                    id SERIAL PRIMARY KEY,
                    code VARCHAR(50) NOT NULL UNIQUE,
                    discount_type VARCHAR(20) NOT NULL CHECK (discount_type IN ('PERCENTAGE', 'FIXED')),
                    discount_value DECIMAL(10, 2) NOT NULL,
                    minimum_amount DECIMAL(10, 2) DEFAULT 0,
                    maximum_discount DECIMAL(10, 2),
                    start_date TIMESTAMP,
                    expiry_date TIMESTAMP,
                    usage_limit INT DEFAULT 0,
                    used_count INT DEFAULT 0,
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

            stmt.executeUpdate("""
                CREATE TABLE notifications (
                    id SERIAL PRIMARY KEY,
                    user_id INT NOT NULL,
                    title VARCHAR(200) NOT NULL,
                    message TEXT NOT NULL,
                    type VARCHAR(50),
                    is_read BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications (user_id)");

            seedData(stmt);

            System.out.println("Database initialized successfully with full marketplace schema.");
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void seedData(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
            INSERT INTO users (id, name, email, phone, password, role, status) VALUES
            (1, 'Admin User', 'admin@buyit.com', '9999900000', 'Admin@123', 'ADMIN', 'ACTIVE'),
            (2, 'Rahul Sharma', 'vendor1@buyit.com', '9876543210', 'Vendor@123', 'VENDOR', 'ACTIVE'),
            (3, 'Priya Patel', 'vendor2@buyit.com', '9876543211', 'Vendor@123', 'VENDOR', 'ACTIVE'),
            (4, 'Amit Kumar', 'vendor3@buyit.com', '9876543212', 'Vendor@123', 'VENDOR', 'ACTIVE'),
            (5, 'Sneha Reddy', 'vendor4@buyit.com', '9876543213', 'Vendor@123', 'VENDOR', 'ACTIVE'),
            (6, 'Vikram Singh', 'vendor5@buyit.com', '9876543214', 'Vendor@123', 'VENDOR', 'ACTIVE'),
            (7, 'Customer One', 'customer@buyit.com', '8765432109', 'Customer@123', 'CUSTOMER', 'ACTIVE'),
            (8, 'Anita Desai', 'customer2@buyit.com', '8765432108', 'Customer@123', 'CUSTOMER', 'ACTIVE'),
            (9, 'Ravi Verma', 'customer3@buyit.com', '8765432107', 'Customer@123', 'CUSTOMER', 'ACTIVE'),
            (10, 'Meena Gupta', 'customer4@buyit.com', '8765432106', 'Customer@123', 'CUSTOMER', 'ACTIVE'),
            (11, 'Arjun Nair', 'customer5@buyit.com', '8765432105', 'Customer@123', 'CUSTOMER', 'ACTIVE'),
            (12, 'Kavita Joshi', 'customer6@buyit.com', '8765432104', 'Customer@123', 'CUSTOMER', 'ACTIVE'),
            (13, 'Sanjay Mehta', 'customer7@buyit.com', '8765432103', 'Customer@123', 'CUSTOMER', 'ACTIVE'),
            (14, 'Pooja Rao', 'customer8@buyit.com', '8765432102', 'Customer@123', 'CUSTOMER', 'ACTIVE'),
            (15, 'Deepak Tiwari', 'customer9@buyit.com', '8765432101', 'Customer@123', 'CUSTOMER', 'ACTIVE'),
            (16, 'Neha Agarwal', 'customer10@buyit.com', '8765432100', 'Customer@123', 'CUSTOMER', 'ACTIVE')
            ON CONFLICT (id) DO NOTHING
            """);

        stmt.executeUpdate("ALTER SEQUENCE users_id_seq RESTART WITH 17");

        stmt.executeUpdate("""
            INSERT INTO vendors (id, user_id, business_name, owner_name, description, address, city, state, pincode, approval_status) VALUES
            (1, 2, 'TechHub Electronics', 'Rahul Sharma', 'Premium electronics and gadgets store', '12 MG Road', 'Mumbai', 'Maharashtra', '400001', 'APPROVED'),
            (2, 3, 'Fashion World', 'Priya Patel', 'Trendy fashion clothing and accessories', '45 Park Street', 'Kolkata', 'West Bengal', '700016', 'APPROVED'),
            (3, 4, 'Home Essentials', 'Amit Kumar', 'Home and kitchen essentials at best prices', '78 Civil Lines', 'Delhi', 'Delhi', '110001', 'APPROVED'),
            (4, 5, 'Books & Beyond', 'Sneha Reddy', 'Online bookstore with wide collection', '23 Anna Salai', 'Chennai', 'Tamil Nadu', '600002', 'APPROVED'),
            (5, 6, 'Sports Arena', 'Vikram Singh', 'Sports equipment and fitness gear', '56 Brigade Road', 'Bangalore', 'Karnataka', '560001', 'APPROVED')
            ON CONFLICT (id) DO NOTHING
            """);

        stmt.executeUpdate("ALTER SEQUENCE vendors_id_seq RESTART WITH 6");

        stmt.executeUpdate("""
            INSERT INTO categories (id, name, description, status) VALUES
            (1, 'Electronics', 'Audio, gadgets, and consumer electronics', 'ACTIVE'),
            (2, 'Mobiles', 'Smartphones, cases, and mobile gear', 'ACTIVE'),
            (3, 'Laptops', 'Notebooks, ultrabooks, and PC accessories', 'ACTIVE'),
            (4, 'Headphones', 'Over-ear, in-ear, and wireless earbuds', 'ACTIVE'),
            (5, 'Clothing', 'Men and women apparel, shirts, and jackets', 'ACTIVE'),
            (6, 'Shoes', 'Sneakers, formal shoes, and boots', 'ACTIVE'),
            (7, 'Watches', 'Analog, digital, and smart watches', 'ACTIVE'),
            (8, 'Bags', 'Backpacks, handbags, travel duffels', 'ACTIVE'),
            (9, 'Home & Kitchen', 'Cookware, dining, kitchen essentials', 'ACTIVE'),
            (10, 'Beauty', 'Skincare, perfumes, and cosmetics', 'ACTIVE'),
            (11, 'Books', 'Fiction, non-fiction, textbooks, and bestsellers', 'ACTIVE'),
            (12, 'Toys', 'Action figures, board games, and puzzles', 'ACTIVE'),
            (13, 'Sports', 'Sporting goods, fitness equipment, activewear', 'ACTIVE'),
            (14, 'Grocery', 'Pantry essentials, organic foods, snacks', 'ACTIVE'),
            (15, 'Appliances', 'Kitchen and home electrical appliances', 'ACTIVE')
            ON CONFLICT (id) DO NOTHING
            """);

        stmt.executeUpdate("ALTER SEQUENCE categories_id_seq RESTART WITH 16");

        stmt.executeUpdate("""
            INSERT INTO brands (id, name, description, status) VALUES
            (1, 'Samsung', 'Leading electronics brand', 'ACTIVE'),
            (2, 'Apple', 'Premium technology brand', 'ACTIVE'),
            (3, 'Nike', 'Global sports brand', 'ACTIVE'),
            (4, 'Adidas', 'Sports and lifestyle brand', 'ACTIVE'),
            (5, 'Prestige', 'Home and kitchen brand', 'ACTIVE'),
            (6, 'HP', 'Computing solutions', 'ACTIVE'),
            (7, 'Lenovo', 'Personal computers and accessories', 'ACTIVE'),
            (8, 'Phillips', 'Consumer electronics', 'ACTIVE'),
            (9, 'Lakme', 'Beauty and cosmetics', 'ACTIVE'),
            (10, 'Penguin Books', 'Publishing house', 'ACTIVE')
            ON CONFLICT (id) DO NOTHING
            """);

        stmt.executeUpdate("ALTER SEQUENCE brands_id_seq RESTART WITH 11");

        List<util.Seed300Products.ProductItem> products = util.Seed300Products.get300Products();
        String insertProductSql = "INSERT INTO products (id, vendor_id, category_id, brand_id, name, description, price, discount, stock_quantity, sku, image, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')";
        String insertImgSql = "INSERT INTO product_images (product_id, image_url, is_primary) VALUES (?, ?, ?)";

        try (PreparedStatement pStmt = stmt.getConnection().prepareStatement(insertProductSql);
             PreparedStatement imgStmt = stmt.getConnection().prepareStatement(insertImgSql)) {
            int id = 1;
            for (util.Seed300Products.ProductItem item : products) {
                pStmt.setInt(1, id);
                pStmt.setInt(2, item.vendorId);
                pStmt.setInt(3, item.categoryId);
                pStmt.setInt(4, item.brandId);
                pStmt.setString(5, item.name);
                pStmt.setString(6, item.description);
                pStmt.setDouble(7, item.price);
                pStmt.setDouble(8, item.discount);
                pStmt.setInt(9, item.stock);
                pStmt.setString(10, item.sku);
                pStmt.setString(11, item.primaryImage);
                pStmt.addBatch();

                boolean isFirst = true;
                for (String img : item.galleryImages) {
                    imgStmt.setInt(1, id);
                    imgStmt.setString(2, img);
                    imgStmt.setBoolean(3, isFirst);
                    imgStmt.addBatch();
                    isFirst = false;
                }
                id++;
            }
            pStmt.executeBatch();
            imgStmt.executeBatch();
        }

        stmt.executeUpdate("ALTER SEQUENCE products_id_seq RESTART WITH " + (products.size() + 1));
        stmt.executeUpdate("ALTER SEQUENCE product_images_id_seq RESTART WITH " + (products.size() + 1));

        stmt.executeUpdate("""
            INSERT INTO coupons (code, discount_type, discount_value, minimum_amount, maximum_discount, start_date, expiry_date, usage_limit, status) VALUES
            ('WELCOME10', 'PERCENTAGE', 10.00, 500.00, 200.00, '2025-01-01', '2026-12-31', 1000, 'ACTIVE'),
            ('FLAT200', 'FIXED', 200.00, 2000.00, 200.00, '2025-01-01', '2026-12-31', 500, 'ACTIVE'),
            ('SUMMER15', 'PERCENTAGE', 15.00, 1000.00, 500.00, '2025-01-01', '2026-12-31', 300, 'ACTIVE')
            ON CONFLICT (code) DO NOTHING
            """);

        stmt.executeUpdate("""
            INSERT INTO cart (id, customer_id) VALUES
            (1, 7),
            (2, 1),
            (3, 8)
            ON CONFLICT (id) DO NOTHING
            """);

        stmt.executeUpdate("ALTER SEQUENCE cart_id_seq RESTART WITH 4");

        stmt.executeUpdate("""
            INSERT INTO cart_items (id, cart_id, product_id, quantity, price) VALUES
            (1, 1, 2, 1, 151905.00),
            (2, 1, 6, 1, 12595.50),
            (3, 1, 22, 1, 23655.00),
            (4, 1, 33, 1, 8095.50),
            (5, 1, 14, 2, 360.00),
            (6, 1, 9, 1, 3499.00),
            (7, 1, 23, 2, 674.25),
            (8, 1, 27, 1, 4099.18),
            (9, 2, 1, 1, 116999.10),
            (10, 2, 11, 1, 8495.75),
            (11, 2, 31, 1, 82708.00),
            (12, 2, 34, 1, 6599.12)
            ON CONFLICT (id) DO NOTHING
            """);

        stmt.executeUpdate("ALTER SEQUENCE cart_items_id_seq RESTART WITH 13");

        stmt.executeUpdate("""
            INSERT INTO reviews (product_id, customer_id, rating, comment, status) VALUES
            (1, 7, 5, 'Superb phone! The camera quality is breathtaking and battery life easily lasts two full days.', 'ACTIVE'),
            (2, 8, 5, 'The titanium finish feels amazing and the 5x optical zoom is stunning!', 'ACTIVE'),
            (6, 9, 5, 'Most comfortable sneakers I have ever worn for daily walking and workouts.', 'ACTIVE'),
            (11, 7, 4, 'Air fryer makes snacks super crispy with barely any oil! Highly recommend.', 'ACTIVE'),
            (14, 10, 5, 'Life-changing book. The 1% better every day concept is immensely powerful.', 'ACTIVE'),
            (22, 7, 5, 'Noise cancellation on these AirPods Pro is magical on daily commutes.', 'ACTIVE'),
            (33, 8, 5, 'The best ergonomic mouse ever built. The MagSpeed scroll wheel is legendary.', 'ACTIVE')
            ON CONFLICT (id) DO NOTHING
            """);

        System.out.println("Seed data inserted successfully.");
    }
}

