package com.wheelcredit.Backend.service.impl;

import com.wheelcredit.Backend.exception.ResourceNotFoundException;
import com.wheelcredit.Backend.model.Customer;
import com.wheelcredit.Backend.repository.CustomerRepository;
import com.wheelcredit.Backend.service.ClientService;
import com.wheelcredit.Backend.service.CustomerService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerServiceImpl implements CustomerService {
    CustomerRepository customerRepository;
    ClientService clientService;

    public CustomerServiceImpl(CustomerRepository customerRepository, ClientService clientService) {
        this.customerRepository = customerRepository;
        this.clientService = clientService;
    }

    @Override
    public Customer save(Customer customer, Long ClientId) {
        customer.setClient(clientService.findById(ClientId));
        return customerRepository.save(customer);
    }

    @Override
    public Customer update(Long customerId, Customer customer) {
        existsCustomerdByCustomerdId(customerId);
        customer.setId(customerId);
        customer.setClient(findById(customerId).getClient());
        return customerRepository.save(customer);
    }

    @Override
    public void delete(Long id) {
        try {
            customerRepository.deleteById(id);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Customer not found with id: " + id);
        }
    }

    @Override
    public Customer findById(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    @Override
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Override
    public boolean existsByEmail(String email) {
        return customerRepository.existsByEmail(email);
    }

    @Override
    public LocalDate getMaxPaymentDateForCurrentMonth(Long customerId) {
        return customerRepository.findById(customerId).get().getMonthlyPaymentDate();
    }

    @Override
    public LocalDate getMaxPaymentDateForMonth(LocalDate date, Long customerId) {
        Optional<Customer> customerOptional = customerRepository.findById(customerId);
        if (customerOptional.isPresent()) {
            Customer customer = customerOptional.get();
            return customer.getMonthlyPaymentDate().withYear(date.getYear()).withMonth(date.getMonthValue());
        }
        throw new IllegalArgumentException("Customer not found for id: " + customerId);
    }

    private void existsCustomerdByCustomerdId(Long cropFieldId) {
        if (!customerRepository.existsById(cropFieldId)) {
            throw new ResourceNotFoundException("Customer not found");
        }
    }
}
