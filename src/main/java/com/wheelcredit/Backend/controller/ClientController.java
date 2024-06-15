package com.wheelcredit.Backend.controller;

import com.wheelcredit.Backend.dto.ClientDto;
import com.wheelcredit.Backend.dto.ExistDto;
import com.wheelcredit.Backend.dto.LoginDto;
import com.wheelcredit.Backend.model.Client;
import com.wheelcredit.Backend.service.ClientService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/go-finance/v1/client")
public class ClientController {
    private final ClientService clientService;
    private final ModelMapper modelMapper;

    public ClientController(ClientService clientService, ModelMapper modelMapper) {
        this.clientService = clientService;
        this.modelMapper = modelMapper;
    }

    // URL: http://localhost:8090/api/wheel-credit/v1/client/signup
    // Method: POST
    @Transactional
    @PostMapping("signup")
    public ResponseEntity<Long> createClient(@RequestBody ClientDto clientDto) {
        return new ResponseEntity<Long>(clientService.save(modelMapper.map(clientDto, Client.class)), HttpStatus.CREATED);
    }

    // URL: http://localhost:8090/api/wheel-credit/v1/client/login
    // Method: POST
    @Transactional(readOnly = true)
    @PostMapping("login")
    public ResponseEntity<Long> login(@RequestBody LoginDto loginDto) {
        return new ResponseEntity<Long>(clientService.login(modelMapper.map(loginDto, Client.class)), HttpStatus.OK);
    }

    // URL: http://localhost:8090/api/wheel-credit/v1/client/{clientId}
    // Method: GET
    @Transactional(readOnly = true)
    @GetMapping("/{clientId}")
    public ResponseEntity<Client> getClientById(@PathVariable(name = "clientId") Long clientId) {
        return new ResponseEntity<>(clientService.findById(clientId), HttpStatus.OK);
    }

    // URL: http://localhost:8090/api/wheel-credit/v1/client
    // Method: GET
    @Transactional(readOnly = true)
    @GetMapping("")
    public ResponseEntity<ExistDto> getClientById() {
        var exist = new ExistDto();
        exist.setMessage("Hi client");
        return new ResponseEntity<>(exist, HttpStatus.OK);
    }

    // URL: http://localhost:8090/api/wheel-credit/v1/client/{clientId}
    // Method: PUT
    @Transactional
    @PutMapping("/{clientId}")
    public ResponseEntity<Client> updateClient(@PathVariable(name = "clientId") Long clientId, @RequestBody ClientDto clientDto) {
        return new ResponseEntity<>(clientService.update(clientId, clientDto), HttpStatus.OK);
    }

    // URL: http://localhost:8090/api/wheel-credit/v1/client/{clientId}
    // Method: DELETE
    @Transactional
    @DeleteMapping("/{clientId}")
    public ResponseEntity<Client> deleteClient(@PathVariable(name = "clientId") Long clientId) {
        clientService.delete(clientId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}