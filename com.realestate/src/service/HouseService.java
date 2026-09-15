package service;

import dao.HouseDAO;
import model.House;
import java.util.List;

public class HouseService {
    private final HouseDAO houseDAO = new HouseDAO();

    public boolean saveHouse(House house) {
        System.out.println("保存房屋: " + house.getId());
        return houseDAO.saveHouse(house);
    }

    public List<Object[]> getAllHouses() {
        System.out.println("从DAO获取所有房屋");
        return houseDAO.getAllHouses();
    }

    public boolean deleteHouse(String houseId) {
        System.out.println("删除房屋: " + houseId);
        return houseDAO.deleteHouse(houseId);
    }
}