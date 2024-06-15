package com.wheelcredit.Backend.repository;

import com.wheelcredit.Backend.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Boolean existsByEmail(String email);
    Client findById(long id);
    Client findByEmailAndPassword(String email, String password);
    Client findByEmail(String email);
}
