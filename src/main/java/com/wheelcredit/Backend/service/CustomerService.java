package com.wheelcredit.Backend.service;

import com.wheelcredit.Backend.model.Customer;

import java.time.LocalDate;
import java.util.List;

public interface CustomerService {
    Customer save(Customer customer, Long ClientId); // Método para guardar un nuevo cliente
    Customer update(Long id, Customer customer); // Método para actualizar un cliente existente
    void delete(Long id); // Método para eliminar un cliente
    Customer findById(Long id); // Método para encontrar un cliente por su ID
    List<Customer> findAll(); // Método para encontrar todos los clientes
    boolean existsByEmail(String email); // Método para verificar si un cliente existe por su correo electrónico
    LocalDate getMaxPaymentDateForCurrentMonth(Long customerId); // Método para obtener la fecha máxima de pago para el mes actual
    LocalDate getMaxPaymentDateForMonth(LocalDate date, Long customerId);
}