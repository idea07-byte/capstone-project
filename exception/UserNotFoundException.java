package exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(int userId) {
        super("No user found with ID: " + userId);
    }
}
