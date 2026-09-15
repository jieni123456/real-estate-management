package service;

import dao.CustomerDAO;
import model.Customer;
import java.util.List;

public class CustomerService {
    private final CustomerDAO customerDAO = new CustomerDAO();

    public boolean saveCustomer(Customer customer) {
        return customerDAO.saveCustomer(customer);
    }

    public List<Object[]> getAllCustomers() {
        return customerDAO.getAllCustomers();
    }

    public boolean deleteCustomer(String customerId) {
        return customerDAO.deleteCustomer(customerId);
    }
}