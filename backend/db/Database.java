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
            (1, 1, 1, 1, 'Samsung Galaxy S24 Ultra', 'Premium flagship smartphone with S Pen and AI features', 129999.00, 10, 25, 'SAMS24U-256', 'samsung-s24.jpg', 'ACTIVE'),
            (2, 1, 1, 2, 'iPhone 15 Pro Max', 'Apple flagship with A17 Pro chip and titanium design', 159900.00, 5, 20, 'APPL15PM-256', 'iphone15.jpg', 'ACTIVE'),
            (3, 1, 10, 6, 'HP Pavilion 15 Laptop', '15.6 inch FHD, Intel i7, 16GB RAM, 512GB SSD', 72999.00, 15, 15, 'HP-PAV15-I7', 'hp-pavilion.jpg', 'ACTIVE'),
            (4, 1, 10, 7, 'Lenovo IdeaPad Slim 5', '14 inch FHD, AMD Ryzen 7, 16GB RAM, 512GB SSD', 64999.00, 12, 18, 'LEN-IS5-R7', 'lenovo-ideapad.jpg', 'ACTIVE'),
            (5, 1, 1, 8, 'Philips TAH7508 Headphones', 'Over-ear wireless headphones with ANC', 3999.00, 20, 50, 'PHI-TAH7508', 'philips-headphones.jpg', 'ACTIVE'),
            (6, 2, 2, 3, 'Nike Air Max 270', 'Men running shoes with Max Air cushioning', 13995.00, 10, 35, 'NIKE-AM270-BLK', 'nike-airmax.jpg', 'ACTIVE'),
            (7, 2, 2, 4, 'Adidas Ultraboost Light', 'Premium running shoes with Boost technology', 16999.00, 8, 30, 'ADI-UBL-LT', 'adidas-ultraboost.jpg', 'ACTIVE'),
            (8, 2, 2, 3, 'Nike Dri-FIT T-Shirt', 'Moisture-wicking athletic t-shirt for men', 2499.00, 15, 100, 'NIKE-DRF-TEE', 'nike-drifit.jpg', 'ACTIVE'),
            (9, 2, 2, 4, 'Adidas Classic Backpack', 'Durable everyday backpack for travel and work', 3499.00, 0, 45, 'ADI-BP-CL', 'adidas-backpack.jpg', 'ACTIVE'),
            (10, 3, 3, 5, 'Prestige Iris Mixer Grinder', '750W mixer grinder with 3 stainless steel jars', 3495.00, 20, 40, 'PRE-IRIS-750', 'prestige-mixer.jpg', 'ACTIVE'),
            (11, 3, 3, 8, 'Philips Air Fryer 4.1L', 'Rapid Air technology for healthy cooking', 9995.00, 15, 25, 'PHI-AF41', 'philips-airfryer.jpg', 'ACTIVE'),
            (12, 3, 3, 5, 'Prestige Induction Cooktop', '1900W cooktop with Indian menu option', 2799.00, 10, 55, 'PRE-IC-1900', 'prestige-induction.jpg', 'ACTIVE'),
            (13, 3, 3, 8, 'Philips Steam Iron 2200W', 'Powerful steam iron with non-stick soleplate', 1899.00, 12, 35, 'PHI-SI-2200', 'philips-iron.jpg', 'ACTIVE'),
            (14, 4, 4, 10, 'Atomic Habits by James Clear', 'An Easy & Proven Way to Build Good Habits', 450.00, 20, 200, 'PB-ATOM-HAB', 'atomic-habits.jpg', 'ACTIVE'),
            (15, 4, 4, 10, 'The Psychology of Money', 'Timeless lessons on wealth and happiness', 399.00, 15, 180, 'PB-PSY-MON', 'psych-money.jpg', 'ACTIVE'),
            (16, 4, 4, 10, 'Rich Dad Poor Dad', 'What the rich teach their kids about money', 350.00, 10, 150, 'PB-RICH-DAD', 'rich-dad.jpg', 'ACTIVE'),
            (17, 4, 4, 10, 'Sapiens: A Brief History of Humankind', 'A bold and thought-provoking narrative', 500.00, 12, 120, 'PB-SAPIENS', 'sapiens.jpg', 'ACTIVE'),
            (18, 5, 5, 3, 'Nike Football', 'Strike Training Football Size 5', 1299.00, 0, 60, 'NIKE-FB-S5', 'nike-football.jpg', 'ACTIVE'),
            (19, 5, 5, 4, 'Adidas Yoga Mat', 'Non-slip exercise mat for yoga and fitness', 2499.00, 15, 40, 'ADI-YM-NS', 'adidas-yogamat.jpg', 'ACTIVE'),
            (20, 5, 5, 3, 'Nike Gym Duffel Bag', 'Large sports duffel bag with multiple compartments', 4499.00, 10, 25, 'NIKE-GYM-DB', 'nike-gym-bag.jpg', 'ACTIVE'),
            (21, 1, 1, 1, 'Samsung Galaxy Tab S9', '11 inch AMOLED display, Snapdragon 8 Gen 2', 74999.00, 8, 15, 'SAMS-TAB-S9', 'samsung-tab.jpg', 'ACTIVE'),
            (22, 1, 9, 2, 'AirPods Pro 2nd Gen', 'Active Noise Cancellation with USB-C charging', 24900.00, 5, 40, 'APPL-APP2', 'airpods-pro.jpg', 'ACTIVE'),
            (23, 2, 6, 9, 'Lakme Absolute Skin Dew Serum', 'Hydrating serum with hyaluronic acid', 899.00, 25, 80, 'LAK-ASD-01', 'lakme-serum.jpg', 'ACTIVE'),
            (24, 2, 6, 9, 'Lakme 9 to 5 Primer + Matte', 'Long-lasting foundation with built-in primer', 799.00, 20, 70, 'LAK-9T5-PM', 'lakme-foundation.jpg', 'ACTIVE'),
            (25, 3, 7, 5, 'Prestige Stainless Steel Water Bottle', '1 litre insulated bottle, keeps water cold/hot', 899.00, 0, 90, 'PRE-SS-WB', 'prestige-bottle.jpg', 'ACTIVE'),
            (26, 5, 8, 4, 'Adidas Sports Water Bottle', '750ml BPA-free squeeze bottle for sports', 599.00, 10, 65, 'ADI-SWB-750', 'adidas-bottle.jpg', 'ACTIVE'),
            (27, 3, 3, 5, 'Prestige Hard Anodised Cookware Set', '5 piece cookware set for modern kitchens', 4999.00, 18, 20, 'PRE-HA-SET5', 'prestige-cookset.jpg', 'ACTIVE'),
            (28, 1, 1, 8, 'Philips Hue Smart Bulb', 'WiFi enabled color changing smart LED bulb', 2499.00, 15, 45, 'PHI-HUE-B22', 'philips-hue.jpg', 'ACTIVE'),
            (29, 4, 4, 10, 'Think and Grow Rich', 'Classic motivational book by Napoleon Hill', 299.00, 0, 160, 'PB-TGR-NH', 'think-grow.jpg', 'ACTIVE'),
            (30, 2, 2, 3, 'Nike Running Shorts', 'Dri-FIT running shorts with built-in liner', 1999.00, 10, 55, 'NIKE-RS-DF', 'nike-shorts.jpg', 'ACTIVE')
            ON CONFLICT (id) DO NOTHING
            """);

        stmt.executeUpdate("ALTER SEQUENCE products_id_seq RESTART WITH 31");

        stmt.executeUpdate("""
            INSERT INTO coupons (code, discount_type, discount_value, minimum_amount, maximum_discount, start_date, expiry_date, usage_limit, status) VALUES
            ('WELCOME10', 'PERCENTAGE', 10.00, 500.00, 200.00, '2025-01-01', '2026-12-31', 1000, 'ACTIVE'),
            ('FLAT200', 'FIXED', 200.00, 2000.00, 200.00, '2025-01-01', '2026-12-31', 500, 'ACTIVE'),
            ('SUMMER15', 'PERCENTAGE', 15.00, 1000.00, 500.00, '2025-01-01', '2026-12-31', 300, 'ACTIVE')
            ON CONFLICT (code) DO NOTHING
            """);

        System.out.println("Seed data inserted successfully.");
    }
}
