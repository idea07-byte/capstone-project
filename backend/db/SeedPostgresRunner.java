package db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class SeedPostgresRunner {

    private static final String[][] CATEGORIES = {
        {"1", "Electronics", "Audio, gadgets, and consumer electronics"},
        {"2", "Mobiles", "Smartphones, cases, and mobile gear"},
        {"3", "Laptops", "Notebooks, ultrabooks, and PC accessories"},
        {"4", "Headphones", "Over-ear, in-ear, and wireless earbuds"},
        {"5", "Clothing", "Men and women apparel, shirts, and jackets"},
        {"6", "Shoes", "Sneakers, formal shoes, and boots"},
        {"7", "Watches", "Analog, digital, and smart watches"},
        {"8", "Bags", "Backpacks, handbags, travel duffels"},
        {"9", "Home & Kitchen", "Cookware, dining, kitchen essentials"},
        {"10", "Beauty", "Skincare, perfumes, and cosmetics"},
        {"11", "Books", "Fiction, non-fiction, textbooks, and bestsellers"},
        {"12", "Toys", "Action figures, board games, and puzzles"},
        {"13", "Sports", "Sporting goods, fitness equipment, activewear"},
        {"14", "Grocery", "Pantry essentials, organic foods, snacks"},
        {"15", "Appliances", "Kitchen and home electrical appliances"}
    };

    private static final Map<String, Integer> CAT_NAME_TO_ID = new HashMap<>();
    static {
        CAT_NAME_TO_ID.put("electronics", 1);
        CAT_NAME_TO_ID.put("mobiles", 2);
        CAT_NAME_TO_ID.put("laptops", 3);
        CAT_NAME_TO_ID.put("headphones", 4);
        CAT_NAME_TO_ID.put("clothing", 5);
        CAT_NAME_TO_ID.put("shoes", 6);
        CAT_NAME_TO_ID.put("watches", 7);
        CAT_NAME_TO_ID.put("bags", 8);
        CAT_NAME_TO_ID.put("home-kitchen", 9);
        CAT_NAME_TO_ID.put("beauty", 10);
        CAT_NAME_TO_ID.put("books", 11);
        CAT_NAME_TO_ID.put("toys", 12);
        CAT_NAME_TO_ID.put("sports", 13);
        CAT_NAME_TO_ID.put("grocery", 14);
        CAT_NAME_TO_ID.put("appliances", 15);
    }

    private static final String[] ADJECTIVES = {
        "Premium", "Ultra", "Pro", "Classic", "Modern", "Compact", "Elite", "Essential", "Wireless", "Smart", "Organic", "Ergonomic", "Durable"
    };

    private static final Map<String, String[]> BASE_NAMES = new HashMap<>();
    static {
        BASE_NAMES.put("electronics", new String[]{"Bluetooth Speaker", "Power Bank 20000mAh", "USB-C Fast Hub", "Streaming Mic", "Digital Voice Recorder", "Wireless Charger Pad"});
        BASE_NAMES.put("mobiles", new String[]{"5G Smartphone 128GB", "Dual SIM Flagship Phone", "Rugged Smartphone", "Foldable Mobile Phone", "Pro Camera Phone"});
        BASE_NAMES.put("laptops", new String[]{"15.6 Inch IPS Laptop", "Slim Ultrabook 16GB RAM", "Gaming Laptop RTX", "Convertible 2-in-1 Laptop", "Business Notebook"});
        BASE_NAMES.put("headphones", new String[]{"Noise Cancelling Headphones", "True Wireless Earbuds", "Studio Monitor Headphones", "Sport Neckband Earphones"});
        BASE_NAMES.put("clothing", new String[]{"Cotton Slim Fit Shirt", "Comfort Fleece Hoodie", "Casual Denim Jacket", "Breathable Polo T-Shirt", "Formal Chino Trousers"});
        BASE_NAMES.put("shoes", new String[]{"Cushioned Running Shoes", "Classic Leather Loafers", "High-Top Canvas Sneakers", "Lightweight Training Shoes"});
        BASE_NAMES.put("watches", new String[]{"Chronograph Stainless Watch", "Minimalist Leather Watch", "Sports GPS Smartwatch", "Automatic Mechanical Watch"});
        BASE_NAMES.put("bags", new String[]{"Water-Resistant Backpack", "Canvas Messenger Bag", "Leather Crossbody Bag", "Carry-On Travel Duffle"});
        BASE_NAMES.put("home-kitchen", new String[]{"Non-Stick Frying Pan 10 Inch", "Stainless Steel Chef Knife", "Cast Iron Dutch Oven", "Insulated Thermal Flask 1L"});
        BASE_NAMES.put("beauty", new String[]{"Hydrating Face Serum", "Gentle Foaming Cleanser", "Eau De Parfum 100ml", "Natural Botanical Moisture Cream"});
        BASE_NAMES.put("books", new String[]{"Complete Guide to Modern Computing", "Chronicles of Adventure Novel", "Data Structures & Algorithms Handbook"});
        BASE_NAMES.put("toys", new String[]{"Modular Building Blocks Set", "Remote Control High-Speed Car", "Strategic Wooden Board Game", "Classic Wooden Puzzle Box"});
        BASE_NAMES.put("sports", new String[]{"Non-Slip Exercise Yoga Mat", "Adjustable Dumbbell Set", "Professional Match Football", "Carbon Fiber Badminton Racket"});
        BASE_NAMES.put("grocery", new String[]{"Organic Roasted Almonds 500g", "Pure Raw Honey 1kg", "Extra Virgin Cold Pressed Olive Oil", "Artisanal Dark Roast Coffee Beans"});
        BASE_NAMES.put("appliances", new String[]{"Digital Air Fryer 4.5L", "Electric Variable Temp Kettle", "High-Speed Smoothie Blender", "Compact Countertop Microwave Oven"});
    }

    private static final Map<String, String[]> CATEGORY_ANGLES = new HashMap<>();
    static {
        CATEGORY_ANGLES.put("electronics", new String[]{
            "https://images.unsplash.com/photo-1546868871-7041f2a55e12?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&auto=format&fit=crop&q=80"
        });
        CATEGORY_ANGLES.put("mobiles", new String[]{
            "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=800&auto=format&fit=crop&q=80"
        });
        CATEGORY_ANGLES.put("laptops", new String[]{
            "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=800&auto=format&fit=crop&q=80"
        });
        CATEGORY_ANGLES.put("headphones", new String[]{
            "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1484704849700-f032a568e944?w=800&auto=format&fit=crop&q=80"
        });
        CATEGORY_ANGLES.put("clothing", new String[]{
            "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=800&auto=format&fit=crop&q=80"
        });
        CATEGORY_ANGLES.put("shoes", new String[]{
            "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1608231387042-66d1773070a5?w=800&auto=format&fit=crop&q=80"
        });
        CATEGORY_ANGLES.put("watches", new String[]{
            "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?w=800&auto=format&fit=crop&q=80"
        });
        CATEGORY_ANGLES.put("bags", new String[]{
            "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1622560480605-d83c853bc5c3?w=800&auto=format&fit=crop&q=80"
        });
        CATEGORY_ANGLES.put("home-kitchen", new String[]{
            "https://images.unsplash.com/photo-1584990347449-a2a51f33f679?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=800&auto=format&fit=crop&q=80"
        });
        CATEGORY_ANGLES.put("beauty", new String[]{
            "https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=800&auto=format&fit=crop&q=80"
        });
        CATEGORY_ANGLES.put("books", new String[]{
            "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=800&auto=format&fit=crop&q=80"
        });
        CATEGORY_ANGLES.put("toys", new String[]{
            "https://images.unsplash.com/photo-1566576912321-d58ddd7a6088?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1596461404969-9ae70f2830c1?w=800&auto=format&fit=crop&q=80"
        });
        CATEGORY_ANGLES.put("sports", new String[]{
            "https://images.unsplash.com/photo-1517649763962-0c623266ddc0?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=800&auto=format&fit=crop&q=80"
        });
        CATEGORY_ANGLES.put("grocery", new String[]{
            "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1610832958506-aa56368176cf?w=800&auto=format&fit=crop&q=80"
        });
        CATEGORY_ANGLES.put("appliances", new String[]{
            "https://images.unsplash.com/photo-1590794056226-79ef3a8147e1?w=800&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1556911220-e15b29be8c8f?w=800&auto=format&fit=crop&q=80"
        });
    }

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("Starting 1,000 Products PostgreSQL Seeder");
        System.out.println("Target: Supabase PostgreSQL Database");
        System.out.println("=================================================");

        File csvFile = new File("../amazon-capstone/image_mapping.csv");
        if (!csvFile.exists()) {
            csvFile = new File("amazon-capstone/image_mapping.csv");
        }
        if (!csvFile.exists()) {
            System.err.println("Error: image_mapping.csv not found at: " + csvFile.getAbsolutePath());
            System.exit(1);
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            try (Statement alterStmt = conn.createStatement()) {
                alterStmt.execute("ALTER TABLE product_images ADD COLUMN IF NOT EXISTS is_primary BOOLEAN DEFAULT FALSE");
                alterStmt.execute("DELETE FROM product_images");
            }

            // 1. Seed Categories
            System.out.println("\n[1/4] Seeding 15 categories...");
            String catSql = "INSERT INTO categories (id, name, description, status) VALUES (?, ?, ?, 'ACTIVE') " +
                            "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description";
            try (PreparedStatement psCat = conn.prepareStatement(catSql)) {
                for (String[] cat : CATEGORIES) {
                    psCat.setInt(1, Integer.parseInt(cat[0]));
                    psCat.setString(2, cat[1]);
                    psCat.setString(3, cat[2]);
                    psCat.addBatch();
                }
                psCat.executeBatch();
            }

            // 2. Read CSV and Prepare Products Batch
            System.out.println("[2/4] Preparing 1,000 products batch from image_mapping.csv...");
            String prodSql = "INSERT INTO products (id, vendor_id, category_id, brand_id, name, description, price, discount, stock_quantity, sku, image, status) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE') " +
                             "ON CONFLICT (id) DO UPDATE SET " +
                             "name = EXCLUDED.name, description = EXCLUDED.description, " +
                             "category_id = EXCLUDED.category_id, brand_id = EXCLUDED.brand_id, vendor_id = EXCLUDED.vendor_id, " +
                             "price = EXCLUDED.price, discount = EXCLUDED.discount, stock_quantity = EXCLUDED.stock_quantity, " +
                             "sku = EXCLUDED.sku, image = EXCLUDED.image, status = EXCLUDED.status";

            String imgSql = "INSERT INTO product_images (id, product_id, image_url, is_primary) VALUES (?, ?, ?, ?) " +
                            "ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, is_primary = EXCLUDED.is_primary";

            int productCount = 0;
            try (BufferedReader br = new BufferedReader(new FileReader(csvFile));
                 PreparedStatement psProd = conn.prepareStatement(prodSql);
                 PreparedStatement psImg = conn.prepareStatement(imgSql)) {

                String line = br.readLine(); // skip header: image_id,product_id,category,filename
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(",");
                    int productId = Integer.parseInt(parts[1].trim());
                    String category = parts[2].trim();
                    String filename = parts[3].trim();

                    int catId = CAT_NAME_TO_ID.getOrDefault(category, 1);
                    int vendorId = ((productId - 1) % 5) + 1; // 1 to 5
                    int brandId = ((productId - 1) % 10) + 1;  // 1 to 10

                    String adj = ADJECTIVES[productId % ADJECTIVES.length];
                    String[] namePool = BASE_NAMES.getOrDefault(category, new String[]{"Product Item"});
                    String baseName = namePool[productId % namePool.length];
                    String title = adj + " " + baseName + " (Model " + String.format("%04d", productId) + ")";
                    String desc = "High performance " + category + " product featuring premium craftsmanship, durability, and standard 1-year warranty.";

                    double basePrice = 29.99;
                    if ("laptops".equals(category)) basePrice = 699.99;
                    else if ("mobiles".equals(category)) basePrice = 499.99;
                    else if ("appliances".equals(category)) basePrice = 129.99;
                    else if ("electronics".equals(category)) basePrice = 59.99;
                    else if ("watches".equals(category)) basePrice = 89.99;
                    else if ("headphones".equals(category)) basePrice = 79.99;
                    else if ("shoes".equals(category)) basePrice = 64.99;
                    else if ("clothing".equals(category)) basePrice = 34.99;
                    else if ("bags".equals(category)) basePrice = 44.99;
                    else if ("home-kitchen".equals(category)) basePrice = 39.99;
                    else if ("sports".equals(category)) basePrice = 35.99;
                    else if ("toys".equals(category)) basePrice = 24.99;
                    else if ("beauty".equals(category)) basePrice = 22.99;
                    else if ("books".equals(category)) basePrice = 18.99;
                    else if ("grocery".equals(category)) basePrice = 12.99;

                    double price = Math.round((basePrice + ((productId * 7) % 40)) * 100.0) / 100.0;
                    double discount = (productId % 5 == 0) ? 15.0 : ((productId % 3 == 0) ? 10.0 : 0.0);
                    int stock = 15 + ((productId * 13) % 185);
                    String sku = "SKU-" + category.substring(0, Math.min(3, category.length())).toUpperCase() + "-" + String.format("%05d", productId);
                    String imagePath = "/product-images/" + category + "/" + filename;

                    // Products statement
                    psProd.setInt(1, productId);
                    psProd.setInt(2, vendorId);
                    psProd.setInt(3, catId);
                    psProd.setInt(4, brandId);
                    psProd.setString(5, title);
                    psProd.setString(6, desc);
                    psProd.setDouble(7, price);
                    psProd.setDouble(8, discount);
                    psProd.setInt(9, stock);
                    psProd.setString(10, sku);
                    psProd.setString(11, imagePath);
                    psProd.addBatch();

                    // Product Images statement - 3 gallery views per product
                    int baseImgId = (productId - 1) * 3 + 1;
                    // Image 1: Main product view
                    psImg.setInt(1, baseImgId);
                    psImg.setInt(2, productId);
                    psImg.setString(3, imagePath);
                    psImg.setBoolean(4, true);
                    psImg.addBatch();

                    String[] angles = CATEGORY_ANGLES.getOrDefault(category, new String[]{imagePath, imagePath});
                    // Image 2: Alternative angle / detail view
                    psImg.setInt(1, baseImgId + 1);
                    psImg.setInt(2, productId);
                    psImg.setString(3, angles[0]);
                    psImg.setBoolean(4, false);
                    psImg.addBatch();

                    // Image 3: Lifestyle / in-use view
                    psImg.setInt(1, baseImgId + 2);
                    psImg.setInt(2, productId);
                    psImg.setString(3, angles[1]);
                    psImg.setBoolean(4, false);
                    psImg.addBatch();

                    productCount++;
                }

                System.out.println("[3/4] Executing database batch insert (" + productCount + " products & images)...");
                psProd.executeBatch();
                psImg.executeBatch();
            }

            // 4. Update sequences
            System.out.println("[4/4] Updating PostgreSQL sequence numbers...");
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT setval('categories_id_seq', (SELECT COALESCE(MAX(id), 1) FROM categories));");
                stmt.execute("SELECT setval('products_id_seq', (SELECT COALESCE(MAX(id), 1) FROM products));");
                stmt.execute("SELECT setval('product_images_id_seq', (SELECT COALESCE(MAX(id), 1) FROM product_images));");
            }

            conn.commit();
            System.out.println("Commit successful!");

            // Verify count
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM products")) {
                    if (rs.next()) {
                        System.out.println("\n=================================================");
                        System.out.println("SUCCESS! Verified total products in DB: " + rs.getInt(1));
                    }
                }
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM product_images")) {
                    if (rs.next()) {
                        System.out.println("SUCCESS! Verified total product images in DB: " + rs.getInt(1));
                        System.out.println("=================================================");
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Seeding failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
}
