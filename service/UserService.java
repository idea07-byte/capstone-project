package service;

import exception.UserNotFoundException;
import model.User;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private final List<User> users = new ArrayList<>();

    public void addUser(User user) { users.add(user); }
    public List<User> getAllUsers() { return List.copyOf(users); }
    public User getUserById(int id) {
        return users.stream().filter(user -> user.getId() == id).findFirst()
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
