package service;

import java.util.ArrayList;
import java.util.List;
import model.Role;
import model.User;

public class UserService {
    private final List<User> users = new ArrayList<>();

    public void addUser(User user) {
        if (getUserById(user.getId()) != null) {
            throw new IllegalArgumentException("User ID already exists: " + user.getId());
        }
        users.add(user);
    }

    public boolean removeUserById(int id) {
        return users.removeIf(user -> user.getId() == id);
    }

    public List<User> getAllUsers() { return List.copyOf(users); }

    public List<User> getUsersByRole(Role role) {
        return users.stream().filter(user -> user.getRole() == role).toList();
    }

    public User getUserById(int id) {
        return users.stream().filter(user -> user.getId() == id).findFirst().orElse(null);
    }
}
