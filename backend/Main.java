import db.Database;
import java.io.IOException;
import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting BuyIt Marketplace...");
        Database.initialize();
        try {
            WebServer.startServer();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        openBrowser("http://localhost:8080");
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("Opening browser at: " + url);
            } else {
                System.out.println("Please visit: " + url);
            }
        } catch (IOException | URISyntaxException e) {
            System.out.println("Please visit: " + url);
        }
    }
}
