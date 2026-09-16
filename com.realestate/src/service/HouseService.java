package service;

import dao.HouseDAO;
import model.House;

import java.util.List;

public class HouseService {

    private final HouseDAO houseDAO = new HouseDAO();

    public boolean insertHouse(House house) {
        System.out.println("新增房屋: " + house.getId());
        return houseDAO.insertHouse(house);
    }

    public boolean updateHouse(House house) {
        System.out.println("更新房屋: " + house.getId());
        return houseDAO.updateHouse(house);
    }

    public boolean existsHouse(String houseId) {
        return houseDAO.exists(houseId);
    }

    public boolean existsLandlord(String landlordId) {
        return houseDAO.landlordExists(landlordId);
    }

    public List<House> getAllHouses() {
        System.out.println("从DAO获取所有房屋");
        return houseDAO.getAllHouses();
    }

    public boolean deleteHouse(String houseId) {
        System.out.println("删除房屋: " + houseId);
        return houseDAO.deleteHouse(houseId);
    }
}
