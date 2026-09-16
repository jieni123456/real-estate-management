package controller;

import model.Customer;
import service.CustomerService;
import util.Permissions;
import util.Session;

import javax.swing.JOptionPane;
import java.util.List;

public class CustomerController {

    private final CustomerService customerService = new CustomerService();

    public boolean addCustomer(String id, String name, String phone, String requirements) {
        System.out.println("添加客户: " + id);
        Customer customer = new Customer(id, name, phone, requirements);
        return customerService.saveCustomer(customer);
    }

    public List<Object[]> getAllCustomers() {
        System.out.println("获取所有客户信息");
        return customerService.getAllCustomers();
    }

    /**
     * 删除客户。需 ADMIN 权限（R-001：AGENT 可增可查但不能删）。
     *
     * <p>界面层已把无权用户的删除按钮置灰，这里是第二道防线——防止绕过界面
     * 直接调用本方法。
     */
    public boolean deleteCustomer(String customerId) {
        if (!Session.can(Permissions.CUSTOMER_DELETE)) {
            denyDelete();
            return false;
        }
        System.out.println("删除客户: " + customerId);
        return customerService.deleteCustomer(customerId);
    }

    /** 供界面层判断是否启用「删除客户」按钮 */
    public boolean canDelete() {
        return Session.can(Permissions.CUSTOMER_DELETE);
    }

    /** 无权限时的兜底提示，正常情况下不会触发 */
    private void denyDelete() {
        System.err.println("[权限不足] " + Session.currentUserLabel() + " 尝试删除客户，已拦截");
        JOptionPane.showMessageDialog(null,
                "权限不足：当前账号（" + Session.currentRoleName() + "）没有删除客户的权限。",
                "权限不足", JOptionPane.WARNING_MESSAGE);
    }
}
