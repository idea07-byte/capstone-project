package service;

import java.util.ArrayList;
import java.util.List;
import model.Product;

public class ProductService {
    private final List<Product> products = new ArrayList<>();

    public void addProduct(Product product) {
        if (getProductById(product.getId()) != null) {
            throw new IllegalArgumentException("Product ID already exists: " + product.getId());
        }
        products.add(product);
    }

    public List<Product> getAllProducts() { return List.copyOf(products); }

    public Product getProductById(int id) {
        return products.stream().filter(product -> product.getId() == id).findFirst().orElse(null);
    }

    public boolean removeProductById(int id) {
        return products.removeIf(product -> product.getId() == id);
    }
}
