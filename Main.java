import exception.ProductNotFoundException;
import model.Customer;
import model.Product;
import model.Seller;
import service.ProductService;
import service.UserService;
import util.InputUtil;

public class Main {
    public static void main(String[] args) {
        UserService userService = new UserService();
        ProductService productService = new ProductService();
        seedData(userService, productService);

        System.out.println("Welcome to ShopSphere!");
        boolean running = true;
        while (running) {
            System.out.println("\n1. View products\n2. Add product\n3. Find product\n4. View users\n0. Exit");
            switch (InputUtil.readInt("Choose an option: ")) {
                case 1 -> productService.getAllProducts().forEach(System.out::println);
                case 2 -> addProduct(productService);
                case 3 -> findProduct(productService);
                case 4 -> userService.getAllUsers().forEach(System.out::println);
                case 0 -> running = false;
                default -> System.out.println("Please choose a valid option.");
            }
        }
        InputUtil.close();
        System.out.println("Thanks for visiting ShopSphere!");
    }

    private static void addProduct(ProductService productService) {
        int id = InputUtil.readInt("Product ID: ");
        String name = InputUtil.readLine("Name: ");
        double price = InputUtil.readDouble("Price: ");
        int quantity = InputUtil.readInt("Quantity: ");
        productService.addProduct(new Product(id, name, price, quantity));
        System.out.println("Product added.");
    }

    private static void findProduct(ProductService productService) {
        try {
            System.out.println(productService.getProductById(InputUtil.readInt("Product ID: ")));
        } catch (ProductNotFoundException exception) {
            System.out.println(exception.getMessage());
        }
    }

    private static void seedData(UserService userService, ProductService productService) {
        userService.addUser(new Customer(1, "Asha", "asha@example.com", "Mumbai"));
        userService.addUser(new Seller(2, "Ravi", "ravi@example.com", "Gadgets"));
        productService.addProduct(new Product(101, "Wireless Mouse", 799.0, 12));
        productService.addProduct(new Product(102, "USB-C Cable", 299.0, 25));
    }
}
