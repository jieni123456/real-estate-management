package controller;

import model.User;
import service.AuthService;

public class AuthController {
    private final AuthService authService = new AuthService();
    private User currentUser;

    public User login(String username, String password) {
        currentUser = authService.authenticate(username, password);
        return currentUser;
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }
}
