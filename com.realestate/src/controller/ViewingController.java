package controller;

import model.Customer;
import model.House;
import model.Viewing;
import service.CustomerService;
import service.HouseService;
import service.LogService;
import service.ViewingService;
import util.DataAccessException;
import util.Permissions;
import util.Result;
import util.Session;
import util.Validators;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 带看记录的业务入口。对应需求报告 G-008。
 *
 * <p>这是客户与房屋之间唯一的业务关联：谁在什么时候看了哪套房、结果如何。
 * 在此之前两张表完全孤立，系统无法回答「这个客户看过哪些房」这类最基本的问题。
 */
public class ViewingController {

    private final ViewingService viewingService = new ViewingService();
    // 对话框里的客户与房屋下拉需要这两份数据。跨 Service 复用比再写一遍查询更省事，
    // 也不必让界面同时持有三个控制器
    private final CustomerService customerService = new CustomerService();
    private final HouseService houseService = new HouseService();
    private final LogService logService = new LogService();

    // ---------------------------------------------------------------- 新增

    public Result addViewing(String customerId, String houseId, LocalDateTime viewedAt,
                             String result, String note) {
        Viewing viewing = build(Viewing.NEW_ID, customerId, houseId, viewedAt, result, note);

        String invalid = validate(viewing);
        if (invalid != null) {
            return Result.fail(invalid);
        }

        try {
            if (!viewingService.insertViewing(viewing)) {
                return Result.fail("保存失败：记录未写入。");
            }
            logService.record("新增带看", customerId + " → " + houseId, note);
            return Result.ok("带看记录添加成功");

        } catch (DataAccessException e) {
            return Result.fail(e.userMessage());
        }
    }

    // ---------------------------------------------------------------- 编辑

    public Result updateViewing(long id, String customerId, String houseId,
                                LocalDateTime viewedAt, String result, String note) {
        Viewing viewing = build(id, customerId, houseId, viewedAt, result, note);

        String invalid = validate(viewing);
        if (invalid != null) {
            return Result.fail(invalid);
        }

        try {
            if (!viewingService.updateViewing(viewing)) {
                return Result.fail("保存失败：该带看记录已不存在。");
            }
            logService.record("编辑带看", customerId + " → " + houseId, result);
            return Result.ok("带看记录已更新");

        } catch (DataAccessException e) {
            return Result.fail(e.userMessage());
        }
    }

    // ---------------------------------------------------------------- 删除

    /**
     * 删除带看记录。需 ADMIN 权限——与房屋、客户一致，删除是唯一被收回的操作。
     */
    public Result deleteViewing(long id) {
        if (!Session.can(Permissions.VIEWING_DELETE)) {
            System.err.println("[权限不足] " + Session.currentUserLabel() + " 尝试删除带看记录，已拦截");
            return Result.fail("权限不足：当前账号（" + Session.currentRoleName()
                    + "）没有删除带看记录的权限。");
        }

        try {
            if (!viewingService.deleteViewing(id)) {
                return Result.fail("删除失败：该带看记录已不存在。");
            }
            logService.record("删除带看", String.valueOf(id), "");
            return Result.ok("带看记录删除成功");

        } catch (DataAccessException e) {
            return Result.fail(e.userMessage());
        }
    }

    // ---------------------------------------------------------------- 查询

    /** 全部带看记录，按带看时间倒序。读取失败由界面层捕获提示 */
    public List<Viewing> getAllViewings() {
        return viewingService.getAllViewings();
    }

    /** 客户下拉的数据来源 */
    public List<Customer> getAllCustomers() {
        return customerService.getAllCustomers();
    }

    /** 房屋下拉的数据来源 */
    public List<House> getAllHouses() {
        return houseService.getAllHouses();
    }

    public boolean canDelete() {
        return Session.can(Permissions.VIEWING_DELETE);
    }

    /** 记录一次导出（G-014 / G-017） */
    public void recordExport(int count, String fileName) {
        logService.record("导出带看", fileName, "共 " + count + " 条");
    }

    // ---------------------------------------------------------------- 内部

    private Viewing build(long id, String customerId, String houseId, LocalDateTime viewedAt,
                          String result, String note) {
        // 客户姓名与房屋地址只是展示字段，写入时留空
        return new Viewing(id, trim(customerId), "", trim(houseId), "",
                viewedAt, trim(result), trim(note));
    }

    /** 校验通过返回 null，否则返回给用户看的原因 */
    private String validate(Viewing viewing) {
        String error = Validators.requiredText("客户", viewing.getCustomerId(), 50);
        if (error != null) {
            return error;
        }
        error = Validators.requiredText("房屋", viewing.getHouseId(), 50);
        if (error != null) {
            return error;
        }
        error = Validators.notFutureDate("带看时间", viewing.getViewedAt());
        if (error != null) {
            return error;
        }
        if (!Viewing.isValidResult(viewing.getResult())) {
            return "带看结果取值不合法，请从下拉列表中选择";
        }
        error = Validators.optionalText("备注", viewing.getNote(), 255);
        if (error != null) {
            return error;
        }

        // 关联存在性：外键也会拦，但驱动的报错对用户毫无意义，
        // 这里先查一次，给出「客户ID「xxx」不存在」这种人能看懂的说法
        try {
            if (!customerService.existsCustomer(viewing.getCustomerId())) {
                return "客户ID「" + viewing.getCustomerId() + "」不存在，请从下拉列表中选择";
            }
            if (!houseService.existsHouse(viewing.getHouseId())) {
                return "房屋ID「" + viewing.getHouseId() + "」不存在，请从下拉列表中选择";
            }
        } catch (DataAccessException e) {
            return e.userMessage();
        }
        return null;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
