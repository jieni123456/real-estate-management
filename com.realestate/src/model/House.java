package model;

public class House {
    private String id;
    private String type;
    private double area;
    private String address;
    private Landlord landlord;

    public House(String id, String type, double area, String address, Landlord landlord) {
        this.id = id;
        this.type = type;
        this.area = area;
        this.address = address;
        this.landlord = landlord;
    }

    // Getters
    public String getId() { return id; }
    public String getType() { return type; }
    public double getArea() { return area; }
    public String getAddress() { return address; }
    public Landlord getLandlord() { return landlord; }
}