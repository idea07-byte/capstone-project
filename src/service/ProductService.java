package service;

import java.util.ArrayList;
import java.util.List;
import model.Product;

public class ProductService {
    private final List<Product> products = new ArrayList<>();

    public void addProduct(Product product) { products.add(product); }

    public List<Product> getAllProducts() { return List.copyOf(products); }

    public Product getProductById(int id) {
        return products.stream().filter(product -> product.getId() == id).findFirst().orElse(null);
    }
}
