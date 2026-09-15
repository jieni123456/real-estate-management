package controller;

import model.House;
import model.Landlord;
import service.HouseService;
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

    public boolean deleteHouse(String houseId) {
        System.out.println("删除房屋: " + houseId);
        return houseService.deleteHouse(houseId);
    }
}