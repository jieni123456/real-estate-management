package controller;

import model.House;
import model.Landlord;
import service.HouseService;
import util.Permissions;
import util.Result;
import util.Session;
import util.Validators;

import java.util.List;

public class HouseController {

    private final HouseService houseService = new HouseService();

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

        if (houseService.existsHouse(house.getId())) {
            return Result.fail("房屋ID「" + house.getId() + "」已存在。请换一个ID，"
                    + "或选中该房屋后用「编辑房屋」修改它。");
        }

        // 房东已存在时沿用原信息，不覆盖
        boolean landlordExisted = houseService.existsLandlord(house.getLandlord().getId());

        if (!houseService.insertHouse(house)) {
            return Result.fail("保存失败，请检查数据库连接后重试。");
        }
        return landlordExisted
                ? Result.ok("房屋添加成功（房东「" + house.getLandlord().getId()
                        + "」已存在，沿用其原有信息）")
                : Result.ok("房屋添加成功");
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

        if (!houseService.existsHouse(house.getId())) {
            return Result.fail("房屋「" + house.getId() + "」已不存在，可能已被其他人删除。");
        }

        boolean landlordExisted = houseService.existsLandlord(house.getLandlord().getId());

        if (!houseService.updateHouse(house)) {
            return Result.fail("保存失败，请检查数据库连接后重试。");
        }
        return landlordExisted
                ? Result.ok("房屋已更新（房东「" + house.getLandlord().getId()
                        + "」的原有信息未被覆盖）")
                : Result.ok("房屋已更新");
    }

    // ---------------------------------------------------------------- 删除

    /**
     * 删除房屋。需 ADMIN 权限（R-001：AGENT 可增可查但不能删）。
     *
     * <p>界面层已把无权用户的删除按钮置灰，这里是第二道防线——防止绕过界面直接调用。
     */
    public Result deleteHouse(String houseId) {
        if (!Session.can(Permissions.HOUSE_DELETE)) {
            System.err.println("[权限不足] " + Session.currentUserLabel() + " 尝试删除房屋，已拦截");
            // 不在此处弹窗——界面层会把 Result 的说明展示出来，两处都弹会重复提示
            return Result.fail("权限不足：当前账号（" + Session.currentRoleName()
                    + "）没有删除房屋的权限。");
        }

        return houseService.deleteHouse(houseId)
                ? Result.ok("房屋删除成功")
                : Result.fail("删除失败，请检查数据库连接后重试。");
    }

    // ---------------------------------------------------------------- 查询

    public List<House> getAllHouses() {
        System.out.println("获取所有房屋信息");
        return houseService.getAllHouses();
    }

    /** 供界面层判断是否启用「删除房屋」按钮 */
    public boolean canDelete() {
        return Session.can(Permissions.HOUSE_DELETE);
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

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
