package util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GenerateProductImages
 *
 * Generates all 1,000 distinct product images (600x600 JPG)
 * for the BuyIt marketplace based on seed_products.sql and image_mapping.csv.
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

        // Primary, Secondary, Accent, TextAccent
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

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("BuyIt Marketplace: 1,000 Product Image Generator");
        System.out.println("=================================================");

        Path projectRoot = Paths.get(".").toAbsolutePath().normalize();
        Path baseImagesDir = projectRoot.resolve("amazon-capstone/product-images").normalize();
        Path sqlPath = projectRoot.resolve("amazon-capstone/seed_products.sql").normalize();
        Path csvPath = projectRoot.resolve("amazon-capstone/image_mapping.csv").normalize();

        if (!Files.exists(sqlPath)) {
            sqlPath = projectRoot.resolve("../amazon-capstone/seed_products.sql").normalize();
            baseImagesDir = projectRoot.resolve("../amazon-capstone/product-images").normalize();
            csvPath = projectRoot.resolve("../amazon-capstone/image_mapping.csv").normalize();
        }

        System.out.println("Product Images Directory: " + baseImagesDir);
        System.out.println("Reading Products SQL:      " + sqlPath);

        List<ProductMeta> products = loadProducts(sqlPath, csvPath);
        System.out.println("Loaded " + products.size() + " product specifications.");

        if (products.isEmpty()) {
            System.err.println("No products loaded! Exiting.");
            return;
        }

        long startTime = System.currentTimeMillis();
        AtomicInteger generated = new AtomicInteger(0);

        final Path targetBaseImagesDir = baseImagesDir;

        // Process in parallel for speed
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
        System.out.println("SUCCESS! Generated " + generated.get() + " product images in " + elapsed + " ms.");
        System.out.println("Output Folder: " + baseImagesDir);
        System.out.println("=================================================");
    }

    private static List<ProductMeta> loadProducts(Path sqlPath, Path csvPath) {
        List<ProductMeta> list = new ArrayList<>();
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
                System.err.println("Warning: could not read CSV: " + e.getMessage());
            }
        }

        if (Files.exists(sqlPath)) {
            Pattern p = Pattern.compile("\\s*\\((\\d+),\\s*(\\d+),\\s*'([^']*)',\\s*'([^']*)',\\s*([0-9.]+),\\s*(\\d+),\\s*'([^']*)',\\s*'([^']*)'\\)");
            try (BufferedReader br = new BufferedReader(new FileReader(sqlPath.toFile()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    Matcher m = p.matcher(line);
                    if (m.find()) {
                        ProductMeta prod = new ProductMeta();
                        prod.id = Integer.parseInt(m.group(1));
                        prod.categoryId = Integer.parseInt(m.group(2));
                        prod.name = m.group(3);
                        prod.description = m.group(4);
                        prod.price = Double.parseDouble(m.group(5));
                        prod.stock = Integer.parseInt(m.group(6));
                        prod.sku = m.group(7);

                        String imgPath = m.group(8);
                        String[] parts = imgPath.split("/");
                        if (parts.length >= 3) {
                            prod.category = parts[parts.length - 2];
                            prod.filename = parts[parts.length - 1];
                        } else if (csvMap.containsKey(prod.id)) {
                            prod.category = csvMap.get(prod.id)[0];
                            prod.filename = csvMap.get(prod.id)[1];
                        } else {
                            prod.category = "general";
                            prod.filename = String.format("product_%04d.jpg", prod.id);
                        }

                        list.add(prod);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing seed SQL: " + e.getMessage());
            }
        }

        if (list.size() < 1000 && !csvMap.isEmpty()) {
            Set<Integer> existing = new HashSet<>();
            for (ProductMeta pm : list) existing.add(pm.id);

            for (Map.Entry<Integer, String[]> entry : csvMap.entrySet()) {
                if (!existing.contains(entry.getKey())) {
                    ProductMeta prod = new ProductMeta();
                    prod.id = entry.getKey();
                    prod.category = entry.getValue()[0];
                    prod.filename = entry.getValue()[1];
                    prod.name = "Premium " + capitalize(prod.category) + " Item (Model " + String.format("%04d", prod.id) + ")";
                    prod.price = 49.99;
                    prod.stock = 50;
                    prod.sku = "SKU-" + prod.category.substring(0, Math.min(3, prod.category.length())).toUpperCase() + "-" + String.format("%05d", prod.id);
                    list.add(prod);
                }
            }
        }

        return list;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
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
        float radius = 320f;
        Point2D center = new Point2D.Float(width / 2.0f, 240f);
        float[] dist = {0.0f, 0.5f, 1.0f};
        Color[] glowColors = {
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60),
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 15),
                new Color(0, 0, 0, 0)
        };
        RadialGradientPaint pGlow = new RadialGradientPaint(center, radius, dist, glowColors);
        g2.setPaint(pGlow);
        g2.fillRect(0, 0, width, height);

        // Subtle background grid lines
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
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(width / 2 - 190, pedY + 40, 380, 50);

        GradientPaint pedGrad = new GradientPaint(width / 2 - 160, pedY, new Color(255, 255, 255, 25), width / 2 + 160, pedY + 30, new Color(255, 255, 255, 5));
        g2.setPaint(pedGrad);
        g2.fillOval(width / 2 - 160, pedY + 15, 320, 45);
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));
        g2.drawOval(width / 2 - 160, pedY + 15, 320, 45);

        // 3. Central Category Vector Artwork
        drawCategoryArtwork(g2, p.category, width / 2, 230, accent, lightAccent, p.id);

        // 4. Top Header Banner
        // Left Pill: Category Tag
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRoundRect(24, 24, 180, 32, 16, 16);
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 180));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(24, 24, 180, 32, 16, 16);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        String icon = CAT_ICONS.getOrDefault(p.category, "★");
        String catTag = icon + "  " + p.category.toUpperCase().replace("-", " & ");
        g2.drawString(catTag, 36, 45);

        // Right Pill: Official BuyIt Badge
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRoundRect(width - 170, 24, 146, 32, 16, 16);
        g2.setColor(new Color(234, 179, 8, 160));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(width - 170, 24, 146, 32, 16, 16);

        g2.setColor(new Color(254, 240, 138));
        g2.drawString("★ BUYIT VERIFIED", width - 158, 45);

        // 5. Lower Frosted Showcase Card
        int cardX = 24;
        int cardY = 410;
        int cardW = width - 48;
        int cardH = 166;

        g2.setColor(new Color(15, 23, 42, 235));
        g2.fillRoundRect(cardX, cardY, cardW, cardH, 20, 20);

        GradientPaint cardBorder = new GradientPaint(cardX, cardY, accent, cardX + cardW, cardY, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
        g2.setPaint(cardBorder);
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRoundRect(cardX, cardY, cardW, cardH, 20, 20);

        // Model & SKU Pills
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
        g2.fillRoundRect(cardX + 16, cardY + 16, 110, 22, 10, 10);
        g2.setColor(lightAccent);
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.drawString(String.format("MODEL #%04d", p.id), cardX + 24, cardY + 31);

        g2.setColor(new Color(255, 255, 255, 30));
        g2.fillRoundRect(cardX + 134, cardY + 16, 130, 22, 10, 10);
        g2.setColor(new Color(203, 213, 225));
        g2.setFont(new Font("Monospaced", Font.BOLD, 10));
        g2.drawString(p.sku != null ? p.sku : ("SKU-" + p.id), cardX + 142, cardY + 31);

        // Rating Stars
        g2.setColor(new Color(251, 191, 36));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.drawString("★★★★★", cardW - 60, cardY + 32);

        // Product Name
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
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
        String priceStr = String.format("$%.2f", p.price > 0 ? p.price : 49.99);
        g2.drawString(priceStr, cardX + 16, cardY + 124);

        // Stock & Genuine Tag
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(new Color(16, 185, 129));
        g2.fillOval(cardX + 16, cardY + 138, 8, 8);
        g2.setColor(new Color(148, 163, 184));
        g2.drawString("IN STOCK • 100% GENUINE", cardX + 30, cardY + 146);

        // Right Warranty Seal
        int sealX = cardX + cardW - 145;
        int sealY = cardY + 98;
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 35));
        g2.fillRoundRect(sealX, sealY, 130, 48, 12, 12);
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(sealX, sealY, 130, 48, 12, 12);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("OFFICIAL WARRANTY", sealX + 12, sealY + 20);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        g2.setColor(lightAccent);
        g2.drawString("1 YEAR REPLACEMENT", sealX + 12, sealY + 36);

        g2.dispose();
        return img;
    }

    private static void drawCategoryArtwork(Graphics2D g, String category, int cx, int cy, Color accent, Color lightAccent, int id) {
        switch (category) {
            case "electronics":
                drawElectronics(g, cx, cy, accent, lightAccent, id);
                break;
            case "mobiles":
                drawMobiles(g, cx, cy, accent, lightAccent, id);
                break;
            case "laptops":
                drawLaptops(g, cx, cy, accent, lightAccent, id);
                break;
            case "headphones":
                drawHeadphones(g, cx, cy, accent, lightAccent, id);
                break;
            case "clothing":
                drawClothing(g, cx, cy, accent, lightAccent, id);
                break;
            case "shoes":
                drawShoes(g, cx, cy, accent, lightAccent, id);
                break;
            case "watches":
                drawWatches(g, cx, cy, accent, lightAccent, id);
                break;
            case "bags":
                drawBags(g, cx, cy, accent, lightAccent, id);
                break;
            case "home-kitchen":
                drawHomeKitchen(g, cx, cy, accent, lightAccent, id);
                break;
            case "beauty":
                drawBeauty(g, cx, cy, accent, lightAccent, id);
                break;
            case "books":
                drawBooks(g, cx, cy, accent, lightAccent, id);
                break;
            case "toys":
                drawToys(g, cx, cy, accent, lightAccent, id);
                break;
            case "sports":
                drawSports(g, cx, cy, accent, lightAccent, id);
                break;
            case "grocery":
                drawGrocery(g, cx, cy, accent, lightAccent, id);
                break;
            case "appliances":
                drawAppliances(g, cx, cy, accent, lightAccent, id);
                break;
            default:
                drawGeneric(g, cx, cy, accent, lightAccent, id);
                break;
        }
    }

    private static void drawElectronics(Graphics2D g, int cx, int cy, Color acc, Color light, int id) {
        int w = 150, h = 180;
        GradientPaint bodyGrad = new GradientPaint(cx - w / 2, cy - h / 2, new Color(40, 48, 68), cx + w / 2, cy + h / 2, new Color(15, 23, 42));
        g.setPaint(bodyGrad);
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 40, 40);
        g.setColor(acc);
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 40, 40);

        g.setColor(new Color(255, 255, 255, 30));
        for (int row = -50; row <= 50; row += 16) {
            for (int col = -45; col <= 45; col += 16) {
                g.fillOval(cx + col, cy + row, 6, 6);
            }
        }

        g.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 80));
        g.fillOval(cx - 42, cy - 42, 84, 84);
        g.setColor(light);
        g.setStroke(new BasicStroke(3f));
        g.drawOval(cx - 32, cy - 32, 64, 64);

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.5f));
        g.drawArc(cx - 14, cy - 14, 28, 28, -60, 300);
        g.drawLine(cx, cy - 18, cx, cy - 6);

        g.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 160));
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(cx - 105, cy - 40, 40, 80, 120, 120);
        g.drawArc(cx - 120, cy - 60, 50, 120, 120, 120);
        g.drawArc(cx + 65, cy - 40, 40, 80, -60, 120);
        g.drawArc(cx + 70, cy - 60, 50, 120, -60, 120);
    }

    private static void drawMobiles(Graphics2D g, int cx, int cy, Color acc, Color light, int id) {
        int w = 120, h = 210;
        g.setColor(new Color(15, 23, 42));
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 36, 36);
        g.setColor(new Color(75, 85, 99));
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 36, 36);

        int sw = w - 14, sh = h - 14;
        GradientPaint screenGrad = new GradientPaint(cx - sw / 2, cy - sh / 2, acc, cx + sw / 2, cy + sh / 2, new Color(30, 27, 75));
        g.setPaint(screenGrad);
        g.fillRoundRect(cx - sw / 2, cy - sh / 2, sw, sh, 30, 30);

        g.setColor(Color.BLACK);
        g.fillRoundRect(cx - 18, cy - sh / 2 + 6, 36, 12, 6, 6);

        g.setColor(new Color(255, 255, 255, 50));
        g.setStroke(new BasicStroke(12f));
        g.drawArc(cx - 40, cy - 30, 80, 110, 45, 180);

        g.setColor(new Color(255, 255, 255, 180));
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                g.fillRoundRect(cx - 32 + c * 24, cy + 10 + r * 22, 16, 16, 5, 5);
            }
        }

        g.setColor(Color.WHITE);
        g.fillRoundRect(cx - 24, cy + sh / 2 - 12, 48, 4, 2, 2);
    }

    private static void drawLaptops(Graphics2D g, int cx, int cy, Color acc, Color light, int id) {
        int sw = 200, sh = 125;
        g.setColor(new Color(15, 23, 42));
        g.fillRoundRect(cx - sw / 2, cy - sh + 20, sw, sh, 14, 14);
        g.setColor(new Color(100, 116, 139));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(cx - sw / 2, cy - sh + 20, sw, sh, 14, 14);

        GradientPaint dispGrad = new GradientPaint(cx - sw / 2, cy - sh + 30, acc, cx + sw / 2, cy + 10, new Color(15, 23, 42));
        g.setPaint(dispGrad);
        g.fillRect(cx - sw / 2 + 8, cy - sh + 28, sw - 16, sh - 20);

        g.setColor(new Color(255, 255, 255, 160));
        g.fillRect(cx - sw / 2 + 20, cy - sh + 42, 60, 6);
        g.fillRect(cx - sw / 2 + 20, cy - sh + 56, 110, 5);
        g.fillRect(cx - sw / 2 + 35, cy - sh + 68, 85, 5);
        g.fillRect(cx - sw / 2 + 35, cy - sh + 80, 95, 5);
        g.setColor(light);
        g.fillRect(cx - sw / 2 + 20, cy - sh + 94, 50, 5);

        int bw1 = 230, bw2 = 250, bh = 55;
        Polygon base = new Polygon();
        base.addPoint(cx - bw1 / 2, cy + 20);
        base.addPoint(cx + bw1 / 2, cy + 20);
        base.addPoint(cx + bw2 / 2, cy + 20 + bh);
        base.addPoint(cx - bw2 / 2, cy + 20 + bh);
        g.setColor(new Color(51, 65, 85));
        g.fillPolygon(base);
        g.setColor(new Color(148, 163, 184));
        g.drawPolygon(base);

        g.setColor(new Color(30, 41, 59));
        g.fillRoundRect(cx - 85, cy + 26, 170, 24, 4, 4);

        g.setColor(new Color(71, 85, 105));
        g.fillRoundRect(cx - 28, cy + 54, 56, 16, 4, 4);
    }

    private static void drawHeadphones(Graphics2D g, int cx, int cy, Color acc, Color light, int id) {
        g.setColor(new Color(55, 65, 81));
        g.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(cx - 85, cy - 105, 170, 160, 20, 140);

        g.setColor(acc);
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(cx - 65, cy - 100, 130, 120, 35, 110);

        int cw = 44, ch = 80;
        g.setColor(new Color(31, 41, 55));
        g.fillRoundRect(cx - 95, cy - 20, cw, ch, 28, 28);
        g.setColor(acc);
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(cx - 95, cy - 20, cw, ch, 28, 28);
        g.setColor(new Color(17, 24, 39));
        g.fillRoundRect(cx - 88, cy - 12, cw - 14, ch - 16, 20, 20);

        g.setColor(new Color(31, 41, 55));
        g.fillRoundRect(cx + 95 - cw, cy - 20, cw, ch, 28, 28);
        g.setColor(acc);
        g.drawRoundRect(cx + 95 - cw, cy - 20, cw, ch, 28, 28);
        g.setColor(new Color(17, 24, 39));
        g.fillRoundRect(cx + 95 - cw + 7, cy - 12, cw - 14, ch - 16, 20, 20);

        g.setColor(new Color(234, 179, 8));
        g.fillOval(cx - 82, cy - 24, 16, 16);
        g.fillOval(cx + 66, cy - 24, 16, 16);
    }

    private static void drawClothing(Graphics2D g, int cx, int cy, Color acc, Color light, int id) {
        Polygon shirt = new Polygon();
        shirt.addPoint(cx - 70, cy - 70);
        shirt.addPoint(cx - 105, cy - 30);
        shirt.addPoint(cx - 85, cy + 75);
        shirt.addPoint(cx + 85, cy + 75);
        shirt.addPoint(cx + 105, cy - 30);
        shirt.addPoint(cx + 70, cy - 70);
        shirt.addPoint(cx, cy - 40);

        GradientPaint shirtGrad = new GradientPaint(cx - 80, cy - 70, acc, cx + 80, cy + 75, new Color(30, 27, 75));
        g.setPaint(shirtGrad);
        g.fillPolygon(shirt);
        g.setColor(light);
        g.setStroke(new BasicStroke(2.5f));
        g.drawPolygon(shirt);

        Polygon collarL = new Polygon(new int[]{cx - 70, cx, cx - 25}, new int[]{cy - 70, cy - 40, cy - 15}, 3);
        Polygon collarR = new Polygon(new int[]{cx + 70, cx, cx + 25}, new int[]{cy - 70, cy - 40, cy - 15}, 3);
        g.setColor(new Color(255, 255, 255, 180));
        g.fillPolygon(collarL);
        g.fillPolygon(collarR);

        g.setColor(new Color(0, 0, 0, 80));
        g.fillRect(cx - 10, cy - 15, 20, 55);
        g.setColor(Color.WHITE);
        g.fillOval(cx - 3, cy - 5, 6, 6);
        g.fillOval(cx - 3, cy + 12, 6, 6);
        g.fillOval(cx - 3, cy + 28, 6, 6);

        g.setColor(new Color(234, 179, 8));
        g.drawString("★", cx + 35, cy + 5);
    }

    private static void drawShoes(Graphics2D g, int cx, int cy, Color acc, Color light, int id) {
        Path2D.Double shoe = new Path2D.Double();
        shoe.moveTo(cx - 110, cy + 30);
        shoe.curveTo(cx - 115, cy - 35, cx - 70, cy - 55, cx - 35, cy - 45);
        shoe.curveTo(cx - 10, cy - 30, cx + 30, cy - 15, cx + 75, cy + 5);
        shoe.curveTo(cx + 110, cy + 15, cx + 120, cy + 30, cx + 110, cy + 45);
        shoe.lineTo(cx - 110, cy + 45);
        shoe.closePath();

        GradientPaint shoeGrad = new GradientPaint(cx - 100, cy - 40, acc, cx + 100, cy + 40, new Color(15, 23, 42));
        g.setPaint(shoeGrad);
        g.fill(shoe);
        g.setColor(light);
        g.setStroke(new BasicStroke(2.5f));
        g.draw(shoe);

        g.setColor(Color.WHITE);
        g.fillRoundRect(cx - 115, cy + 35, 232, 22, 10, 10);
        g.setColor(new Color(30, 41, 59));
        g.fillRect(cx - 115, cy + 50, 232, 6);

        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double swoosh = new Path2D.Double();
        swoosh.moveTo(cx - 75, cy + 15);
        swoosh.curveTo(cx - 30, cy + 25, cx + 20, cy + 5, cx + 65, cy - 5);
        g.draw(swoosh);

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.5f));
        for (int i = 0; i < 4; i++) {
            g.drawLine(cx - 20 + i * 15, cy - 25 + i * 8, cx - 8 + i * 15, cy - 20 + i * 8);
        }
    }

    private static void drawWatches(Graphics2D g, int cx, int cy, Color acc, Color light, int id) {
        g.setColor(new Color(38, 20, 11));
        g.fillRoundRect(cx - 32, cy - 105, 64, 60, 8, 8);
        g.fillRoundRect(cx - 32, cy + 45, 64, 60, 8, 8);
        g.setColor(new Color(234, 179, 8, 120));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(cx - 32, cy - 105, 64, 60, 8, 8);
        g.drawRoundRect(cx - 32, cy + 45, 64, 60, 8, 8);

        int dia = 130;
        GradientPaint goldGrad = new GradientPaint(cx - dia / 2, cy - dia / 2, new Color(254, 240, 138), cx + dia / 2, cy + dia / 2, new Color(161, 98, 7));
        g.setPaint(goldGrad);
        g.fillOval(cx - dia / 2, cy - dia / 2, dia, dia);

        int dialDia = 108;
        g.setColor(new Color(15, 23, 42));
        g.fillOval(cx - dialDia / 2, cy - dialDia / 2, dialDia, dialDia);

        g.setColor(new Color(255, 255, 255, 30));
        g.fillOval(cx - 30, cy - 12, 24, 24);
        g.fillOval(cx + 6, cy - 12, 24, 24);
        g.fillOval(cx - 12, cy + 12, 24, 24);

        g.setColor(new Color(254, 240, 138));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx, cy - 46, cx, cy - 36);
        g.drawLine(cx, cy + 36, cx, cy + 46);
        g.drawLine(cx - 46, cy, cx - 36, cy);
        g.drawLine(cx + 36, cy, cx + 46, cy);

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx, cy, cx + 24, cy - 20);
        g.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx, cy, cx - 18, cy - 14);
        g.setColor(acc);
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(cx, cy, cx, cy + 30);

        g.setColor(new Color(234, 179, 8));
        g.fillOval(cx - 4, cy - 4, 8, 8);
    }

    private static void drawBags(Graphics2D g, int cx, int cy, Color acc, Color light, int id) {
        g.setColor(new Color(69, 26, 3));
        g.setStroke(new BasicStroke(8f));
        g.drawArc(cx - 26, cy - 90, 52, 40, 0, 180);

        int w = 140, h = 170;
        GradientPaint bagGrad = new GradientPaint(cx - w / 2, cy - h / 2, acc, cx + w / 2, cy + h / 2, new Color(40, 20, 10));
        g.setPaint(bagGrad);
        g.fillRoundRect(cx - w / 2, cy - h / 2 + 10, w, h, 45, 45);
        g.setColor(light);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - w / 2, cy - h / 2 + 10, w, h, 45, 45);

        int pw = 100, ph = 75;
        g.setColor(new Color(69, 26, 3));
        g.fillRoundRect(cx - pw / 2, cy + 15, pw, ph, 20, 20);
        g.setColor(new Color(234, 179, 8));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(cx - pw / 2, cy + 15, pw, ph, 20, 20);

        g.setColor(new Color(203, 213, 225));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(cx - pw / 2 + 10, cy + 32, cx + pw / 2 - 10, cy + 32);
        g.fillOval(cx - pw / 2 + 25, cy + 29, 6, 6);

        g.setColor(new Color(217, 119, 6));
        g.fillRoundRect(cx - 18, cy - 35, 36, 20, 6, 6);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 8));
        g.drawString("BUYIT", cx - 14, cy - 22);
    }

    private static void drawHomeKitchen(Graphics2D g, int cx, int cy, Color acc, Color light, int id) {
        int pw = 140, ph = 60;
        GradientPaint panGrad = new GradientPaint(cx - pw / 2, cy - 10, new Color(40, 40, 40), cx + pw / 2, cy + ph, new Color(15, 15, 15));
        g.setPaint(panGrad);
        g.fillOval(cx - pw / 2 - 10, cy, pw, ph);
        g.setColor(acc);
        g.setStroke(new BasicStroke(3f));
        g.drawOval(cx - pw / 2 - 10, cy, pw, ph);

        g.setColor(new Color(60, 60, 60));
        g.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx + pw / 2 - 20, cy + 30, cx + pw / 2 + 65, cy + 45);
        g.setColor(new Color(244, 63, 94));
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx + pw / 2 + 25, cy + 38, cx + pw / 2 + 60, cy + 44);

        g.setColor(new Color(255, 255, 255, 120));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = -1; i <= 1; i++) {
            Path2D.Double steam = new Path2D.Double();
            steam.moveTo(cx - 20 + i * 35, cy);
            steam.curveTo(cx - 30 + i * 35, cy - 30, cx - 10 + i * 35, cy - 50, cx - 25 + i * 35, cy - 80);
            g.draw(steam);
        }

        Polygon knife = new Polygon();
        knife.addPoint(cx - 85, cy - 40);
        knife.addPoint(cx - 20, cy - 40);
        knife.addPoint(cx - 85, cy - 15);
        g.setColor(new Color(226, 232, 240));
        g.fillPolygon(knife);
        g.setColor(new Color(15, 23, 42));
        g.fillRect(cx - 20, cy - 38, 32, 10);
    }

    private static void drawBeauty(Graphics2D g, int cx, int cy, Color acc, Color light, int id) {
        int bw = 90, bh = 135;
        GradientPaint bottleGrad = new GradientPaint(cx - bw / 2, cy - bh / 2, acc, cx + bw / 2, cy + bh / 2, new Color(76, 5, 25));
        g.setPaint(bottleGrad);
        g.fillRoundRect(cx - bw / 2, cy - bh / 2 + 25, bw, bh, 28, 28);
        g.setColor(light);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - bw / 2, cy - bh / 2 + 25, bw, bh, 28, 28);

        g.setColor(new Color(234, 179, 8));
        g.fillRect(cx - 18, cy - bh / 2 + 5, 36, 20);
        g.fillRoundRect(cx - 12, cy - bh / 2 - 15, 24, 20, 8, 8);

        int lw = 62, lh = 65;
        g.setColor(new Color(255, 255, 255, 220));
        g.fillRoundRect(cx - lw / 2, cy - 10, lw, lh, 8, 8);
        g.setColor(new Color(76, 5, 25));
        g.setFont(new Font("Serif", Font.BOLD, 10));
        g.drawString("ELIXIR", cx - 18, cy + 12);
        g.setFont(new Font("SansSerif", Font.PLAIN, 7));
        g.drawString("PURE ESSENCE", cx - 26, cy + 28);

        g.setColor(new Color(254, 240, 138));
        drawStar(g, cx - 65, cy - 45, 12);
        drawStar(g, cx + 65, cy - 15, 16);
        drawStar(g, cx + 55, cy + 60, 10);
    }

    private static void drawBooks(Graphics2D g, int cx, int cy, Color acc, Color light, int id) {
        int bw = 130, bh = 175;
        GradientPaint bookGrad = new GradientPaint(cx - bw / 2, cy - bh / 2, acc, cx + bw / 2, cy + bh / 2, new Color(54, 16, 36));
        g.setPaint(bookGrad);
        g.fillRoundRect(cx - bw / 2 + 10, cy - bh / 2, bw, bh, 14, 14);
        g.setColor(new Color(234, 179, 8));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(cx - bw / 2 + 10, cy - bh / 2, bw, bh, 14, 14);

        g.setColor(new Color(255, 255, 255, 40));
        g.fillRect(cx - bw / 2 + 10, cy - bh / 2, 22, bh);

        g.setColor(new Color(254, 240, 138));
        g.fillRect(cx + bw / 2 + 8, cy - bh / 2 + 10, 12, bh - 20);

        g.setColor(new Color(234, 179, 8));
        g.drawRoundRect(cx - bw / 2 + 42, cy - bh / 2 + 25, 75, 110, 8, 8);
        g.setFont(new Font("Serif", Font.BOLD, 12));
        g.drawString("CLASSIC", cx - bw / 2 + 50, cy - bh / 2 + 65);
        g.drawString("EDITION", cx - bw / 2 + 50, cy - bh / 2 + 85);

        g.setColor(new Color(220, 38, 38));
        int[] rx = {cx + 20, cx + 32, cx + 26, cx + 20};
        int[] ry = {cy + bh / 2 - 10, cy + bh / 2 - 10, cy + bh / 2 + 20, cy + bh / 2 + 14};
        g.fillPolygon(rx, ry, 4);
    }

    private static void drawToys(Graphics2D g, int cx, int cy, Color acc, Color light, int id) {
        int w = 175, h = 110;
        GradientPaint padGrad = new GradientPaint(cx - w / 2, cy - h / 2, acc, cx + w / 2, cy + h / 2, new Color(15, 23, 42));
        g.setPaint(padGrad);
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 50, 50);
        g.setColor(light);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 50, 50);

        g.setColor(new Color(30, 41, 59));
        g.fillRect(cx - 56, cy - 20, 14, 40);
        g.fillRect(cx - 70, cy - 6, 42, 14);

        g.setColor(new Color(239, 68, 68));
        g.fillOval(cx + 45, cy - 20, 12, 12);
        g.setColor(new Color(34, 197, 94));
        g.fillOval(cx + 45, cy + 8, 12, 12);
        g.setColor(new Color(59, 130, 246));
        g.fillOval(cx + 31, cy - 6, 12, 12);
        g.setColor(new Color(234, 179, 8));
        g.fillOval(cx + 59, cy - 6, 12, 12);

        g.setColor(new Color(51, 65, 85));
        g.fillOval(cx - 28, cy + 10, 26, 26);
        g.fillOval(cx + 2, cy + 10, 26, 26);
        g.setColor(acc);
        g.drawOval(cx - 28, cy + 10, 26, 26);
        g.drawOval(cx + 2, cy + 10, 26, 26);

        g.setColor(Color.WHITE);
        g.fillOval(cx - 7, cy - 16, 14, 14);
    }

    private static void drawSports(Graphics2D g, int cx, int cy, Color acc, Color light, int id) {
        g.setColor(new Color(203, 213, 225));
        g.fillRect(cx - 65, cy - 8, 130, 16);
        g.setColor(new Color(100, 116, 139));
        for (int i = -35; i <= 35; i += 6) {
            g.drawLine(cx + i, cy - 8, cx + i, cy + 8);
        }

        drawHexPlate(g, cx - 65, cy, 38, acc, light);
        drawHexPlate(g, cx - 85, cy, 46, acc, light);

        drawHexPlate(g, cx + 65, cy, 38, acc, light);
        drawHexPlate(g, cx + 85, cy, 46, acc, light);

        g.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 120));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(cx - 100, cy - 70, 200, 140, 20, 140);
    }

    private static void drawHexPlate(Graphics2D g, int x, int y, int radius, Color acc, Color light) {
        Polygon hex = new Polygon();
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3;
            hex.addPoint((int) (x + radius * Math.cos(angle)), (int) (y + radius * Math.sin(angle)));
        }
        g.setColor(new Color(30, 41, 59));
        g.fillPolygon(hex);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2.5f));
        g.drawPolygon(hex);
    }

    private static void drawGrocery(Graphics2D g, int cx, int cy, Color acc, Color light, int id) {
        int fw = 110, fh = 150;
        GradientPaint fruitGrad = new GradientPaint(cx - fw / 2, cy - fh / 2, acc, cx + fw / 2, cy + fh / 2, new Color(6, 78, 59));
        g.setPaint(fruitGrad);
        g.fillOval(cx - fw / 2, cy - fh / 2 + 10, fw, fh);
        g.setColor(light);
        g.setStroke(new BasicStroke(2.5f));
        g.drawOval(cx - fw / 2, cy - fh / 2 + 10, fw, fh);

        g.setColor(new Color(180, 83, 9));
        g.fillOval(cx - 28, cy + 8, 56, 68);
        g.setColor(new Color(245, 158, 11));
        g.setStroke(new BasicStroke(2f));
        g.drawOval(cx - 28, cy + 8, 56, 68);

        Path2D.Double leaf = new Path2D.Double();
        leaf.moveTo(cx, cy - fh / 2 + 10);
        leaf.curveTo(cx - 40, cy - fh / 2 - 25, cx - 35, cy - fh / 2 - 60, cx, cy - fh / 2 - 65);
        leaf.curveTo(cx + 35, cy - fh / 2 - 60, cx + 40, cy - fh / 2 - 25, cx, cy - fh / 2 + 10);
        g.setColor(new Color(34, 197, 94));
        g.fill(leaf);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(cx, cy - fh / 2 + 8, cx, cy - fh / 2 - 60);

        g.setColor(new Color(255, 255, 255, 220));
        g.fillOval(cx - 14, cy - fh / 2 - 35, 8, 8);
    }

    private static void drawAppliances(Graphics2D g, int cx, int cy, Color acc, Color light, int id) {
        int w = 150, h = 180;
        GradientPaint appGrad = new GradientPaint(cx - w / 2, cy - h / 2, new Color(51, 65, 85), cx + w / 2, cy + h / 2, new Color(15, 23, 42));
        g.setPaint(appGrad);
        g.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 35, 35);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 35, 35);

        g.setColor(new Color(15, 23, 42));
        g.fillRoundRect(cx - w / 2 + 15, cy - 10, w - 30, 95, 20, 20);
        g.setColor(new Color(255, 255, 255, 30));
        g.drawRoundRect(cx - w / 2 + 15, cy - 10, w - 30, 95, 20, 20);

        g.setColor(Color.BLACK);
        g.fillRoundRect(cx - w / 2 + 20, cy - h / 2 + 18, w - 40, 42, 12, 12);
        g.setColor(acc);
        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.drawString("375°F  25M", cx - 48, cy - h / 2 + 45);

        g.setColor(new Color(148, 163, 184));
        g.fillOval(cx - 18, cy + 30, 36, 36);
        g.setColor(acc);
        g.setStroke(new BasicStroke(2.5f));
        g.drawOval(cx - 18, cy + 30, 36, 36);
        g.drawLine(cx, cy + 30, cx, cy + 42);
    }

    private static void drawGeneric(Graphics2D g, int cx, int cy, Color acc, Color light, int id) {
        g.setColor(acc);
        g.fillOval(cx - 60, cy - 60, 120, 120);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        g.drawString("★", cx - 24, cy + 18);
    }

    private static void drawStar(Graphics2D g, int cx, int cy, int size) {
        int half = size / 2;
        g.drawLine(cx - half, cy, cx + half, cy);
        g.drawLine(cx, cy - half, cx, cy + half);
        g.drawLine(cx - half / 2, cy - half / 2, cx + half / 2, cy + half / 2);
        g.drawLine(cx - half / 2, cy + half / 2, cx + half / 2, cy - half / 2);
    }
}
