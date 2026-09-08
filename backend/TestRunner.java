import db.Database;
import model.*;
import service.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TestRunner {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("     BuyIt Marketplace Automated Test Suite       ");
        System.out.println("==================================================");

        runTest("Database Connection & Table Integrity", TestRunner::testDatabase);
        runTest("Product Catalog & Search Operations", TestRunner::testProductCatalog);
        runTest("User Roles, Authentication & Polymorphism", TestRunner::testUsersAndAuth);
        runTest("Cart & Wishlist Operations", TestRunner::testCartAndWishlist);
        runTest("Transactional Order Placement & Atomic Stock", TestRunner::testOrderAndStock);
        runTest("JSON Serialization & Parsing Helpers", TestRunner::testJsonHelpers);

        System.out.println("==================================================");
        System.out.printf("Test Results: %d Passed, %d Failed (Total: %d)%n", passed, failed, passed + failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void runTest(String testName, TestCase test) {
        System.out.print("[TEST] " + testName + " ... ");
        try {
            test.run();
            System.out.println("PASSED ✓");
            passed++;
        } catch (Throwable t) {
            System.out.println("FAILED ✗");
            System.err.println("  Error: " + t.getMessage());
            t.printStackTrace(System.err);
            failed++;
        }
    }

    private static void testDatabase() throws Exception {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            assertNotNull("Connection should not be null", conn);
            assertTrue("Connection must be valid", conn.isValid(5));

            String[] requiredTables = {
                "users", "vendors", "categories", "brands", "products",
                "product_images", "orders", "order_items", "cart", "cart_items",
                "wishlist", "wishlist_items", "reviews", "notifications"
            };

            for (String table : requiredTables) {
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM " + table)) {
                    assertTrue("Table " + table + " must exist and return count", rs.next());
                }
            }
        }
    }

    private static void testProductCatalog() throws Exception {
        ProductService ps = new ProductService();
        List<Product> all = ps.getAllProducts();
        assertNotNull("Products list must not be null", all);
        assertTrue("Catalog must contain products", !all.isEmpty());

        Product p = ps.getProductById(all.get(0).getId());
        assertNotNull("Product by ID should exist", p);
        assertTrue("Product ID must match", p.getId() == all.get(0).getId());
        assertTrue("Product price must be non-negative", p.getPrice() >= 0);
        assertTrue("Product final price must be <= base price", p.getFinalPrice() <= p.getPrice());

        // Test search
        List<Product> searchResults = ps.searchProducts(null, null, null, null, null, null);
        assertTrue("Search without filters should return products", !searchResults.isEmpty());

        // Test stock checking
        boolean stockCheck = ps.hasSufficientStock(p.getId(), 0);
        assertTrue("hasSufficientStock with qty 0 should return true", stockCheck);
    }

    private static void testUsersAndAuth() throws Exception {
        UserService us = new UserService();
        List<User> users = us.getAllUsers();
        assertTrue("Users must be present in database", !users.isEmpty());

        User admin = us.getUserByEmail("admin@buyit.com");
        assertNotNull("Admin user admin@buyit.com must exist", admin);
        assertEquals("Admin role must be ADMIN", Role.ADMIN, admin.getRole());
        assertEquals("Admin password must match", "Admin@123", admin.getPassword());
        assertTrue("Admin must be instance of Admin class", admin instanceof Admin);

        User customer = us.getUserByEmail("customer@buyit.com");
        assertNotNull("Customer user customer@buyit.com must exist", customer);
        assertEquals("Customer role must be CUSTOMER", Role.CUSTOMER, customer.getRole());
        assertTrue("Customer must be instance of Customer class", customer instanceof Customer);

        User vendor = us.getUserByEmail("vendor1@buyit.com");
        assertNotNull("Vendor user vendor1@buyit.com must exist", vendor);
        assertEquals("Vendor role must be VENDOR", Role.VENDOR, vendor.getRole());
    }

    private static void testCartAndWishlist() throws Exception {
        UserService us = new UserService();
        User customer = us.getUserByEmail("customer@buyit.com");
        assertNotNull("Customer required for cart test", customer);
        int customerId = customer.getId();

        ProductService ps = new ProductService();
        List<Product> products = ps.getAllProducts();
        assertTrue("Products required for cart test", !products.isEmpty());
        Product testProduct = products.get(0);

        // Test Cart
        CartService cs = new CartService();
        cs.clearCart(customerId);
        cs.addToCart(customerId, testProduct.getId(), 2, testProduct.getFinalPrice());

        List<CartItem> cartItems = cs.getCartItems(customerId);
        assertTrue("Cart must contain 1 item", cartItems.size() == 1);
        assertEquals("Cart item product ID must match", testProduct.getId(), cartItems.get(0).getProductId());
        assertEquals("Cart item quantity must be 2", 2, cartItems.get(0).getQuantity());

        cs.clearCart(customerId);
        assertTrue("Cart must be empty after clear", cs.getCartItems(customerId).isEmpty());

        // Test Wishlist
        WishlistService ws = new WishlistService();
        ws.removeFromWishlist(customerId, testProduct.getId());
        boolean added = ws.addToWishlist(customerId, testProduct.getId());
        assertTrue("Adding to wishlist should return true", added);
        assertTrue("Product should be wishlisted", ws.isWishlisted(customerId, testProduct.getId()));

        ws.removeFromWishlist(customerId, testProduct.getId());
        assertTrue("Product should no longer be wishlisted", !ws.isWishlisted(customerId, testProduct.getId()));
    }

    private static void testOrderAndStock() throws Exception {
        UserService us = new UserService();
        User customer = us.getUserByEmail("customer@buyit.com");
        assertNotNull("Customer required for order test", customer);

        ProductService ps = new ProductService();
        List<Product> products = ps.getAllProducts();
        Product product = null;
        for (Product p : products) {
            if (p.getStockQuantity() >= 10) {
                product = p;
                break;
            }
        }
        assertNotNull("Must have a product with stock >= 10 for order test", product);

        int initialStock = product.getStockQuantity();
        int orderQty = 1;

        OrderService os = new OrderService();
        List<OrderItem> items = new ArrayList<>();
        OrderItem item = new OrderItem(product.getId(), product.getName(), product.getFinalPrice(), orderQty);
        item.setVendorId(product.getVendorId());
        item.setSubtotal(product.getFinalPrice() * orderQty);
        items.add(item);

        Order createdOrder = os.createOrder((Customer) customer, items);
        assertNotNull("Created order should not be null", createdOrder);
        assertTrue("Order ID must be > 0", createdOrder.getId() > 0);
        assertEquals("Order status must be PLACED", "PLACED", createdOrder.getOrderStatus());

        // Verify stock was reduced
        Product updatedProduct = ps.getProductById(product.getId());
        assertEquals("Product stock should be reduced by " + orderQty, initialStock - orderQty, updatedProduct.getStockQuantity());

        // Verify retrieve order by ID
        Order fetched = os.getOrderById(createdOrder.getId());
        assertNotNull("Fetched order by ID must exist", fetched);
        assertEquals("Fetched order total must match", createdOrder.getFinalAmount(), fetched.getFinalAmount(), 0.01);
        assertTrue("Fetched order must have line items", fetched.getItems() != null && !fetched.getItems().isEmpty());

        // Test rollback: attempt to order more than available stock
        List<OrderItem> excessiveItems = new ArrayList<>();
        OrderItem excessiveItem = new OrderItem(product.getId(), product.getName(), product.getFinalPrice(), 999999);
        excessiveItem.setVendorId(product.getVendorId());
        excessiveItem.setSubtotal(product.getFinalPrice() * 999999);
        excessiveItems.add(excessiveItem);

        boolean exceptionThrown = false;
        try {
            os.createOrder((Customer) customer, excessiveItems);
        } catch (Exception e) {
            exceptionThrown = true;
        }
        assertTrue("Ordering excessive stock must throw exception and rollback", exceptionThrown);
    }

    private static void testJsonHelpers() throws Exception {
        String sampleJson = "{\"name\":\"Wireless Mouse\",\"price\":499.50,\"count\":5,\"tags\":[\"tech\",\"accessories\"],\"active\":true}";

        String name = WebServer.jsonStr(sampleJson, "name");
        assertEquals("jsonStr must extract string correctly", "Wireless Mouse", name);

        int count = WebServer.parseInt(sampleJson, "count");
        assertEquals("parseInt must extract integer correctly", 5, count);

        double price = WebServer.parseDouble(sampleJson, "price");
        assertEquals("parseDouble must extract double correctly", 499.50, price, 0.001);

        List<String> tags = WebServer.jsonStrList(sampleJson, "tags");
        assertEquals("jsonStrList must extract 2 elements", 2, tags.size());
        assertEquals("Tag 1 must match", "tech", tags.get(0));
        assertEquals("Tag 2 must match", "accessories", tags.get(1));
    }

    // --- Assertions ---
    private static void assertTrue(String message, boolean condition) {
        if (!condition) throw new AssertionError("Assertion Failed: " + message);
    }

    private static void assertNotNull(String message, Object obj) {
        if (obj == null) throw new AssertionError("Assertion Failed: " + message + " (expected not null)");
    }

    private static void assertEquals(String message, Object expected, Object actual) {
        if (expected == null && actual == null) return;
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError("Assertion Failed: " + message + " [expected: " + expected + ", actual: " + actual + "]");
        }
    }

    private static void assertEquals(String message, double expected, double actual, double delta) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError("Assertion Failed: " + message + " [expected: " + expected + ", actual: " + actual + "]");
        }
    }

    @FunctionalInterface
    private interface TestCase {
        void run() throws Exception;
    }
}
