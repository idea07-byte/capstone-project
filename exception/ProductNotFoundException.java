package exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(int productId) {
        super("No product found with ID: " + productId);
    }
}
