package com.wheelcredit.Backend.controller;

import com.wheelcredit.Backend.model.Customer;
import com.wheelcredit.Backend.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/go-finance/v1/customer")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Transactional
    @PostMapping("/register/{clientId}")
    public ResponseEntity<Customer> createCustomer(@PathVariable(name = "clientId") Long clientId, @RequestBody Customer customer) {
        Customer savedCustomer =customerService.save(customer, clientId);
        return new ResponseEntity<>(savedCustomer, HttpStatus.CREATED);
    }

    @Transactional(readOnly = true)
    @GetMapping("/{customerId}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable(name = "customerId") Long customerId) {
        Customer customer = customerService.findById(customerId);
        if (customer == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(customer, HttpStatus.OK);
    }

    @Transactional
    @PutMapping("/{customerId}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable(name = "customerId") Long customerId, @RequestBody Customer customer) {
        Customer updatedCustomer = customerService.update(customerId, customer);
        if (updatedCustomer == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(updatedCustomer, HttpStatus.OK);
    }

    @Transactional
    @DeleteMapping("/{customerId}")
    public ResponseEntity<Customer> deleteCustomer(@PathVariable(name = "customerId") Long customerId) {
        customerService.delete(customerId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        List<Customer> customers = customerService.findAll();
        return new ResponseEntity<>(customers, HttpStatus.OK);
    }
}
