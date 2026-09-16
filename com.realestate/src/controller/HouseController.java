package controller;

import model.House;
import model.Landlord;
import service.HouseService;
import util.Permissions;
import util.Session;

import javax.swing.JOptionPane;
import java.util.List;

public class HouseController {

    private final HouseService houseService = new HouseService();

    public boolean addHouse(String id, String type, double area, String address,
                            String landlordId, String landlordName, String landlordContact) {
        System.out.println("添加房屋: " + id);
        Landlord landlord = new Landlord(landlordId, landlordName, landlordContact);
        House house = new House(id, type, area, address, landlord);
        return houseService.saveHouse(house);
    }

    public List<Object[]> getAllHouses() {
        System.out.println("获取所有房屋信息");
        return houseService.getAllHouses();
    }

    /**
     * 删除房屋。需 ADMIN 权限（R-001：AGENT 可增可查但不能删）。
     *
     * <p>界面层已把无权用户的删除按钮置灰，这里是第二道防线——防止绕过界面
     * 直接调用本方法。
     */
    public boolean deleteHouse(String houseId) {
        if (!Session.can(Permissions.HOUSE_DELETE)) {
            denyDelete();
            return false;
        }
        System.out.println("删除房屋: " + houseId);
        return houseService.deleteHouse(houseId);
    }

    /** 供界面层判断是否启用「删除房屋」按钮 */
    public boolean canDelete() {
        return Session.can(Permissions.HOUSE_DELETE);
    }

    /** 无权限时的兜底提示，正常情况下不会触发 */
    private void denyDelete() {
        System.err.println("[权限不足] " + Session.currentUserLabel() + " 尝试删除房屋，已拦截");
        JOptionPane.showMessageDialog(null,
                "权限不足：当前账号（" + Session.currentRoleName() + "）没有删除房屋的权限。",
                "权限不足", JOptionPane.WARNING_MESSAGE);
    }
}
