package controller;

import model.Customer;
import service.CustomerService;
import util.Permissions;
import util.Result;
import util.Session;
import util.Validators;

import java.util.List;

public class CustomerController {

    private final CustomerService customerService = new CustomerService();

    // ---------------------------------------------------------------- 新增

    /**
     * 新增客户。
     *
     * <p>对应需求报告 G-001：ID 已存在时<b>明确报错并拒绝</b>，绝不覆盖原记录。
     */
    public Result addCustomer(String id, String name, String phone, String requirements) {
        Customer customer = buildCustomer(id, name, phone, requirements);

        String invalid = validate(customer);
        if (invalid != null) {
            return Result.fail(invalid);
        }

        if (customerService.existsCustomer(customer.getId())) {
            return Result.fail("客户ID「" + customer.getId() + "」已存在。请换一个ID，"
                    + "或选中该客户后用「编辑客户」修改它。");
        }

        if (!customerService.insertCustomer(customer)) {
            return Result.fail("保存失败，请检查数据库连接后重试。");
        }
        return Result.ok("客户添加成功");
    }

    // ---------------------------------------------------------------- 编辑

    /** 更新客户。对应需求报告 G-002。客户ID 不可修改 */
    public Result updateCustomer(String id, String name, String phone, String requirements) {
        Customer customer = buildCustomer(id, name, phone, requirements);

        String invalid = validate(customer);
        if (invalid != null) {
            return Result.fail(invalid);
        }

        if (!customerService.existsCustomer(customer.getId())) {
            return Result.fail("客户「" + customer.getId() + "」已不存在，可能已被其他人删除。");
        }

        if (!customerService.updateCustomer(customer)) {
            return Result.fail("保存失败，请检查数据库连接后重试。");
        }
        return Result.ok("客户已更新");
    }

    // ---------------------------------------------------------------- 删除

    /**
     * 删除客户。需 ADMIN 权限（R-001：AGENT 可增可查但不能删）。
     *
     * <p>界面层已把无权用户的删除按钮置灰，这里是第二道防线——防止绕过界面直接调用。
     */
    public Result deleteCustomer(String customerId) {
        if (!Session.can(Permissions.CUSTOMER_DELETE)) {
            System.err.println("[权限不足] " + Session.currentUserLabel() + " 尝试删除客户，已拦截");
            // 不在此处弹窗——界面层会把 Result 的说明展示出来，两处都弹会重复提示
            return Result.fail("权限不足：当前账号（" + Session.currentRoleName()
                    + "）没有删除客户的权限。");
        }

        return customerService.deleteCustomer(customerId)
                ? Result.ok("客户删除成功")
                : Result.fail("删除失败，请检查数据库连接后重试。");
    }

    // ---------------------------------------------------------------- 查询

    public List<Customer> getAllCustomers() {
        System.out.println("获取所有客户信息");
        return customerService.getAllCustomers();
    }

    /** 供界面层判断是否启用「删除客户」按钮 */
    public boolean canDelete() {
        return Session.can(Permissions.CUSTOMER_DELETE);
    }

    // ---------------------------------------------------------------- 内部

    private Customer buildCustomer(String id, String name, String phone, String requirements) {
        return new Customer(trim(id), trim(name), trim(phone), trim(requirements));
    }

    /** 校验通过返回 null，否则返回给用户看的原因 */
    private String validate(Customer customer) {
        String error = Validators.requiredText("客户ID", customer.getId(), 50);
        if (error != null) {
            return error;
        }
        error = Validators.requiredText("姓名", customer.getName(), 100);
        if (error != null) {
            return error;
        }
        error = Validators.phone("电话", customer.getPhone());
        if (error != null) {
            return error;
        }
        return Validators.optionalText("需求描述", customer.getRequirements(), 500);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
