package controller;

import model.House;
import model.Landlord;
import service.HouseService;
import service.LogService;
import util.DataAccessException;
import util.Permissions;
import util.Result;
import util.Session;
import util.Validators;

import java.util.List;

/**
 * 房屋相关的业务入口。
 *
 * <p>对应需求报告：
 * <ul>
 *   <li>G-001  新增走纯 INSERT，ID 冲突明确报错，绝不覆盖原记录</li>
 *   <li>G-002  提供编辑入口（房屋 ID 为主键，不可修改）</li>
 *   <li>G-012  数据库异常转成用户能看懂的说明，且区分开「ID 已存在」与
 *       「数据库连不上」这类不同原因</li>
 *   <li>G-017  新增 / 编辑 / 删除 / 导出写操作日志</li>
 * </ul>
 *
 * <p>界面层已按权限把无权用户的删除按钮置灰，{@link #deleteHouse} 里仍会再查一次
 * 权限——这是第二道防线，防止绕过界面直接调用。
 */
public class HouseController {

    private final HouseService houseService = new HouseService();
    private final LogService logService = new LogService();

    // ---------------------------------------------------------------- 新增

    /**
     * 新增房屋。
     *
     * <p>对应需求报告 G-001：ID 已存在时<b>明确报错并拒绝</b>，绝不覆盖原记录。
     * 原先 DAO 用的是 INSERT ... ON DUPLICATE KEY UPDATE，会把已有房屋静默改写。
     */
    public Result addHouse(String id, String type, double area, String address,
                           String landlordId, String landlordName, String landlordContact) {
        House house = buildHouse(id, type, area, address, landlordId, landlordName, landlordContact);

        String invalid = validate(house);
        if (invalid != null) {
            return Result.fail(invalid);
        }

        try {
            if (houseService.existsHouse(house.getId())) {
                return Result.fail("房屋ID「" + house.getId() + "」已存在。请换一个ID，"
                        + "或选中该房屋后用「编辑房屋」修改它。");
            }

            // 房东已存在时沿用原信息，不覆盖
            boolean landlordExisted = houseService.existsLandlord(house.getLandlord().getId());

            if (!houseService.insertHouse(house)) {
                return Result.fail("保存失败：记录未写入。");
            }

            logService.record("新增房屋", house.getId(), house.getAddress());
            return landlordExisted
                    ? Result.ok("房屋添加成功（房东「" + house.getLandlord().getId()
                            + "」已存在，沿用其原有信息）")
                    : Result.ok("房屋添加成功");

        } catch (DataAccessException e) {
            return Result.fail(describe(e, "房屋"));
        }
    }

    // ---------------------------------------------------------------- 编辑

    /**
     * 更新房屋。对应需求报告 G-002。
     * 房屋ID 不可修改（界面上该字段为只读），因此这里用 ID 定位记录。
     */
    public Result updateHouse(String id, String type, double area, String address,
                              String landlordId, String landlordName, String landlordContact) {
        House house = buildHouse(id, type, area, address, landlordId, landlordName, landlordContact);

        String invalid = validate(house);
        if (invalid != null) {
            return Result.fail(invalid);
        }

        try {
            if (!houseService.existsHouse(house.getId())) {
                return Result.fail("房屋「" + house.getId() + "」已不存在，可能已被其他人删除。");
            }

            boolean landlordExisted = houseService.existsLandlord(house.getLandlord().getId());

            if (!houseService.updateHouse(house)) {
                return Result.fail("保存失败：记录未更新。");
            }

            logService.record("编辑房屋", house.getId(), house.getAddress());
            return landlordExisted
                    ? Result.ok("房屋已更新（房东「" + house.getLandlord().getId()
                            + "」的原有信息未被覆盖）")
                    : Result.ok("房屋已更新");

        } catch (DataAccessException e) {
            return Result.fail(describe(e, "房屋"));
        }
    }

    // ---------------------------------------------------------------- 删除

    /**
     * 删除房屋。需 ADMIN 权限（R-001：AGENT 可增可查但不能删）。
     */
    public Result deleteHouse(String houseId) {
        if (!Session.can(Permissions.HOUSE_DELETE)) {
            System.err.println("[权限不足] " + Session.currentUserLabel() + " 尝试删除房屋，已拦截");
            // 不在此处弹窗——界面层会把 Result 的说明展示出来，两处都弹会重复提示
            return Result.fail("权限不足：当前账号（" + Session.currentRoleName()
                    + "）没有删除房屋的权限。");
        }

        try {
            if (!houseService.deleteHouse(houseId)) {
                return Result.fail("删除失败：该房屋已不存在。");
            }
            logService.record("删除房屋", houseId, "");
            return Result.ok("房屋删除成功");

        } catch (DataAccessException e) {
            return Result.fail(describe(e, "房屋"));
        }
    }

    // ---------------------------------------------------------------- 查询

    /**
     * 全部房屋。
     *
     * <p>读取失败时<b>不</b>在控制器里吞掉异常——空列表与「数据库连不上」必须
     * 区分开，否则用户会以为数据丢了。由界面层捕获 {@link DataAccessException}
     * 并提示。
     */
    public List<House> getAllHouses() {
        System.out.println("获取所有房屋信息");
        return houseService.getAllHouses();
    }

    /** 房东列表，供「添加 / 编辑房屋」对话框的下拉选择使用（G-007） */
    public List<Landlord> getAllLandlords() {
        return houseService.getAllLandlords();
    }

    /** 供界面层判断是否启用「删除房屋」按钮 */
    public boolean canDelete() {
        return Session.can(Permissions.HOUSE_DELETE);
    }

    /** 记录一次导出（G-014 / G-017）。导出本身由界面层完成，这里只负责留痕 */
    public void recordExport(int count, String fileName) {
        logService.record("导出房屋", fileName, "共 " + count + " 条");
    }

    // ---------------------------------------------------------------- 内部

    private House buildHouse(String id, String type, double area, String address,
                             String landlordId, String landlordName, String landlordContact) {
        Landlord landlord = new Landlord(trim(landlordId), trim(landlordName), trim(landlordContact));
        return new House(trim(id), trim(type), area, trim(address), landlord);
    }

    /** 校验通过返回 null，否则返回给用户看的原因 */
    private String validate(House house) {
        String error = Validators.requiredText("房屋ID", house.getId(), 50);
        if (error != null) {
            return error;
        }
        error = Validators.requiredText("户型", house.getType(), 50);
        if (error != null) {
            return error;
        }
        error = Validators.positiveNumber("面积", house.getArea());
        if (error != null) {
            return error;
        }
        error = Validators.requiredText("地址", house.getAddress(), 255);
        if (error != null) {
            return error;
        }
        error = Validators.requiredText("房东ID", house.getLandlord().getId(), 50);
        if (error != null) {
            return error;
        }
        error = Validators.requiredText("房东姓名", house.getLandlord().getName(), 100);
        if (error != null) {
            return error;
        }
        return Validators.phone("房东电话", house.getLandlord().getContact());
    }

    /**
     * 把数据访问异常转成给用户的一句话（G-012）。
     * 主键冲突这一种给更贴业务的说法，其余用统一的分类说明。
     */
    private String describe(DataAccessException e, String subject) {
        if (e.getKind() == DataAccessException.Kind.DUPLICATE_KEY) {
            return "该" + subject + "ID 已存在，请换一个 ID。";
        }
        return e.userMessage();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
