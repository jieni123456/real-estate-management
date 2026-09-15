package controller;

import model.Customer;
import service.CustomerService;
import java.util.List;

public class CustomerController {
    private final CustomerService customerService = new CustomerService();

    public boolean addCustomer(String id, String name, String phone, String requirements) {
        Customer customer = new Customer(id, name, phone, requirements);
        return customerService.saveCustomer(customer);
    }

    public List<Object[]> getAllCustomers() {
        return customerService.getAllCustomers();
    }

    public boolean deleteCustomer(String customerId) {
        return customerService.deleteCustomer(customerId);
    }
}
