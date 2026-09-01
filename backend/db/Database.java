package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
            if (input == null) {
                System.err.println("Unable to find database.properties. Using defaults.");
                URL = "jdbc:postgresql://localhost:5432/buyit_marketplace?sslmode=require";
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
            URL = "jdbc:postgresql://localhost:5432/buyit_marketplace?sslmode=require";
            USER = "postgres";
            PASSWORD = "";
        }
    }

    private static Connection createRealConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
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
                            activeCount--;
                            throw e;
                        }
                    }
                }
                try {
                    realConn = pool.poll(3, TimeUnit.SECONDS);
                    if (realConn == null) {
                        return createRealConnection();
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
                synchronized (Database.class) { activeCount--; }
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
                            if (!finalRealConn.isClosed()) {
                                if (!pool.offer(finalRealConn)) {
                                    try { finalRealConn.close(); } catch (Exception ignored) {}
                                    synchronized (Database.class) { activeCount--; }
                                }
                            } else {
                                synchronized (Database.class) { activeCount--; }
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
            (1, 'Electronics', 'Gadgets, phones, laptops and accessories', 'ACTIVE'),
            (2, 'Fashion', 'Clothing, footwear and accessories', 'ACTIVE'),
            (3, 'Home & Kitchen', 'Furniture, appliances and kitchen items', 'ACTIVE'),
            (4, 'Books', 'Fiction, non-fiction, academic books', 'ACTIVE'),
            (5, 'Sports', 'Sports equipment and fitness gear', 'ACTIVE'),
            (6, 'Beauty', 'Skincare, makeup and personal care', 'ACTIVE'),
            (7, 'Grocery', 'Food items and daily essentials', 'ACTIVE'),
            (8, 'Toys', 'Toys and games for all ages', 'ACTIVE'),
            (9, 'Mobile Phones', 'Smartphones and accessories', 'ACTIVE'),
            (10, 'Computers', 'Laptops, desktops and peripherals', 'ACTIVE')
            ON CONFLICT (id) DO NOTHING
            """);

        stmt.executeUpdate("ALTER SEQUENCE categories_id_seq RESTART WITH 11");

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

        stmt.executeUpdate("""
            INSERT INTO products (id, vendor_id, category_id, brand_id, name, description, price, discount, stock_quantity, sku, image, status) VALUES
            (1, 1, 1, 1, 'Samsung Galaxy S24 Ultra', 'Premium flagship smartphone with S Pen and Galaxy AI features, 200MP camera and titanium build', 129999.00, 10, 25, 'SAMS24U-256', 'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (2, 1, 9, 2, 'iPhone 15 Pro Max', 'Apple flagship with A17 Pro chip, titanium frame, Action button, and 5x optical zoom camera', 159900.00, 5, 20, 'APPL15PM-256', 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (3, 1, 10, 6, 'HP Pavilion 15 Laptop', '15.6 inch FHD IPS display, Intel Core i7 13th Gen, 16GB RAM, 512GB NVMe SSD, Backlit keyboard', 72999.00, 15, 15, 'HP-PAV15-I7', 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (4, 1, 10, 7, 'Lenovo IdeaPad Slim 5', '14 inch 2.8K OLED display, AMD Ryzen 7 7730U, 16GB LPDDR5, 512GB SSD, Ultra-lightweight aluminium chassis', 64999.00, 12, 18, 'LEN-IS5-R7', 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (5, 1, 1, 8, 'Philips TAH7508 Headphones', 'Over-ear wireless headphones with Hybrid Active Noise Cancellation, 60-hour battery life and Hi-Res Audio', 3999.00, 20, 50, 'PHI-TAH7508', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (6, 2, 2, 3, 'Nike Air Max 270', 'Men lifestyle running sneakers featuring Nike biggest heel Air unit yet for a super-soft ride', 13995.00, 10, 35, 'NIKE-AM270-BLK', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (7, 2, 2, 4, 'Adidas Ultraboost Light', 'Next-generation running shoes engineered with Light BOOST cushioning for epic energy return', 16999.00, 8, 30, 'ADI-UBL-LT', 'https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (8, 2, 2, 3, 'Nike Dri-FIT T-Shirt', 'Breathable moisture-wicking athletic short-sleeve training shirt for workouts and casual wear', 2499.00, 15, 100, 'NIKE-DRF-TEE', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (9, 2, 2, 4, 'Adidas Classic Backpack', 'Durable everyday laptop backpack with padded shoulder straps and water-resistant coated bottom', 3499.00, 0, 45, 'ADI-BP-CL', 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (10, 3, 3, 5, 'Prestige Iris Mixer Grinder', '750W heavy-duty motor mixer grinder with 3 stainless steel jars and multi-utility juice jar', 3495.00, 20, 40, 'PRE-IRIS-750', 'https://images.unsplash.com/photo-1570222094114-d054a817e56b?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (11, 3, 3, 8, 'Philips Air Fryer 4.1L', 'Rapid Air technology for delicious crispy fries with up to 90% less fat and touch control panel', 9995.00, 15, 25, 'PHI-AF41', 'https://images.unsplash.com/photo-1584990347449-a2a51f33f679?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (12, 3, 3, 5, 'Prestige Induction Cooktop', '1900W electromagnetic induction cooktop with Indian menu preset and automatic voltage regulator', 2799.00, 10, 55, 'PRE-IC-1900', 'https://images.unsplash.com/photo-1584568694244-14fbdf83bd30?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (13, 3, 3, 8, 'Philips Steam Iron 2200W', 'Powerful steam boost iron with non-stick ceramic soleplate and continuous anti-calc system', 1899.00, 12, 35, 'PHI-SI-2200', 'https://images.unsplash.com/photo-1540544093-b0880061e1a5?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (14, 4, 4, 10, 'Atomic Habits by James Clear', 'An Easy & Proven Way to Build Good Habits & Break Bad Ones. International million-copy bestseller', 450.00, 20, 200, 'PB-ATOM-HAB', 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (15, 4, 4, 10, 'The Psychology of Money', 'Timeless lessons on wealth, greed, and happiness by Morgan Housel. Must-read personal finance classic', 399.00, 15, 180, 'PB-PSY-MON', 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (16, 4, 4, 10, 'Rich Dad Poor Dad', 'What the rich teach their kids about money that the poor and middle class do not! by Robert Kiyosaki', 350.00, 10, 150, 'PB-RICH-DAD', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (17, 4, 4, 10, 'Sapiens: A Brief History of Humankind', 'A groundbreaking narrative of human history and civilization by Yuval Noah Harari', 500.00, 12, 120, 'PB-SAPIENS', 'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (18, 5, 5, 3, 'Nike Strike Training Football', 'High-visibility match and training football size 5 with textured casing for consistent touch', 1299.00, 0, 60, 'NIKE-FB-S5', 'https://images.unsplash.com/photo-1614632537423-1e6c2e7e0aab?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (19, 5, 5, 4, 'Adidas Yoga Mat Pro', '6mm extra-thick textured non-slip exercise mat with carrying strap for yoga, pilates and fitness', 2499.00, 15, 40, 'ADI-YM-NS', 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (20, 5, 5, 3, 'Nike Gym Duffel Bag', 'Spacious water-resistant sports training duffel bag with dedicated shoe compartment', 4499.00, 10, 25, 'NIKE-GYM-DB', 'https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (21, 1, 10, 1, 'Samsung Galaxy Tab S9', '11 inch Dynamic AMOLED 2X 120Hz display, Snapdragon 8 Gen 2, included S Pen and IP68 water resistance', 74999.00, 8, 15, 'SAMS-TAB-S9', 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (22, 1, 9, 2, 'AirPods Pro 2nd Gen', 'Apple flagship earbuds with Active Noise Cancellation, Adaptive Audio, Transparency mode, USB-C MagSafe case', 24900.00, 5, 40, 'APPL-APP2', 'https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (23, 2, 6, 9, 'Lakme Absolute Skin Dew Serum', 'Deeply hydrating facial serum infused with Hyaluronic Acid and Vitamin E for instant glass skin glow', 899.00, 25, 80, 'LAK-ASD-01', 'https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (24, 2, 6, 9, 'Lakme 9 to 5 Primer + Matte', 'Flawless coverage liquid foundation with built-in primer for a 16-hour shine-free matte finish', 799.00, 20, 70, 'LAK-9T5-PM', 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (25, 3, 7, 5, 'Prestige Stainless Steel Bottle', '1000ml double-wall vacuum insulated flask bottle keeps drinks cold 24h or hot 18h', 899.00, 0, 90, 'PRE-SS-WB', 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (26, 5, 8, 4, 'Adidas Sports Squeeze Bottle', '750ml BPA-free ergonomically shaped sports water bottle with fast-flow leakproof valve', 599.00, 10, 65, 'ADI-SWB-750', 'https://images.unsplash.com/photo-1523362628745-0c100150b504?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (27, 3, 3, 5, 'Prestige Hard Anodised Cookware', '5 piece premium non-toxic cookware set with induction bottoms and heat-resistant silicone handles', 4999.00, 18, 20, 'PRE-HA-SET5', 'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (28, 1, 1, 8, 'Philips Hue Smart Bulb', '16 Million colors smart LED bulb compatible with Alexa, Google Home and Apple HomeKit via Wi-Fi', 2499.00, 15, 45, 'PHI-HUE-B22', 'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (29, 4, 4, 10, 'Think and Grow Rich', 'Napoleon Hill legendary classic blueprint to success, wealth creation and personal achievement', 299.00, 0, 160, 'PB-TGR-NH', 'https://images.unsplash.com/photo-1592841200221-a6898f307baa?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (30, 2, 2, 3, 'Nike Dri-FIT Running Shorts', 'Lightweight stretch performance running shorts with internal brief liner and secure zip pocket', 1999.00, 10, 55, 'NIKE-RS-DF', 'https://images.unsplash.com/photo-1591195853828-11db59a44f6b?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (31, 1, 1, 2, 'Apple Watch Ultra 2', 'Rugged 49mm titanium case, precision dual-frequency GPS, 3000 nits display, and 36-hour battery life', 89900.00, 8, 12, 'APPL-AWU2', 'https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (32, 1, 1, 8, 'Sony WH-1000XM5 Headphones', 'Industry-leading noise canceling wireless headphones with 8 microphones, LDAC audio, and 30h battery', 29990.00, 15, 25, 'SONY-WH5', 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (33, 1, 10, 6, 'Logitech MX Master 3S Mouse', 'Ultra-fast MagSpeed scrolling, 8K DPI any-surface tracking, quiet clicks, and ergonomic palm rest', 8995.00, 10, 40, 'LOGI-MX3S', 'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (34, 1, 10, 7, 'Keychron K2 Mechanical Keyboard', 'Wireless 75% layout mechanical keyboard with hot-swappable Gateron switches and RGB backlight', 7499.00, 12, 30, 'KEY-K2-RGB', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (35, 1, 1, 1, 'Samsung 55-inch Crystal 4K UHD TV', 'Crystal Processor 4K, HDR10+, PurColor technology, Motion Xcelerator and Q-Symphony smart TV', 44990.00, 22, 10, 'SAMS-TV-55', 'https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (36, 1, 1, 1, 'Sony Alpha A7 IV Mirrorless Camera', '33MP full-frame Exmor R sensor, 4K 60p video, Real-time Eye AF, and 5-axis optical image stabilization', 214990.00, 5, 8, 'SONY-A7M4', 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (37, 1, 10, 2, 'Apple iPad Air M2 11-inch', 'Stunning Liquid Retina display, Apple M2 chip, 12MP Ultra Wide front camera with Center Stage, 128GB', 59900.00, 6, 15, 'APPL-IPAD-M2', 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (38, 3, 3, 5, 'DeLonghi Espresso Coffee Maker', '15-bar professional pressure pump espresso and cappuccino maker with manual milk frothing wand', 18990.00, 15, 14, 'DEL-ESP-15', 'https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (39, 2, 2, 3, 'Nike Pegasus 40 Running Shoes', 'Engineered mesh upper with responsive React foam and dual Zoom Air units for daily distance runs', 10495.00, 18, 30, 'NIKE-PEG40', 'https://images.unsplash.com/photo-1552346154-21d32810aba3?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (40, 3, 3, 8, 'Dyson V12 Cordless Vacuum', 'Laser Slim Fluffy cleaner head illuminates microscopic dust with powerful 150AW suction power', 47900.00, 10, 12, 'DYS-V12-SL', 'https://images.unsplash.com/photo-1558317374-067fb5f30001?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (41, 2, 2, 4, 'Classic Polarized Sunglasses', 'Timeless retro sunglasses with UV400 polarized scratch-resistant lenses and lightweight frame', 1899.00, 25, 60, 'POL-SUN-CL', 'https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (42, 3, 3, 5, 'Smart Temperature Travel Tumbler', 'LED touch temperature display, double-wall stainless steel, 500ml leak-proof tea and coffee flask', 2199.00, 10, 50, 'SMART-TUMB-5', 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (43, 5, 5, 3, 'Resistance Workout Bands Set', 'Heavy-duty 5-tube resistance exercise bands with foam handles, door anchor and ankle straps', 1299.00, 20, 75, 'RES-BAND-5', 'https://images.unsplash.com/photo-1598289431512-b97b0917affc?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (44, 2, 6, 9, 'Lakme Vitamin C Glow Cream', 'Nourishing lightweight day cream with 99% pure Vitamin C complex for intense hydration and radiance', 649.00, 15, 90, 'LAK-VITC-CRM', 'https://images.unsplash.com/photo-1608248597359-bb583597d396?w=600&auto=format&fit=crop&q=80', 'ACTIVE'),
            (45, 4, 4, 10, 'Deep Work by Cal Newport', 'Rules for Focused Success in a Distracted World. Transformative productivity and focus guide', 420.00, 10, 140, 'PB-DEEP-WORK', 'https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=600&auto=format&fit=crop&q=80', 'ACTIVE')
            ON CONFLICT (id) DO NOTHING
            """);

        stmt.executeUpdate("ALTER SEQUENCE products_id_seq RESTART WITH 46");

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

