package controller;

import model.User;
import service.AuthService;
import util.Session;

/**
 * 登录与退出。
 *
 * <p>对应需求报告 R-001：登录成功后把用户写入全局 {@link Session}，
 * 退出时清空——务必清空，否则再次登录会残留上一次的权限状态。
 */
public class AuthController {

    private final AuthService authService = new AuthService();

    /**
     * @return 登录成功返回用户对象，失败返回 null
     */
    public User login(String username, String password) {
        User user = authService.authenticate(username, password);
        if (user != null) {
            Session.login(user);
        }
        return user;
    }

    public void logout() {
        Session.logout();
    }

    /** 当前登录用户。以 Session 为唯一事实来源，避免两处状态不一致 */
    public User getCurrentUser() {
        return Session.currentUser();
    }
}
