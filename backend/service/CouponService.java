package service;

import db.Database;

import java.sql.*;

public class CouponService {

    public double validateAndApply(String code, double orderAmount) {
        String sql = "SELECT discount_type, discount_value, minimum_amount, maximum_discount, usage_limit, used_count, expiry_date, status FROM coupons WHERE code = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return -1;
                if (!"ACTIVE".equals(rs.getString("status"))) return -1;
                Timestamp expiry = rs.getTimestamp("expiry_date");
                if (expiry != null && expiry.before(new Timestamp(System.currentTimeMillis()))) return -1;
                double minAmount = rs.getDouble("minimum_amount");
                if (orderAmount < minAmount) return -1;
                int usageLimit = rs.getInt("usage_limit");
                int usedCount = rs.getInt("used_count");
                if (usageLimit > 0 && usedCount >= usageLimit) return -1;
                double discountValue = rs.getDouble("discount_value");
                double maxDiscount = rs.getDouble("maximum_discount");
                String type = rs.getString("discount_type");
                double discount;
                if ("PERCENTAGE".equals(type)) {
                    discount = orderAmount * discountValue / 100;
                    if (maxDiscount > 0) discount = Math.min(discount, maxDiscount);
                } else {
                    discount = discountValue;
                }
                try (PreparedStatement upStmt = conn.prepareStatement("UPDATE coupons SET used_count = used_count + 1 WHERE code = ?")) {
                    upStmt.setString(1, code);
                    upStmt.executeUpdate();
                }
                return Math.min(discount, orderAmount);
            }
        } catch (SQLException e) { throw new RuntimeException("Failed to validate coupon: " + e.getMessage(), e); }
    }
}
