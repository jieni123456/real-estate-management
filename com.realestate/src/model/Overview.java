package model;

import java.util.List;

/**
 * 概览页所需的统计数据。对应需求报告 G-009。
 *
 * <p>说明：「最近添加」这一项在原始建议里出现过，但表结构中没有创建时间列
 * （houses / customers 只有业务字段），取不到真实值。与其用一个假的数据充数，
 * 这里改为展示「平均面积」与「户型分布」——同样能反映数据概况，且都可由现有字段算出。
 * 若日后确实需要「最近添加」，应给表加 created_at 列并另行登记为需求。
 */
public class Overview {

    private final int houseCount;
    private final int customerCount;
    private final int landlordCount;
    private final double averageArea;
    private final List<TypeCount> typeCounts;

    public Overview(int houseCount, int customerCount, int landlordCount,
                    double averageArea, List<TypeCount> typeCounts) {
        this.houseCount = houseCount;
        this.customerCount = customerCount;
        this.landlordCount = landlordCount;
        this.averageArea = averageArea;
        this.typeCounts = typeCounts == null ? List.of() : typeCounts;
    }

    /** 数据库不可用或查询失败时的空统计，避免界面出现 null 判断 */
    public static Overview empty() {
        return new Overview(0, 0, 0, 0, List.of());
    }

    public int getHouseCount() {
        return houseCount;
    }

    public int getCustomerCount() {
        return customerCount;
    }

    public int getLandlordCount() {
        return landlordCount;
    }

    public double getAverageArea() {
        return averageArea;
    }

    /** 各户型的房源数，按数量降序 */
    public List<TypeCount> getTypeCounts() {
        return typeCounts;
    }

    /** 单个户型的房源数 */
    public static class TypeCount {

        private final String type;
        private final int count;

        public TypeCount(String type, int count) {
            this.type = type;
            this.count = count;
        }

        public String getType() {
            return type;
        }

        public int getCount() {
            return count;
        }
    }
}
