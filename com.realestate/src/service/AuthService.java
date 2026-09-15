package service;


import dao.UserDAO;
import model.User;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();

    public User authenticate(String username, String password) {
        return userDAO.authenticate(username, password);
    }
}