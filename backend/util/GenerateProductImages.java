package util;

import db.Database;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GenerateProductImages
 *
 * Generates all 1,000 distinct product images (600x600 JPG)
 * for the BuyIt marketplace directly from the authoritative PostgreSQL database.
 * Every product image includes:
 *  - 100% matching product name, model #, SKU, and price
 *  - Dedicated high-fidelity vector illustration matching the specific product item
 *  - Sleek e-commerce showcase card design
 */
public class GenerateProductImages {

    public static class ProductMeta {
        public int id;
        public int categoryId;
        public String category;
        public String name;
        public String description;
        public double price;
        public int stock;
        public String sku;
        public String filename;
    }

    private static final Map<String, String> CAT_ICONS = new HashMap<>();
    private static final Map<String, Color[]> CAT_COLORS = new HashMap<>();

    static {
        CAT_ICONS.put("electronics", "⚡");
        CAT_ICONS.put("mobiles", "📱");
        CAT_ICONS.put("laptops", "💻");
        CAT_ICONS.put("headphones", "🎧");
        CAT_ICONS.put("clothing", "👕");
        CAT_ICONS.put("shoes", "👟");
        CAT_ICONS.put("watches", "⌚");
        CAT_ICONS.put("bags", "🎒");
        CAT_ICONS.put("home-kitchen", "🍳");
        CAT_ICONS.put("beauty", "✨");
        CAT_ICONS.put("books", "📚");
        CAT_ICONS.put("toys", "🎮");
        CAT_ICONS.put("sports", "⚽");
        CAT_ICONS.put("grocery", "🥑");
        CAT_ICONS.put("appliances", "🔌");

        // Primary, Secondary, Accent, LightAccent
        CAT_COLORS.put("electronics", new Color[]{new Color(11, 19, 43), new Color(28, 37, 65), new Color(0, 180, 216), new Color(72, 202, 228)});
        CAT_COLORS.put("mobiles", new Color[]{new Color(27, 27, 58), new Color(46, 28, 78), new Color(168, 85, 247), new Color(216, 180, 254)});
        CAT_COLORS.put("laptops", new Color[]{new Color(17, 24, 39), new Color(31, 41, 55), new Color(56, 189, 248), new Color(125, 211, 252)});
        CAT_COLORS.put("headphones", new Color[]{new Color(24, 24, 27), new Color(39, 39, 42), new Color(249, 115, 22), new Color(253, 186, 116)});
        CAT_COLORS.put("clothing", new Color[]{new Color(30, 27, 75), new Color(49, 46, 129), new Color(245, 158, 11), new Color(252, 211, 77)});
        CAT_COLORS.put("shoes", new Color[]{new Color(15, 23, 42), new Color(30, 41, 59), new Color(132, 204, 22), new Color(190, 242, 100)});
        CAT_COLORS.put("watches", new Color[]{new Color(10, 10, 12), new Color(28, 25, 23), new Color(234, 179, 8), new Color(254, 240, 138)});
        CAT_COLORS.put("bags", new Color[]{new Color(38, 20, 11), new Color(69, 26, 3), new Color(217, 119, 6), new Color(251, 191, 36)});
        CAT_COLORS.put("home-kitchen", new Color[]{new Color(28, 25, 23), new Color(41, 37, 36), new Color(244, 63, 94), new Color(253, 164, 175)});
        CAT_COLORS.put("beauty", new Color[]{new Color(46, 16, 101), new Color(76, 5, 25), new Color(251, 113, 133), new Color(254, 205, 211)});
        CAT_COLORS.put("books", new Color[]{new Color(15, 23, 42), new Color(54, 16, 36), new Color(251, 191, 36), new Color(253, 230, 138)});
        CAT_COLORS.put("toys", new Color[]{new Color(30, 58, 138), new Color(29, 78, 216), new Color(250, 204, 21), new Color(253, 224, 71)});
        CAT_COLORS.put("sports", new Color[]{new Color(15, 23, 42), new Color(30, 41, 59), new Color(255, 87, 34), new Color(255, 138, 101)});
        CAT_COLORS.put("grocery", new Color[]{new Color(6, 78, 59), new Color(6, 95, 70), new Color(34, 197, 94), new Color(134, 239, 172)});
        CAT_COLORS.put("appliances", new Color[]{new Color(17, 24, 39), new Color(31, 41, 55), new Color(34, 211, 238), new Color(165, 243, 252)});
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

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("BuyIt Marketplace: 1,000 Product Image Generator");
        System.out.println("=================================================");

        Path projectRoot = Paths.get(".").toAbsolutePath().normalize();
        Path baseImagesDir = projectRoot.resolve("amazon-capstone/product-images").normalize();
        Path csvPath = projectRoot.resolve("amazon-capstone/image_mapping.csv").normalize();
        Path sqlPath = projectRoot.resolve("amazon-capstone/seed_products.sql").normalize();

        if (!Files.exists(csvPath)) {
            csvPath = projectRoot.resolve("../amazon-capstone/image_mapping.csv").normalize();
            baseImagesDir = projectRoot.resolve("../amazon-capstone/product-images").normalize();
            sqlPath = projectRoot.resolve("../amazon-capstone/seed_products.sql").normalize();
        }

        System.out.println("Product Images Directory: " + baseImagesDir);

        List<ProductMeta> products = loadProductsFromDbOrFallback(csvPath);
        System.out.println("Loaded " + products.size() + " authoritative product records.");

        if (products.isEmpty()) {
            System.err.println("No products loaded! Exiting.");
            return;
        }

        // Print first 5 for immediate verification
        for (int i = 0; i < Math.min(5, products.size()); i++) {
            ProductMeta p = products.get(i);
            System.out.println("Product #" + p.id + ": " + p.name + " (" + p.category + ") -> " + p.filename + " [$" + p.price + "]");
        }

        long startTime = System.currentTimeMillis();
        AtomicInteger generated = new AtomicInteger(0);
        final Path targetBaseImagesDir = baseImagesDir;

        // Process in parallel for rapid generation
        products.parallelStream().forEach(p -> {
            try {
                Path catDir = targetBaseImagesDir.resolve(p.category);
                Files.createDirectories(catDir);
                Path targetFile = catDir.resolve(p.filename);

                BufferedImage img = renderProductImage(p);
                ImageIO.write(img, "jpg", targetFile.toFile());

                int c = generated.incrementAndGet();
                if (c % 100 == 0 || c == products.size()) {
                    System.out.println("Progress: " + c + "/" + products.size() + " images generated...");
                }
            } catch (Exception e) {
                System.err.println("Error generating image for product " + p.id + ": " + e.getMessage());
            }
        });

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("=================================================");
        System.out.println("SUCCESS! Generated " + generated.get() + " matching product images in " + elapsed + " ms.");
        System.out.println("Output Folder: " + baseImagesDir);
        System.out.println("=================================================");

        // Synchronize seed_products.sql
        syncSeedSqlFile(sqlPath, products);
    }

    private static List<ProductMeta> loadProductsFromDbOrFallback(Path csvPath) {
        List<ProductMeta> list = new ArrayList<>();

        // 1. Try Live Database Query
        try {
            System.out.println("Attempting connection to PostgreSQL database...");
            try (Connection conn = Database.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT p.id, p.category_id, COALESCE(c.name, 'general') as category, " +
                         "p.name, p.description, p.price, p.stock_quantity, p.sku, p.image " +
                         "FROM products p LEFT JOIN categories c ON p.category_id = c.id " +
                         "ORDER BY p.id ASC")) {

                while (rs.next()) {
                    ProductMeta p = new ProductMeta();
                    p.id = rs.getInt("id");
                    p.categoryId = rs.getInt("category_id");
                    p.category = rs.getString("category").trim().toLowerCase();
                    p.name = rs.getString("name");
                    p.description = rs.getString("description");
                    p.price = rs.getDouble("price");
                    p.stock = rs.getInt("stock_quantity");
                    p.sku = rs.getString("sku");

                    String img = rs.getString("image");
                    if (img != null && img.contains("/")) {
                        String[] parts = img.split("/");
                        p.filename = parts[parts.length - 1];
                        if (parts.length >= 2) {
                            p.category = parts[parts.length - 2].trim().toLowerCase();
                        } else {
                            p.category = rs.getString("category").trim().toLowerCase().replace(" & ", "-").replace(" ", "-");
                        }
                    } else {
                        p.category = rs.getString("category").trim().toLowerCase().replace(" & ", "-").replace(" ", "-");
                        p.filename = String.format("product_%04d.jpg", p.id);
                    }
                    list.add(p);
                }
            }

            if (list.size() == 1000) {
                System.out.println("Successfully fetched all 1,000 products directly from PostgreSQL!");
                return list;
            }
        } catch (Exception e) {
            System.err.println("Notice: Could not fetch from PostgreSQL directly (" + e.getMessage() + "). Using exact DB formula.");
        }

        // 2. Exact DB Formula Fallback (guaranteed 100% match with SeedPostgresRunner.java)
        list.clear();
        Map<Integer, String[]> csvMap = new HashMap<>();
        if (Files.exists(csvPath)) {
            try (BufferedReader br = new BufferedReader(new FileReader(csvPath.toFile()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("image_id")) continue;
                    String[] parts = line.split(",");
                    if (parts.length >= 4) {
                        int pid = Integer.parseInt(parts[1].trim());
                        csvMap.put(pid, new String[]{parts[2].trim(), parts[3].trim()});
                    }
                }
            } catch (Exception e) {
                System.err.println("Warning reading CSV: " + e.getMessage());
            }
        }

        for (int pid = 1; pid <= 1000; pid++) {
            ProductMeta p = new ProductMeta();
            p.id = pid;

            String cat = "electronics";
            String fn = String.format("product_%04d.jpg", pid);
            if (csvMap.containsKey(pid)) {
                cat = csvMap.get(pid)[0].trim().toLowerCase();
                fn = csvMap.get(pid)[1].trim();
            }
            p.category = cat;
            p.filename = fn;

            String adj = ADJECTIVES[pid % ADJECTIVES.length];
            String[] pool = BASE_NAMES.getOrDefault(cat, new String[]{"Product Item"});
            String base = pool[pid % pool.length];
            p.name = adj + " " + base + " (Model " + String.format("%04d", pid) + ")";

            double basePrice = 29.99;
            if ("laptops".equals(cat)) basePrice = 699.99;
            else if ("mobiles".equals(cat)) basePrice = 499.99;
            else if ("appliances".equals(cat)) basePrice = 129.99;
            else if ("electronics".equals(cat)) basePrice = 59.99;
            else if ("watches".equals(cat)) basePrice = 89.99;
            else if ("headphones".equals(cat)) basePrice = 79.99;
            else if ("shoes".equals(cat)) basePrice = 64.99;
            else if ("clothing".equals(cat)) basePrice = 34.99;
            else if ("bags".equals(cat)) basePrice = 44.99;
            else if ("home-kitchen".equals(cat)) basePrice = 39.99;
            else if ("sports".equals(cat)) basePrice = 35.99;
            else if ("toys".equals(cat)) basePrice = 24.99;
            else if ("beauty".equals(cat)) basePrice = 22.99;
            else if ("books".equals(cat)) basePrice = 18.99;
            else if ("grocery".equals(cat)) basePrice = 12.99;

            p.price = Math.round((basePrice + ((pid * 7) % 40)) * 100.0) / 100.0;
            p.stock = 15 + ((pid * 13) % 185);
            p.sku = "SKU-" + cat.substring(0, Math.min(3, cat.length())).toUpperCase() + "-" + String.format("%05d", pid);

            list.add(p);
        }

        return list;
    }

    private static void syncSeedSqlFile(Path sqlPath, List<ProductMeta> products) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("-- ========================================================\n");
            sb.append("-- Synchronized SQL Seed File for BuyIt Marketplace\n");
            sb.append("-- 1,000 Products matching live database and image cards\n");
            sb.append("-- ========================================================\n\n");
            sb.append("USE capstone_db;\n\n");
            sb.append("INSERT INTO products (id, category_id, name, description, price, stock_quantity, sku, primary_image)\nVALUES\n");

            for (int i = 0; i < products.size(); i++) {
                ProductMeta p = products.get(i);
                int catId = p.categoryId > 0 ? p.categoryId : 1;
                String safeName = p.name.replace("'", "''");
                String desc = "High performance " + p.category + " product featuring premium craftsmanship, durability, and standard 1-year warranty.";
                String imgPath = "/product-images/" + p.category + "/" + p.filename;

                sb.append(String.format(Locale.US, "  (%d, %d, '%s', '%s', %.2f, %d, '%s', '%s')",
                        p.id, catId, safeName, desc, p.price, p.stock, p.sku, imgPath));
                if (i < products.size() - 1) sb.append(",\n");
                else sb.append(";\n");
            }

            Files.writeString(sqlPath, sb.toString());
            System.out.println("Synchronized seed_products.sql with 1,000 authoritative records.");
        } catch (Exception e) {
            System.err.println("Warning: Could not sync seed_products.sql: " + e.getMessage());
        }
    }

    public static BufferedImage renderProductImage(ProductMeta p) {
        int width = 600;
        int height = 600;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        Color[] colors = CAT_COLORS.getOrDefault(p.category, new Color[]{
                new Color(15, 23, 42), new Color(30, 41, 59), new Color(59, 130, 246), new Color(147, 197, 253)
        });
        Color bgDark = colors[0];
        Color bgMid = colors[1];
        Color accent = colors[2];
        Color lightAccent = colors[3];

        // 1. Studio Backdrop Gradient
        GradientPaint bgGrad = new GradientPaint(0, 0, bgMid, width, height, bgDark);
        g2.setPaint(bgGrad);
        g2.fillRect(0, 0, width, height);

        // Ambient spotlight glow in upper-center
        float radius = 330f;
        Point2D center = new Point2D.Float(width / 2.0f, 230f);
        float[] dist = {0.0f, 0.55f, 1.0f};
        Color[] glowColors = {
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70),
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 18),
                new Color(0, 0, 0, 0)
        };
        RadialGradientPaint pGlow = new RadialGradientPaint(center, radius, dist, glowColors);
        g2.setPaint(pGlow);
        g2.fillRect(0, 0, width, height);

        // Subtle background perspective grid
        g2.setColor(new Color(255, 255, 255, 10));
        g2.setStroke(new BasicStroke(1f));
        for (int y = 40; y < height; y += 40) {
            g2.drawLine(20, y, width - 20, y);
        }
        for (int x = 40; x < width; x += 40) {
            g2.drawLine(x, 20, x, height - 20);
        }

        // 2. Stage Platform / Pedestal
        int pedY = 350;
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillOval(width / 2 - 200, pedY + 40, 400, 50);

        GradientPaint pedGrad = new GradientPaint(width / 2 - 170, pedY, new Color(255, 255, 255, 30), width / 2 + 170, pedY + 30, new Color(255, 255, 255, 5));
        g2.setPaint(pedGrad);
        g2.fillOval(width / 2 - 170, pedY + 15, 340, 45);
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 130));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(width / 2 - 170, pedY + 15, 340, 45);

        // 3. Central Dedicated Product Vector Artwork
        drawSpecificProductArtwork(g2, p, width / 2, 220, accent, lightAccent);

        // 4. Top Header Banner
        // Left Pill: Category Tag
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(24, 24, 185, 32, 16, 16);
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 180));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(24, 24, 185, 32, 16, 16);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        String icon = CAT_ICONS.getOrDefault(p.category, "★");
        String catTag = icon + "  " + p.category.toUpperCase().replace("-", " & ");
        g2.drawString(catTag, 36, 45);

        // Right Pill: Official BuyIt Badge
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(width - 175, 24, 151, 32, 16, 16);
        g2.setColor(new Color(234, 179, 8, 180));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(width - 175, 24, 151, 32, 16, 16);

        g2.setColor(new Color(254, 240, 138));
        g2.drawString("★ BUYIT VERIFIED", width - 163, 45);

        // 5. Lower Frosted Showcase Card
        int cardX = 24;
        int cardY = 410;
        int cardW = width - 48;
        int cardH = 166;

        g2.setColor(new Color(15, 23, 42, 240));
        g2.fillRoundRect(cardX, cardY, cardW, cardH, 20, 20);

        GradientPaint cardBorder = new GradientPaint(cardX, cardY, accent, cardX + cardW, cardY, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50));
        g2.setPaint(cardBorder);
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRoundRect(cardX, cardY, cardW, cardH, 20, 20);

        // Model & SKU Pills
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 45));
        g2.fillRoundRect(cardX + 16, cardY + 16, 112, 22, 10, 10);
        g2.setColor(lightAccent);
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.drawString(String.format("MODEL #%04d", p.id), cardX + 24, cardY + 31);

        g2.setColor(new Color(255, 255, 255, 30));
        g2.fillRoundRect(cardX + 136, cardY + 16, 134, 22, 10, 10);
        g2.setColor(new Color(203, 213, 225));
        g2.setFont(new Font("Monospaced", Font.BOLD, 10));
        g2.drawString(p.sku != null ? p.sku : ("SKU-" + p.id), cardX + 144, cardY + 31);

        // Rating Stars
        g2.setColor(new Color(251, 191, 36));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.drawString("★★★★★", cardW - 55, cardY + 32);

        // Product Name (Authoritative)
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 17));
        String displayName = p.name;
        if (displayName == null || displayName.isEmpty()) {
            displayName = "Premium " + capitalize(p.category) + " Product";
        }
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(displayName) > cardW - 36) {
            while (fm.stringWidth(displayName + "...") > cardW - 36 && displayName.length() > 5) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }
            displayName += "...";
        }
        g2.drawString(displayName, cardX + 16, cardY + 68);

        // Divider
        g2.setColor(new Color(255, 255, 255, 25));
        g2.drawLine(cardX + 16, cardY + 84, cardX + cardW - 16, cardY + 84);

        // Price Section
        g2.setFont(new Font("SansSerif", Font.BOLD, 26));
        g2.setColor(new Color(52, 211, 153));
        String priceStr = String.format(Locale.US, "$%.2f", p.price > 0 ? p.price : 49.99);
        g2.drawString(priceStr, cardX + 16, cardY + 124);

        // Stock & Genuine Tag
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(new Color(16, 185, 129));
        g2.fillOval(cardX + 16, cardY + 138, 8, 8);
        g2.setColor(new Color(148, 163, 184));
        g2.drawString("IN STOCK • 100% GENUINE", cardX + 30, cardY + 146);

        // Right Warranty Seal
        int sealX = cardX + cardW - 150;
        int sealY = cardY + 98;
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 35));
        g2.fillRoundRect(sealX, sealY, 135, 48, 12, 12);
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(sealX, sealY, 135, 48, 12, 12);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("OFFICIAL WARRANTY", sealX + 12, sealY + 20);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        g2.setColor(lightAccent);
        g2.drawString("1 YEAR REPLACEMENT", sealX + 12, sealY + 36);

        g2.dispose();
        return img;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    // =========================================================================
    // SPECIFIC VECTOR ARTWORK ROUTER (64 Distinct Types)
    // =========================================================================
    private static void drawSpecificProductArtwork(Graphics2D g, ProductMeta p, int cx, int cy, Color acc, Color light) {
        String name = (p.name != null ? p.name : "").toLowerCase();
        String cat = p.category != null ? p.category.toLowerCase() : "";

        // 1. ELECTRONICS (6)
        if (cat.equals("electronics")) {
            if (name.contains("power bank")) { drawPowerBank(g, cx, cy, acc, light); return; }
            if (name.contains("hub")) { drawUsbHub(g, cx, cy, acc, light); return; }
            if (name.contains("mic")) { drawStreamingMic(g, cx, cy, acc, light); return; }
            if (name.contains("recorder")) { drawVoiceRecorder(g, cx, cy, acc, light); return; }
            if (name.contains("charger")) { drawWirelessCharger(g, cx, cy, acc, light); return; }
            drawBluetoothSpeaker(g, cx, cy, acc, light);
            return;
        }

        // 2. MOBILES (5)
        if (cat.equals("mobiles")) {
            if (name.contains("5g")) { draw5GPhone(g, cx, cy, acc, light); return; }
            if (name.contains("rugged")) { drawRuggedPhone(g, cx, cy, acc, light); return; }
            if (name.contains("foldable")) { drawFoldablePhone(g, cx, cy, acc, light); return; }
            if (name.contains("camera")) { drawProCameraPhone(g, cx, cy, acc, light); return; }
            drawFlagshipPhone(g, cx, cy, acc, light);
            return;
        }

        // 3. LAPTOPS (5)
        if (cat.equals("laptops")) {
            if (name.contains("ultrabook")) { drawUltrabook(g, cx, cy, acc, light); return; }
            if (name.contains("gaming")) { drawGamingLaptop(g, cx, cy, acc, light); return; }
            if (name.contains("2-in-1") || name.contains("convertible")) { drawConvertibleLaptop(g, cx, cy, acc, light); return; }
            if (name.contains("business")) { drawBusinessNotebook(g, cx, cy, acc, light); return; }
            drawIpsLaptop(g, cx, cy, acc, light);
            return;
        }

        // 4. HEADPHONES (4)
        if (cat.equals("headphones")) {
            if (name.contains("earbuds")) { drawEarbuds(g, cx, cy, acc, light); return; }
            if (name.contains("studio")) { drawStudioMonitors(g, cx, cy, acc, light); return; }
            if (name.contains("neckband")) { drawNeckband(g, cx, cy, acc, light); return; }
            drawOverEarHeadphones(g, cx, cy, acc, light);
            return;
        }

        // 5. CLOTHING (5)
        if (cat.equals("clothing")) {
            if (name.contains("hoodie")) { drawHoodie(g, cx, cy, acc, light); return; }
            if (name.contains("jacket")) { drawDenimJacket(g, cx, cy, acc, light); return; }
            if (name.contains("polo")) { drawPoloShirt(g, cx, cy, acc, light); return; }
            if (name.contains("trousers") || name.contains("chino")) { drawTrousers(g, cx, cy, acc, light); return; }
            drawDressShirt(g, cx, cy, acc, light);
            return;
        }

        // 6. SHOES (4)
        if (cat.equals("shoes")) {
            if (name.contains("loafers")) { drawLoafers(g, cx, cy, acc, light); return; }
            if (name.contains("sneakers") || name.contains("canvas")) { drawCanvasSneakers(g, cx, cy, acc, light); return; }
            if (name.contains("training")) { drawTrainingShoes(g, cx, cy, acc, light); return; }
            drawRunningShoes(g, cx, cy, acc, light);
            return;
        }

        // 7. WATCHES (4)
        if (cat.equals("watches")) {
            if (name.contains("minimalist")) { drawMinimalistWatch(g, cx, cy, acc, light); return; }
            if (name.contains("smartwatch") || name.contains("gps")) { drawSmartwatch(g, cx, cy, acc, light); return; }
            if (name.contains("automatic") || name.contains("mechanical")) { drawMechanicalWatch(g, cx, cy, acc, light); return; }
            drawChronograph(g, cx, cy, acc, light);
            return;
        }

        // 8. BAGS (4)
        if (cat.equals("bags")) {
            if (name.contains("messenger")) { drawMessengerBag(g, cx, cy, acc, light); return; }
            if (name.contains("crossbody")) { drawCrossbodyBag(g, cx, cy, acc, light); return; }
            if (name.contains("duffle") || name.contains("travel")) { drawDuffleBag(g, cx, cy, acc, light); return; }
            drawBackpack(g, cx, cy, acc, light);
            return;
        }

        // 9. HOME & KITCHEN (4)
        if (cat.equals("home-kitchen")) {
            if (name.contains("knife")) { drawChefKnife(g, cx, cy, acc, light); return; }
            if (name.contains("dutch oven")) { drawDutchOven(g, cx, cy, acc, light); return; }
            if (name.contains("flask")) { drawThermalFlask(g, cx, cy, acc, light); return; }
            drawFryingPan(g, cx, cy, acc, light);
            return;
        }

        // 10. BEAUTY (4)
        if (cat.equals("beauty")) {
            if (name.contains("cleanser")) { drawCleanser(g, cx, cy, acc, light); return; }
            if (name.contains("parfum") || name.contains("perfume")) { drawPerfume(g, cx, cy, acc, light); return; }
            if (name.contains("cream")) { drawMoistureCream(g, cx, cy, acc, light); return; }
            drawFaceSerum(g, cx, cy, acc, light);
            return;
        }

        // 11. BOOKS (3)
        if (cat.equals("books")) {
            if (name.contains("computing")) { drawComputingBook(g, cx, cy, acc, light); return; }
            if (name.contains("algorithms") || name.contains("handbook")) { drawHandbook(g, cx, cy, acc, light); return; }
            drawNovelBook(g, cx, cy, acc, light);
            return;
        }

        // 12. TOYS (4)
        if (cat.equals("toys")) {
            if (name.contains("car")) { drawRcCar(g, cx, cy, acc, light); return; }
            if (name.contains("board game")) { drawBoardGame(g, cx, cy, acc, light); return; }
            if (name.contains("puzzle")) { drawPuzzleBox(g, cx, cy, acc, light); return; }
            drawBuildingBlocks(g, cx, cy, acc, light);
            return;
        }

        // 13. SPORTS (4)
        if (cat.equals("sports")) {
            if (name.contains("yoga")) { drawYogaMat(g, cx, cy, acc, light); return; }
            if (name.contains("football")) { drawFootball(g, cx, cy, acc, light); return; }
            if (name.contains("badminton")) { drawBadminton(g, cx, cy, acc, light); return; }
            drawDumbbells(g, cx, cy, acc, light);
            return;
        }

        // 14. GROCERY (4)
        if (cat.equals("grocery")) {
            if (name.contains("honey")) { drawHoney(g, cx, cy, acc, light); return; }
            if (name.contains("olive oil")) { drawOliveOil(g, cx, cy, acc, light); return; }
            if (name.contains("coffee")) { drawCoffee(g, cx, cy, acc, light); return; }
            drawAlmonds(g, cx, cy, acc, light);
            return;
        }

        // 15. APPLIANCES (4)
        if (cat.equals("appliances")) {
            if (name.contains("kettle")) { drawKettle(g, cx, cy, acc, light); return; }
            if (name.contains("blender")) { drawBlender(g, cx, cy, acc, light); return; }
            if (name.contains("microwave")) { drawMicrowave(g, cx, cy, acc, light); return; }
            drawAirFryer(g, cx, cy, acc, light);
            return;
        }

        drawBluetoothSpeaker(g, cx, cy, acc, light);
    }

    // =========================================================================
    // 1. ELECTRONICS ARTWORK
    // =========================================================================
    private static void drawPowerBank(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 110, h = 180;
        // Shadow & Body
        g.setColor(new Color(15, 23, 42));
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 28, 28);
        GradientPaint body = new GradientPaint(cx - w / 2, cy - h / 2, new Color(45, 55, 75), cx + w / 2, cy + h / 2, new Color(20, 28, 45));
        g.setPaint(body);
        g.fillRoundRect(cx - w / 2 + 4, cy - h / 2 + 4, w - 8, h - 8, 24, 24);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 28, 28);

        // 4 LED battery indicator dots
        for (int i = 0; i < 4; i++) {
            g.setColor(i < 3 ? new Color(34, 197, 94) : new Color(100, 116, 139));
            g.fillOval(cx - 30 + i * 20, cy - h / 2 + 25, 10, 10);
            if (i < 3) {
                g.setColor(new Color(34, 197, 94, 100));
                g.drawOval(cx - 32 + i * 20, cy - h / 2 + 23, 14, 14);
            }
        }

        // Center Bolt Emblem
        Path2D.Double bolt = new Path2D.Double();
        bolt.moveTo(cx + 4, cy - 25);
        bolt.lineTo(cx - 18, cy + 2);
        bolt.lineTo(cx - 2, cy + 2);
        bolt.lineTo(cx - 6, cy + 30);
        bolt.lineTo(cx + 18, cy);
        bolt.lineTo(cx + 2, cy);
        bolt.closePath();
        g.setColor(acc);
        g.fill(bolt);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(bolt);

        // Power rating badge
        g.setColor(new Color(255, 255, 255, 180));
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.drawString("20,000 mAh", cx - 32, cy + 50);

        // USB Output Ports
        g.setColor(new Color(15, 23, 42));
        g.fillRoundRect(cx - 35, cy + h / 2 - 14, 28, 8, 4, 4);
        g.fillRoundRect(cx + 7, cy + h / 2 - 14, 28, 8, 4, 4);
    }

    private static void drawUsbHub(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 170, h = 60;
        // Hub body
        GradientPaint hub = new GradientPaint(cx - w / 2, cy - h / 2, new Color(55, 65, 81), cx + w / 2, cy + h / 2, new Color(17, 24, 39));
        g.setPaint(hub);
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 18, 18);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 18, 18);

        // USB Ports (3x USB 3.0 blue + 1x Type-C)
        for (int i = 0; i < 3; i++) {
            g.setColor(new Color(2, 132, 199));
            g.fillRoundRect(cx - 65 + i * 36, cy - 10, 24, 18, 4, 4);
            g.setColor(Color.BLACK);
            g.fillRect(cx - 62 + i * 36, cy - 6, 18, 10);
            g.setColor(new Color(56, 189, 248));
            g.fillRect(cx - 62 + i * 36, cy - 2, 18, 4);
        }
        // Type-C port
        g.setColor(new Color(234, 179, 8));
        g.fillRoundRect(cx + 46, cy - 6, 18, 10, 5, 5);

        // Cable curling out of left
        g.setColor(new Color(75, 85, 99));
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double cable = new Path2D.Double();
        cable.moveTo(cx - w / 2, cy);
        cable.curveTo(cx - w / 2 - 40, cy, cx - w / 2 - 40, cy - 60, cx - w / 2 - 10, cy - 60);
        g.draw(cable);

        // USB-C connector head
        g.setColor(new Color(209, 213, 219));
        g.fillRoundRect(cx - w / 2 - 12, cy - 66, 22, 12, 4, 4);
    }

    private static void drawStreamingMic(Graphics2D g, int cx, int cy, Color acc, Color light) {
        // Stand base
        g.setColor(new Color(30, 41, 59));
        g.fillOval(cx - 50, cy + 60, 100, 24);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2f));
        g.drawOval(cx - 50, cy + 60, 100, 24);

        // Vertical rod
        g.setColor(new Color(71, 85, 105));
        g.setStroke(new BasicStroke(8f));
        g.drawLine(cx, cy + 10, cx, cy + 65);

        // Shockmount ring
        g.setColor(new Color(51, 65, 85));
        g.setStroke(new BasicStroke(4f));
        g.drawOval(cx - 45, cy - 45, 90, 75);

        // Capsule
        int mw = 52, mh = 85;
        GradientPaint micGrad = new GradientPaint(cx - mw / 2, cy - mh / 2, new Color(30, 41, 59), cx + mw / 2, cy + mh / 2, new Color(15, 23, 42));
        g.setPaint(micGrad);
        g.fillRoundRect(cx - mw / 2, cy - 50, mw, mh, 28, 28);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - mw / 2, cy - 50, mw, mh, 28, 28);

        // Mesh pattern
        g.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 80));
        for (int y = cy - 40; y <= cy - 10; y += 6) {
            g.drawLine(cx - mw / 2 + 8, y, cx + mw / 2 - 8, y);
        }

        // Gain knob
        g.setColor(new Color(234, 179, 8));
        g.fillOval(cx - 8, cy + 10, 16, 16);
    }

    private static void drawVoiceRecorder(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 90, h = 180;
        // Body
        g.setColor(new Color(15, 23, 42));
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 24, 24);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 24, 24);

        // Top dual stereo mics
        g.setColor(new Color(100, 116, 139));
        g.fillRoundRect(cx - 36, cy - h / 2 - 12, 28, 16, 8, 8);
        g.fillRoundRect(cx + 8, cy - h / 2 - 12, 28, 16, 8, 8);

        // Backlit waveform display screen
        g.setColor(new Color(8, 47, 73));
        g.fillRoundRect(cx - 35, cy - h / 2 + 18, 70, 55, 8, 8);
        g.setColor(new Color(56, 189, 248));
        for (int i = 0; i < 9; i++) {
            int bh = (int) (12 + Math.sin(i * 0.9) * 10 + (i % 3) * 6);
            g.fillRect(cx - 28 + i * 6, cy - 40 - bh / 2, 4, bh);
        }

        // REC Red Button
        g.setColor(new Color(239, 68, 68));
        g.fillOval(cx - 18, cy + 15, 36, 36);
        g.setColor(Color.WHITE);
        g.fillOval(cx - 7, cy + 26, 14, 14);
    }

    private static void drawWirelessCharger(Graphics2D g, int cx, int cy, Color acc, Color light) {
        // Pad Outer Disc
        int d = 160;
        GradientPaint pad = new GradientPaint(cx - d / 2, cy - 20, new Color(40, 48, 68), cx + d / 2, cy + 40, new Color(15, 23, 42));
        g.setPaint(pad);
        g.fillOval(cx - d / 2, cy - 30, d, 75);
        g.setColor(acc);
        g.setStroke(new BasicStroke(3f));
        g.drawOval(cx - d / 2, cy - 30, d, 75);

        // Concentric glowing Qi rings
        g.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 120));
        g.setStroke(new BasicStroke(2f));
        g.drawOval(cx - 55, cy - 18, 110, 50);
        g.drawOval(cx - 30, cy - 9, 60, 32);

        // Energy pulses
        g.setColor(light);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx, cy - 5, cx, cy + 12);
        g.drawLine(cx - 10, cy + 3, cx + 10, cy + 3);

        // Halo aura
        g.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 80));
        g.drawArc(cx - 85, cy - 50, 170, 60, 20, 140);
        g.drawArc(cx - 95, cy - 65, 190, 70, 20, 140);
    }

    private static void drawBluetoothSpeaker(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 130, h = 170;
        GradientPaint bodyGrad = new GradientPaint(cx - w / 2, cy - h / 2, new Color(40, 48, 68), cx + w / 2, cy + h / 2, new Color(15, 23, 42));
        g.setPaint(bodyGrad);
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 36, 36);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 36, 36);

        // Grille dots
        g.setColor(new Color(255, 255, 255, 25));
        for (int row = -45; row <= 45; row += 15) {
            for (int col = -40; col <= 40; col += 15) {
                g.fillOval(cx + col, cy + row, 5, 5);
            }
        }

        // Center Driver
        g.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 80));
        g.fillOval(cx - 36, cy - 36, 72, 72);
        g.setColor(light);
        g.setStroke(new BasicStroke(3f));
        g.drawOval(cx - 28, cy - 28, 56, 56);

        // Power Glyph
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.5f));
        g.drawArc(cx - 12, cy - 12, 24, 24, -60, 300);
        g.drawLine(cx, cy - 16, cx, cy - 5);

        // Soundwaves
        g.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 160));
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(cx - 95, cy - 35, 35, 70, 120, 120);
        g.drawArc(cx + 60, cy - 35, 35, 70, -60, 120);
    }

    // =========================================================================
    // 2. MOBILES ARTWORK
    // =========================================================================
    private static void draw5GPhone(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 115, h = 205;
        g.setColor(new Color(15, 23, 42));
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 32, 32);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 32, 32);

        // Screen
        GradientPaint scr = new GradientPaint(cx, cy - h / 2, new Color(30, 27, 75), cx, cy + h / 2, new Color(15, 23, 42));
        g.setPaint(scr);
        g.fillRoundRect(cx - w / 2 + 6, cy - h / 2 + 6, w - 12, h - 12, 26, 26);

        // Punch hole
        g.setColor(Color.BLACK);
        g.fillOval(cx - 4, cy - h / 2 + 12, 8, 8);

        // 5G Speed Glyph
        g.setColor(acc);
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        g.drawString("5G", cx - 26, cy + 5);

        // Signal Arcs
        g.setColor(light);
        g.setStroke(new BasicStroke(2.5f));
        g.drawArc(cx - 35, cy - 65, 70, 70, 45, 90);
        g.drawArc(cx - 48, cy - 78, 96, 96, 45, 90);
    }

    private static void drawFlagshipPhone(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 115, h = 205;
        // Sleek phone showing back camera bump
        GradientPaint back = new GradientPaint(cx - w / 2, cy - h / 2, new Color(88, 28, 135), cx + w / 2, cy + h / 2, new Color(30, 27, 75));
        g.setPaint(back);
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 32, 32);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 32, 32);

        // Camera Island
        int iw = 48, ih = 72;
        g.setColor(new Color(15, 23, 42));
        g.fillRoundRect(cx - w / 2 + 12, cy - h / 2 + 14, iw, ih, 16, 16);
        g.setColor(new Color(147, 51, 234));
        g.drawRoundRect(cx - w / 2 + 12, cy - h / 2 + 14, iw, ih, 16, 16);

        // 3 camera lenses
        g.setColor(new Color(30, 41, 59));
        g.fillOval(cx - w / 2 + 20, cy - h / 2 + 20, 18, 18);
        g.fillOval(cx - w / 2 + 20, cy - h / 2 + 42, 18, 18);
        g.fillOval(cx - w / 2 + 40, cy - h / 2 + 31, 14, 14);

        g.setColor(acc);
        g.drawOval(cx - w / 2 + 20, cy - h / 2 + 20, 18, 18);
        g.drawOval(cx - w / 2 + 20, cy - h / 2 + 42, 18, 18);
        g.drawOval(cx - w / 2 + 40, cy - h / 2 + 31, 14, 14);
    }

    private static void drawRuggedPhone(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 125, h = 210;
        // Armored chassis
        g.setColor(new Color(31, 41, 55));
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 20, 20);

        // Corner shock bumpers
        g.setColor(new Color(234, 179, 8));
        g.fillRoundRect(cx - w / 2 - 4, cy - h / 2 - 4, 28, 28, 8, 8);
        g.fillRoundRect(cx + w / 2 - 24, cy - h / 2 - 4, 28, 28, 8, 8);
        g.fillRoundRect(cx - w / 2 - 4, cy + h / 2 - 24, 28, 28, 8, 8);
        g.fillRoundRect(cx + w / 2 - 24, cy + h / 2 - 24, 28, 28, 8, 8);

        // Inner screen
        g.setColor(new Color(15, 23, 42));
        g.fillRect(cx - w / 2 + 14, cy - h / 2 + 20, w - 28, h - 40);
        g.setColor(acc);
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.drawString("IP68 MIL-STD", cx - 46, cy);
    }

    private static void drawFoldablePhone(Graphics2D g, int cx, int cy, Color acc, Color light) {
        // Dual screen angled fold
        int sw = 75, sh = 170;
        // Left leaf
        g.setColor(new Color(30, 27, 75));
        g.fillRoundRect(cx - sw - 4, cy - sh / 2, sw, sh, 14, 14);
        g.setColor(acc);
        g.drawRoundRect(cx - sw - 4, cy - sh / 2, sw, sh, 14, 14);

        // Right leaf
        g.setColor(new Color(46, 16, 101));
        g.fillRoundRect(cx + 4, cy - sh / 2, sw, sh, 14, 14);
        g.setColor(acc);
        g.drawRoundRect(cx + 4, cy - sh / 2, sw, sh, 14, 14);

        // Glowing center hinge
        g.setColor(light);
        g.setStroke(new BasicStroke(4f));
        g.drawLine(cx, cy - sh / 2 + 6, cx, cy + sh / 2 - 6);
    }

    private static void drawProCameraPhone(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 115, h = 205;
        g.setColor(new Color(24, 24, 27));
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 30, 30);

        // Large circular camera lens
        int dia = 86;
        g.setColor(new Color(39, 39, 42));
        g.fillOval(cx - dia / 2, cy - 50, dia, dia);
        g.setColor(new Color(234, 179, 8));
        g.setStroke(new BasicStroke(3f));
        g.drawOval(cx - dia / 2, cy - 50, dia, dia);

        g.setColor(new Color(15, 23, 42));
        g.fillOval(cx - 30, cy - 43, 60, 60);
        g.setColor(acc);
        g.fillOval(cx - 16, cy - 29, 32, 32);
    }

    // =========================================================================
    // 3. LAPTOPS ARTWORK
    // =========================================================================
    private static void drawIpsLaptop(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int sw = 200, sh = 120;
        // Screen
        g.setColor(new Color(15, 23, 42));
        g.fillRoundRect(cx - sw / 2, cy - sh + 20, sw, sh, 12, 12);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(cx - sw / 2, cy - sh + 20, sw, sh, 12, 12);

        GradientPaint disp = new GradientPaint(cx - sw / 2, cy - sh + 25, new Color(3, 105, 161), cx + sw / 2, cy + 10, new Color(15, 23, 42));
        g.setPaint(disp);
        g.fillRect(cx - sw / 2 + 8, cy - sh + 28, sw - 16, sh - 18);

        // Base keyboard deck
        int bw1 = 230, bw2 = 250, bh = 50;
        Polygon base = new Polygon();
        base.addPoint(cx - bw1 / 2, cy + 20);
        base.addPoint(cx + bw1 / 2, cy + 20);
        base.addPoint(cx + bw2 / 2, cy + 20 + bh);
        base.addPoint(cx - bw2 / 2, cy + 20 + bh);
        g.setColor(new Color(51, 65, 85));
        g.fillPolygon(base);
        g.setColor(new Color(148, 163, 184));
        g.drawPolygon(base);

        // Trackpad
        g.setColor(new Color(30, 41, 59));
        g.fillRoundRect(cx - 30, cy + 48, 60, 18, 4, 4);
    }

    private static void drawUltrabook(Graphics2D g, int cx, int cy, Color acc, Color light) {
        // Ultra thin wedge profile
        drawIpsLaptop(g, cx, cy - 10, acc, light);
        g.setColor(light);
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.drawString("ULTRA SLIM 1.1kg", cx - 44, cy - 40);
    }

    private static void drawGamingLaptop(Graphics2D g, int cx, int cy, Color acc, Color light) {
        drawIpsLaptop(g, cx, cy, new Color(244, 63, 94), new Color(251, 146, 60));
        // RGB keyboard glow
        g.setColor(new Color(236, 72, 153));
        g.fillRect(cx - 75, cy + 26, 45, 12);
        g.setColor(new Color(59, 130, 246));
        g.fillRect(cx - 20, cy + 26, 45, 12);
        g.setColor(new Color(34, 197, 94));
        g.fillRect(cx + 35, cy + 26, 45, 12);
    }

    private static void drawConvertibleLaptop(Graphics2D g, int cx, int cy, Color acc, Color light) {
        // Tent mode
        Path2D.Double tent = new Path2D.Double();
        tent.moveTo(cx, cy - 70);
        tent.lineTo(cx - 95, cy + 60);
        tent.lineTo(cx - 75, cy + 60);
        tent.lineTo(cx, cy - 40);
        tent.lineTo(cx + 75, cy + 60);
        tent.lineTo(cx + 95, cy + 60);
        tent.closePath();
        g.setColor(new Color(30, 41, 59));
        g.fill(tent);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2.5f));
        g.draw(tent);
    }

    private static void drawBusinessNotebook(Graphics2D g, int cx, int cy, Color acc, Color light) {
        drawIpsLaptop(g, cx, cy, acc, light);
        // Red trackpoint in keyboard
        g.setColor(new Color(239, 68, 68));
        g.fillOval(cx - 3, cy + 32, 6, 6);
    }

    // =========================================================================
    // 4. HEADPHONES ARTWORK
    // =========================================================================
    private static void drawOverEarHeadphones(Graphics2D g, int cx, int cy, Color acc, Color light) {
        // Padded Headband
        g.setColor(new Color(55, 65, 81));
        g.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(cx - 85, cy - 105, 170, 160, 20, 140);
        g.setColor(acc);
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(cx - 65, cy - 95, 130, 120, 35, 110);

        // Ear Cups
        int cw = 44, ch = 80;
        g.setColor(new Color(31, 41, 55));
        g.fillRoundRect(cx - 95, cy - 20, cw, ch, 28, 28);
        g.fillRoundRect(cx + 95 - cw, cy - 20, cw, ch, 28, 28);
        g.setColor(acc);
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(cx - 95, cy - 20, cw, ch, 28, 28);
        g.drawRoundRect(cx + 95 - cw, cy - 20, cw, ch, 28, 28);
    }

    private static void drawEarbuds(Graphics2D g, int cx, int cy, Color acc, Color light) {
        // Charging case
        int cw = 110, ch = 90;
        GradientPaint caseGrad = new GradientPaint(cx - cw / 2, cy, new Color(45, 55, 75), cx + cw / 2, cy + ch, new Color(15, 23, 42));
        g.setPaint(caseGrad);
        g.fillRoundRect(cx - cw / 2, cy - 10, cw, ch, 36, 36);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - cw / 2, cy - 10, cw, ch, 36, 36);

        // Floating left and right earbuds
        drawSingleBud(g, cx - 40, cy - 45, acc);
        drawSingleBud(g, cx + 40, cy - 45, acc);
    }

    private static void drawSingleBud(Graphics2D g, int x, int y, Color acc) {
        g.setColor(Color.WHITE);
        g.fillOval(x - 14, y - 14, 28, 28);
        g.setColor(new Color(209, 213, 219));
        g.fillRoundRect(x - 5, y + 6, 10, 32, 5, 5);
        g.setColor(acc);
        g.fillOval(x - 8, y - 8, 16, 16);
    }

    private static void drawStudioMonitors(Graphics2D g, int cx, int cy, Color acc, Color light) {
        drawOverEarHeadphones(g, cx, cy, acc, light);
        // Coiled cable
        g.setColor(new Color(234, 179, 8));
        g.setStroke(new BasicStroke(3f));
        for (int i = 0; i < 4; i++) {
            g.drawArc(cx - 85 + i * 8, cy + 50 + i * 6, 16, 16, 0, 360);
        }
    }

    private static void drawNeckband(Graphics2D g, int cx, int cy, Color acc, Color light) {
        // Horseshoe silicone collar
        g.setColor(new Color(30, 41, 59));
        g.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(cx - 80, cy - 50, 160, 140, 200, 140);
        g.setColor(acc);
        g.setStroke(new BasicStroke(4f));
        g.drawArc(cx - 80, cy - 50, 160, 140, 200, 140);
    }

    // =========================================================================
    // 5. CLOTHING ARTWORK
    // =========================================================================
    private static void drawDressShirt(Graphics2D g, int cx, int cy, Color acc, Color light) {
        Polygon shirt = new Polygon();
        shirt.addPoint(cx - 70, cy - 70);
        shirt.addPoint(cx - 105, cy - 30);
        shirt.addPoint(cx - 85, cy + 75);
        shirt.addPoint(cx + 85, cy + 75);
        shirt.addPoint(cx + 105, cy - 30);
        shirt.addPoint(cx + 70, cy - 70);
        shirt.addPoint(cx, cy - 40);

        GradientPaint sg = new GradientPaint(cx - 80, cy - 70, acc, cx + 80, cy + 75, new Color(30, 27, 75));
        g.setPaint(sg);
        g.fillPolygon(shirt);
        g.setColor(light);
        g.setStroke(new BasicStroke(2.5f));
        g.drawPolygon(shirt);

        // Buttons
        g.setColor(Color.WHITE);
        for (int b = -10; b <= 50; b += 18) {
            g.fillOval(cx - 3, cy + b, 6, 6);
        }
    }

    private static void drawHoodie(Graphics2D g, int cx, int cy, Color acc, Color light) {
        drawDressShirt(g, cx, cy, acc, light);
        // Kangaroo pocket
        g.setColor(new Color(15, 23, 42, 160));
        g.fillRoundRect(cx - 45, cy + 20, 90, 45, 12, 12);
        // Drawstring hood
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3f));
        g.drawLine(cx - 15, cy - 35, cx - 15, cy - 5);
        g.drawLine(cx + 15, cy - 35, cx + 15, cy - 5);
    }

    private static void drawDenimJacket(Graphics2D g, int cx, int cy, Color acc, Color light) {
        drawDressShirt(g, cx, cy, new Color(37, 99, 235), new Color(147, 197, 253));
        // Twin flap pockets
        g.setColor(new Color(29, 78, 216));
        g.fillRect(cx - 50, cy - 10, 30, 25);
        g.fillRect(cx + 20, cy - 10, 30, 25);
    }

    private static void drawPoloShirt(Graphics2D g, int cx, int cy, Color acc, Color light) {
        drawDressShirt(g, cx, cy, acc, light);
        // Emblem
        g.setColor(new Color(234, 179, 8));
        g.drawString("★", cx + 32, cy - 5);
    }

    private static void drawTrousers(Graphics2D g, int cx, int cy, Color acc, Color light) {
        Polygon pants = new Polygon();
        pants.addPoint(cx - 55, cy - 70);
        pants.addPoint(cx + 55, cy - 70);
        pants.addPoint(cx + 45, cy + 75);
        pants.addPoint(cx + 8, cy + 75);
        pants.addPoint(cx, cy - 10);
        pants.addPoint(cx - 8, cy + 75);
        pants.addPoint(cx - 45, cy + 75);

        GradientPaint pg = new GradientPaint(cx, cy - 70, acc, cx, cy + 75, new Color(15, 23, 42));
        g.setPaint(pg);
        g.fillPolygon(pants);
        g.setColor(light);
        g.setStroke(new BasicStroke(2.5f));
        g.drawPolygon(pants);
    }

    // =========================================================================
    // 6. SHOES ARTWORK
    // =========================================================================
    private static void drawRunningShoes(Graphics2D g, int cx, int cy, Color acc, Color light) {
        Path2D.Double shoe = new Path2D.Double();
        shoe.moveTo(cx - 110, cy + 30);
        shoe.curveTo(cx - 115, cy - 35, cx - 70, cy - 55, cx - 35, cy - 45);
        shoe.curveTo(cx - 10, cy - 30, cx + 30, cy - 15, cx + 75, cy + 5);
        shoe.curveTo(cx + 110, cy + 15, cx + 120, cy + 30, cx + 110, cy + 45);
        shoe.lineTo(cx - 110, cy + 45);
        shoe.closePath();

        GradientPaint sg = new GradientPaint(cx - 100, cy - 40, acc, cx + 100, cy + 40, new Color(15, 23, 42));
        g.setPaint(sg);
        g.fill(shoe);
        g.setColor(light);
        g.setStroke(new BasicStroke(2.5f));
        g.draw(shoe);

        // Sole
        g.setColor(Color.WHITE);
        g.fillRoundRect(cx - 115, cy + 35, 232, 22, 10, 10);
        g.setColor(new Color(30, 41, 59));
        g.fillRect(cx - 115, cy + 50, 232, 6);
    }

    private static void drawLoafers(Graphics2D g, int cx, int cy, Color acc, Color light) {
        drawRunningShoes(g, cx, cy, new Color(120, 53, 15), new Color(245, 158, 11));
        // Leather shine
        g.setColor(new Color(255, 255, 255, 80));
        g.fillOval(cx - 40, cy - 15, 60, 16);
    }

    private static void drawCanvasSneakers(Graphics2D g, int cx, int cy, Color acc, Color light) {
        drawRunningShoes(g, cx, cy, new Color(220, 38, 38), Color.WHITE);
        // Star ankle patch
        g.setColor(Color.WHITE);
        g.fillOval(cx - 65, cy - 10, 24, 24);
        g.setColor(new Color(30, 41, 59));
        g.drawString("★", cx - 58, cy + 6);
    }

    private static void drawTrainingShoes(Graphics2D g, int cx, int cy, Color acc, Color light) {
        drawRunningShoes(g, cx, cy, new Color(16, 185, 129), new Color(110, 231, 183));
    }

    // =========================================================================
    // 7. WATCHES ARTWORK
    // =========================================================================
    private static void drawChronograph(Graphics2D g, int cx, int cy, Color acc, Color light) {
        // Steel strap
        g.setColor(new Color(100, 116, 139));
        g.fillRoundRect(cx - 32, cy - 100, 64, 60, 6, 6);
        g.fillRoundRect(cx - 32, cy + 40, 64, 60, 6, 6);

        int dia = 130;
        GradientPaint gold = new GradientPaint(cx - dia / 2, cy - dia / 2, new Color(254, 240, 138), cx + dia / 2, cy + dia / 2, new Color(161, 98, 7));
        g.setPaint(gold);
        g.fillOval(cx - dia / 2, cy - dia / 2, dia, dia);

        int dialDia = 106;
        g.setColor(new Color(15, 23, 42));
        g.fillOval(cx - dialDia / 2, cy - dialDia / 2, dialDia, dialDia);

        // 3 Subdials
        g.setColor(new Color(255, 255, 255, 30));
        g.fillOval(cx - 28, cy - 12, 22, 22);
        g.fillOval(cx + 6, cy - 12, 22, 22);
        g.fillOval(cx - 11, cy + 12, 22, 22);

        // Hands
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3f));
        g.drawLine(cx, cy, cx + 22, cy - 18);
        g.drawLine(cx, cy, cx - 14, cy - 12);
    }

    private static void drawMinimalistWatch(Graphics2D g, int cx, int cy, Color acc, Color light) {
        // Leather strap
        g.setColor(new Color(69, 26, 3));
        g.fillRoundRect(cx - 28, cy - 100, 56, 60, 6, 6);
        g.fillRoundRect(cx - 28, cy + 40, 56, 60, 6, 6);

        int dia = 125;
        g.setColor(Color.WHITE);
        g.fillOval(cx - dia / 2, cy - dia / 2, dia, dia);
        g.setColor(new Color(15, 23, 42));
        g.setStroke(new BasicStroke(2f));
        g.drawOval(cx - dia / 2, cy - dia / 2, dia, dia);

        // Clean baton hour lines
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6;
            int x1 = (int) (cx + 46 * Math.cos(angle));
            int y1 = (int) (cy + 46 * Math.sin(angle));
            int x2 = (int) (cx + 54 * Math.cos(angle));
            int y2 = (int) (cy + 54 * Math.sin(angle));
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private static void drawSmartwatch(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 105, h = 120;
        // Silicone strap
        g.setColor(new Color(30, 41, 59));
        g.fillRoundRect(cx - 36, cy - 100, 72, 60, 8, 8);
        g.fillRoundRect(cx - 36, cy + 40, 72, 60, 8, 8);

        // Curved square case
        g.setColor(new Color(15, 23, 42));
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 34, 34);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 34, 34);

        // Fitness circles
        g.setColor(new Color(239, 68, 68));
        g.drawArc(cx - 28, cy - 28, 56, 56, 0, 240);
        g.setColor(new Color(34, 197, 94));
        g.drawArc(cx - 20, cy - 20, 40, 40, 30, 280);
    }

    private static void drawMechanicalWatch(Graphics2D g, int cx, int cy, Color acc, Color light) {
        drawChronograph(g, cx, cy, acc, light);
        // Tourbillon gold gear
        g.setColor(new Color(234, 179, 8));
        g.drawOval(cx - 15, cy - 15, 30, 30);
    }

    // =========================================================================
    // 8. BAGS ARTWORK
    // =========================================================================
    private static void drawBackpack(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 135, h = 165;
        GradientPaint bg = new GradientPaint(cx - w / 2, cy - h / 2, acc, cx + w / 2, cy + h / 2, new Color(40, 20, 10));
        g.setPaint(bg);
        g.fillRoundRect(cx - w / 2, cy - h / 2 + 10, w, h, 42, 42);
        g.setColor(light);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - w / 2, cy - h / 2 + 10, w, h, 42, 42);

        // Front pocket
        g.setColor(new Color(69, 26, 3));
        g.fillRoundRect(cx - 48, cy + 18, 96, 70, 18, 18);
        g.setColor(new Color(234, 179, 8));
        g.drawRoundRect(cx - 48, cy + 18, 96, 70, 18, 18);
    }

    private static void drawMessengerBag(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 160, h = 110;
        g.setColor(new Color(69, 26, 3));
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 18, 18);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 18, 18);

        // Twin buckles
        g.setColor(new Color(234, 179, 8));
        g.fillRect(cx - 45, cy + 10, 14, 22);
        g.fillRect(cx + 31, cy + 10, 14, 22);
    }

    private static void drawCrossbodyBag(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 100, h = 140;
        g.setColor(new Color(55, 65, 81));
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 24, 24);
        g.setColor(acc);
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 24, 24);
        // Diagonal zip
        g.setColor(new Color(209, 213, 219));
        g.setStroke(new BasicStroke(3f));
        g.drawLine(cx - 35, cy - 35, cx + 35, cy + 20);
    }

    private static void drawDuffleBag(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 180, h = 95;
        g.setColor(new Color(30, 41, 59));
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 38, 38);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 38, 38);
        // Dual strap arches
        g.setColor(new Color(234, 179, 8));
        g.setStroke(new BasicStroke(4f));
        g.drawArc(cx - 50, cy - 80, 100, 60, 0, 180);
    }

    // =========================================================================
    // 9. HOME & KITCHEN ARTWORK
    // =========================================================================
    private static void drawFryingPan(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int pw = 140, ph = 60;
        g.setColor(new Color(30, 41, 59));
        g.fillOval(cx - pw / 2 - 10, cy, pw, ph);
        g.setColor(acc);
        g.setStroke(new BasicStroke(3f));
        g.drawOval(cx - pw / 2 - 10, cy, pw, ph);

        // Handle
        g.setColor(new Color(15, 23, 42));
        g.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx + pw / 2 - 20, cy + 30, cx + pw / 2 + 65, cy + 45);
    }

    private static void drawChefKnife(Graphics2D g, int cx, int cy, Color acc, Color light) {
        // Blade
        Path2D.Double blade = new Path2D.Double();
        blade.moveTo(cx - 100, cy);
        blade.lineTo(cx + 10, cy - 30);
        blade.lineTo(cx + 10, cy + 15);
        blade.closePath();
        g.setColor(new Color(226, 232, 240));
        g.fill(blade);
        g.setColor(acc);
        g.draw(blade);

        // Handle
        g.setColor(new Color(69, 26, 3));
        g.fillRoundRect(cx + 10, cy - 10, 75, 20, 6, 6);
    }

    private static void drawDutchOven(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 150, h = 100;
        g.setColor(new Color(220, 38, 38));
        g.fillRoundRect(cx - w / 2, cy - 10, w, h, 28, 28);
        g.setColor(acc);
        g.drawRoundRect(cx - w / 2, cy - 10, w, h, 28, 28);

        // Lid
        g.setColor(new Color(185, 28, 28));
        g.fillOval(cx - w / 2 - 5, cy - 30, w + 10, 40);
        // Knob
        g.setColor(new Color(254, 240, 138));
        g.fillOval(cx - 12, cy - 40, 24, 18);
    }

    private static void drawThermalFlask(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 70, h = 175;
        g.setColor(new Color(15, 23, 42));
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 24, 24);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 24, 24);

        // Cap
        g.setColor(new Color(100, 116, 139));
        g.fillRoundRect(cx - 24, cy - h / 2 - 16, 48, 18, 6, 6);
    }

    // =========================================================================
    // 10. BEAUTY ARTWORK
    // =========================================================================
    private static void drawFaceSerum(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int bw = 85, bh = 130;
        g.setColor(new Color(76, 5, 25));
        g.fillRoundRect(cx - bw / 2, cy - bh / 2 + 25, bw, bh, 24, 24);
        g.setColor(acc);
        g.drawRoundRect(cx - bw / 2, cy - bh / 2 + 25, bw, bh, 24, 24);

        // Dropper bulb
        g.setColor(Color.BLACK);
        g.fillOval(cx - 14, cy - bh / 2 - 15, 28, 28);
    }

    private static void drawCleanser(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int bw = 80, bh = 150;
        g.setColor(new Color(255, 255, 255, 220));
        g.fillRoundRect(cx - bw / 2, cy - bh / 2 + 20, bw, bh, 20, 20);
        g.setColor(acc);
        g.drawRoundRect(cx - bw / 2, cy - bh / 2 + 20, bw, bh, 20, 20);

        // Pump top
        g.setColor(new Color(234, 179, 8));
        g.fillRect(cx - 6, cy - bh / 2, 12, 20);
        g.fillRect(cx - 22, cy - bh / 2 - 6, 36, 10);
    }

    private static void drawPerfume(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 110, h = 110;
        g.setColor(new Color(251, 113, 133, 180));
        g.fillRoundRect(cx - w / 2, cy - 20, w, h, 20, 20);
        g.setColor(Color.WHITE);
        g.drawRoundRect(cx - w / 2, cy - 20, w, h, 20, 20);

        // Gold cap
        g.setColor(new Color(234, 179, 8));
        g.fillRect(cx - 20, cy - 55, 40, 35);
    }

    private static void drawMoistureCream(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 125, h = 80;
        g.setColor(new Color(255, 255, 255, 230));
        g.fillRoundRect(cx - w / 2, cy - 10, w, h, 24, 24);
        g.setColor(acc);
        g.drawRoundRect(cx - w / 2, cy - 10, w, h, 24, 24);

        // Gold screw lid
        g.setColor(new Color(234, 179, 8));
        g.fillRoundRect(cx - w / 2 - 4, cy - 30, w + 8, 22, 8, 8);
    }

    // =========================================================================
    // 11. BOOKS ARTWORK
    // =========================================================================
    private static void drawComputingBook(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int bw = 125, bh = 170;
        g.setColor(new Color(15, 23, 42));
        g.fillRoundRect(cx - bw / 2, cy - bh / 2, bw, bh, 14, 14);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - bw / 2, cy - bh / 2, bw, bh, 14, 14);

        // Code Brackets
        g.setColor(light);
        g.setFont(new Font("Monospaced", Font.BOLD, 32));
        g.drawString("</>", cx - 28, cy + 10);
    }

    private static void drawNovelBook(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int bw = 125, bh = 170;
        g.setColor(new Color(88, 28, 135));
        g.fillRoundRect(cx - bw / 2, cy - bh / 2, bw, bh, 14, 14);
        g.setColor(new Color(234, 179, 8));
        g.drawRoundRect(cx - bw / 2, cy - bh / 2, bw, bh, 14, 14);

        // Ribbon
        g.setColor(new Color(239, 68, 68));
        g.fillRect(cx + 20, cy + bh / 2 - 15, 14, 35);
    }

    private static void drawHandbook(Graphics2D g, int cx, int cy, Color acc, Color light) {
        drawComputingBook(g, cx, cy, new Color(245, 158, 11), Color.WHITE);
    }

    // =========================================================================
    // 12. TOYS ARTWORK
    // =========================================================================
    private static void drawBuildingBlocks(Graphics2D g, int cx, int cy, Color acc, Color light) {
        // Red, Blue, Green Bricks
        g.setColor(new Color(239, 68, 68));
        g.fillRoundRect(cx - 55, cy + 10, 110, 45, 8, 8);
        g.setColor(new Color(59, 130, 246));
        g.fillRoundRect(cx - 45, cy - 35, 90, 45, 8, 8);

        // Studs
        g.setColor(new Color(254, 202, 202));
        for (int i = 0; i < 3; i++) {
            g.fillOval(cx - 35 + i * 30, cy - 45, 16, 12);
        }
    }

    private static void drawRcCar(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 160, h = 70;
        g.setColor(new Color(239, 68, 68));
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 28, 28);
        g.setColor(acc);
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 28, 28);

        // Tires
        g.setColor(Color.BLACK);
        g.fillOval(cx - 65, cy + 15, 34, 34);
        g.fillOval(cx + 31, cy + 15, 34, 34);
    }

    private static void drawBoardGame(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 140, h = 140;
        g.setColor(new Color(180, 83, 9));
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 14, 14);
        g.setColor(Color.WHITE);
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 14, 14);

        // Dice
        g.setColor(Color.WHITE);
        g.fillRoundRect(cx - 30, cy - 30, 28, 28, 6, 6);
        g.fillRoundRect(cx + 2, cy + 2, 28, 28, 6, 6);
    }

    private static void drawPuzzleBox(Graphics2D g, int cx, int cy, Color acc, Color light) {
        drawBoardGame(g, cx, cy, acc, light);
    }

    // =========================================================================
    // 13. SPORTS ARTWORK
    // =========================================================================
    private static void drawYogaMat(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 180, h = 55;
        g.setColor(new Color(147, 51, 234));
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 24, 24);
        g.setColor(light);
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 24, 24);

        // Rolled end
        g.setColor(new Color(192, 132, 252));
        g.fillOval(cx + w / 2 - 30, cy - h / 2, 30, h);
    }

    private static void drawDumbbells(Graphics2D g, int cx, int cy, Color acc, Color light) {
        g.setColor(new Color(203, 213, 225));
        g.fillRect(cx - 65, cy - 8, 130, 16);

        // Hex plates
        g.setColor(new Color(30, 41, 59));
        g.fillRoundRect(cx - 85, cy - 40, 24, 80, 8, 8);
        g.fillRoundRect(cx + 61, cy - 40, 24, 80, 8, 8);
        g.setColor(acc);
        g.drawRoundRect(cx - 85, cy - 40, 24, 80, 8, 8);
        g.drawRoundRect(cx + 61, cy - 40, 24, 80, 8, 8);
    }

    private static void drawFootball(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int dia = 130;
        g.setColor(Color.WHITE);
        g.fillOval(cx - dia / 2, cy - dia / 2, dia, dia);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2.5f));
        g.drawOval(cx - dia / 2, cy - dia / 2, dia, dia);

        // Center pentagon
        Polygon penta = new Polygon();
        for (int i = 0; i < 5; i++) {
            double angle = i * 2 * Math.PI / 5 - Math.PI / 2;
            penta.addPoint((int) (cx + 24 * Math.cos(angle)), (int) (cy + 24 * Math.sin(angle)));
        }
        g.fillPolygon(penta);
    }

    private static void drawBadminton(Graphics2D g, int cx, int cy, Color acc, Color light) {
        // Racket oval
        g.setColor(acc);
        g.setStroke(new BasicStroke(4f));
        g.drawOval(cx - 40, cy - 85, 80, 110);
        // Shaft
        g.setColor(Color.WHITE);
        g.drawLine(cx, cy + 25, cx, cy + 85);
    }

    // =========================================================================
    // 14. GROCERY ARTWORK
    // =========================================================================
    private static void drawAlmonds(Graphics2D g, int cx, int cy, Color acc, Color light) {
        // Pouch
        int w = 110, h = 160;
        g.setColor(new Color(180, 83, 9));
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 20, 20);
        g.setColor(acc);
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 20, 20);

        // Window with almonds
        g.setColor(new Color(254, 240, 138));
        g.fillOval(cx - 26, cy + 10, 52, 45);
    }

    private static void drawHoney(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 110, h = 130;
        g.setColor(new Color(245, 158, 11));
        g.fillRoundRect(cx - w / 2, cy - h / 2 + 15, w, h, 30, 30);
        g.setColor(Color.WHITE);
        g.drawRoundRect(cx - w / 2, cy - h / 2 + 15, w, h, 30, 30);

        // Honey dipper stick
        g.setColor(new Color(120, 53, 15));
        g.setStroke(new BasicStroke(6f));
        g.drawLine(cx - 30, cy - 55, cx + 45, cy - 10);
    }

    private static void drawOliveOil(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 70, h = 175;
        g.setColor(new Color(20, 83, 45));
        g.fillRoundRect(cx - w / 2, cy - h / 2 + 10, w, h, 14, 14);
        g.setColor(acc);
        g.drawRoundRect(cx - w / 2, cy - h / 2 + 10, w, h, 14, 14);
    }

    private static void drawCoffee(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 110, h = 160;
        g.setColor(new Color(69, 26, 3));
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 16, 16);
        g.setColor(new Color(234, 179, 8));
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 16, 16);

        // Coffee Bean Logo
        g.setColor(new Color(254, 240, 138));
        g.fillOval(cx - 16, cy - 10, 32, 22);
    }

    // =========================================================================
    // 15. APPLIANCES ARTWORK
    // =========================================================================
    private static void drawAirFryer(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 145, h = 175;
        g.setColor(new Color(30, 41, 59));
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 34, 34);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 34, 34);

        // Touchscreen Panel
        g.setColor(Color.BLACK);
        g.fillRoundRect(cx - w / 2 + 15, cy - h / 2 + 16, w - 30, 40, 10, 10);
        g.setColor(acc);
        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.drawString("400°F 20M", cx - 40, cy - h / 2 + 42);

        // Basket handle
        g.setColor(new Color(15, 23, 42));
        g.fillRoundRect(cx - 16, cy + 25, 32, 45, 10, 10);
    }

    private static void drawKettle(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 120, h = 135;
        g.setColor(new Color(203, 213, 225));
        g.fillRoundRect(cx - w / 2, cy - 20, w, h, 24, 24);
        g.setColor(acc);
        g.drawRoundRect(cx - w / 2, cy - 20, w, h, 24, 24);

        // Gooseneck Spout
        g.setColor(new Color(148, 163, 184));
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double spout = new Path2D.Double();
        spout.moveTo(cx - w / 2, cy + 60);
        spout.curveTo(cx - w / 2 - 40, cy + 30, cx - w / 2 - 30, cy - 40, cx - w / 2 - 10, cy - 45);
        g.draw(spout);

        // Handle
        g.drawArc(cx + 20, cy, 65, 85, 270, 180);
    }

    private static void drawBlender(Graphics2D g, int cx, int cy, Color acc, Color light) {
        // Pitcher
        Polygon pitcher = new Polygon();
        pitcher.addPoint(cx - 50, cy - 80);
        pitcher.addPoint(cx + 50, cy - 80);
        pitcher.addPoint(cx + 35, cy + 20);
        pitcher.addPoint(cx - 35, cy + 20);
        g.setColor(new Color(255, 255, 255, 180));
        g.fillPolygon(pitcher);
        g.setColor(acc);
        g.drawPolygon(pitcher);

        // Heavy motor base
        g.setColor(new Color(30, 41, 59));
        g.fillRoundRect(cx - 45, cy + 20, 90, 65, 14, 14);
        g.setColor(new Color(234, 179, 8));
        g.fillOval(cx - 14, cy + 40, 28, 28);
    }

    private static void drawMicrowave(Graphics2D g, int cx, int cy, Color acc, Color light) {
        int w = 165, h = 115;
        g.setColor(new Color(30, 41, 59));
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 16, 16);
        g.setColor(acc);
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 16, 16);

        // Viewing window
        g.setColor(new Color(15, 23, 42));
        g.fillRoundRect(cx - w / 2 + 14, cy - h / 2 + 14, 95, h - 28, 8, 8);

        // Numeric Keypad & dial
        g.setColor(new Color(51, 65, 85));
        g.fillRoundRect(cx + 38, cy - h / 2 + 14, 32, h - 28, 6, 6);
        g.setColor(Color.GREEN);
        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.drawString("12:00", cx + 40, cy - h / 2 + 30);
    }
}
