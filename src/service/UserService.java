package service;

import exception.UserNotFoundException;
import java.util.ArrayList;
import java.util.List;
import model.User;

public class UserService {
    private final List<User> users = new ArrayList<>();

    public void addUser(User user) { users.add(user); }
    public List<User> getAllUsers() { return List.copyOf(users); }

    public User getUserById(int id) {
        return users.stream().filter(user -> user.getId() == id).findFirst()
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public User authenticate(String email, String password) {
        return users.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email) && u.getPassword().equals(password))
                .findFirst().orElse(null);
    }

    public User getByEmail(String email) {
        return users.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst().orElse(null);
    }

    public boolean emailExists(String email) {
        return users.stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
    }

    public int nextId() {
        return users.stream().mapToInt(User::getId).max().orElse(0) + 1;
    }
}
