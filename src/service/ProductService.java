package service;

import exception.ProductNotFoundException;
import model.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProductService {
    private final List<Product> products = new ArrayList<>();

    public void addProduct(Product product) { products.add(product); }

    public List<Product> getAllProducts() { return List.copyOf(products); }

    public Product getProductById(int id) {
        return products.stream().filter(product -> product.getId() == id).findFirst()
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public List<Product> getProductsBySellerId(int sellerId) {
        return products.stream().filter(p -> p.getSellerId() == sellerId).collect(Collectors.toList());
    }

    public List<Product> searchProducts(String query) {
        String lower = query.toLowerCase();
        return products.stream()
                .filter(p -> p.getName().toLowerCase().contains(lower)
                        || p.getCategory().toLowerCase().contains(lower)
                        || p.getDescription().toLowerCase().contains(lower))
                .collect(Collectors.toList());
    }

    public boolean updateProduct(int id, String name, double price, int quantity, String category, String description) {
        Product p = products.stream().filter(x -> x.getId() == id).findFirst().orElse(null);
        if (p == null) return false;
        p.setName(name);
        p.setPrice(price);
        p.setQuantity(quantity);
        p.setCategory(category);
        p.setDescription(description);
        return true;
    }

    public boolean reduceStock(int id, int amount) {
        Product p = products.stream().filter(x -> x.getId() == id).findFirst().orElse(null);
        if (p == null || p.getQuantity() < amount) return false;
        p.setQuantity(p.getQuantity() - amount);
        return true;
    }

    public boolean removeProduct(int id) {
        return products.removeIf(p -> p.getId() == id);
    }

    public int nextId() {
        return products.stream().mapToInt(Product::getId).max().orElse(0) + 1;
    }
}
