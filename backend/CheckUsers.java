import db.Database;
import java.sql.*;

public class CheckUsers {
    public static void main(String[] args) {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, email, password, role, status FROM users")) {
            System.out.println("Users in DB:");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.println(rs.getInt("id") + ": " + rs.getString("email") + " | " + rs.getString("password") + " | " + rs.getString("role"));
            }
            if (!found) {
                System.out.println("NO USERS FOUND IN DATABASE!");
            }
            for (String tbl : new String[]{"categories", "brands", "vendors", "products", "product_images", "orders", "order_items", "reviews", "wishlist_items", "cart_items"}) {
                try (ResultSet r = stmt.executeQuery("SELECT count(*) FROM " + tbl)) {
                    if (r.next()) System.out.println(tbl + ": " + r.getInt(1));
                } catch (Exception err) {
                    System.out.println(tbl + " error: " + err.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
