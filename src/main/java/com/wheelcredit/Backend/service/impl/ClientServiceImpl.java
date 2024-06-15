package com.wheelcredit.Backend.service.impl;

import com.wheelcredit.Backend.dto.ClientDto;
import com.wheelcredit.Backend.exception.ResourceNotFoundException;
import com.wheelcredit.Backend.exception.ValidationException;
import com.wheelcredit.Backend.model.Client;
import com.wheelcredit.Backend.repository.ClientRepository;
import com.wheelcredit.Backend.service.ClientService;
import org.springframework.stereotype.Service;

@Service
public class ClientServiceImpl implements ClientService {
    ClientRepository clientRepository;

    public ClientServiceImpl(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public Long save(Client client) {
        validateClientoInfo(client);
        Client savedClient = clientRepository.save(client);
        savedClient.setImage("https://robohash.org/profile");
        savedClient.setDescription("This is my description");
        return savedClient.getId();
    }

    @Override
    public Client update(Long id, ClientDto clientDto) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
        updateClientFromDto(client, clientDto);
        return clientRepository.save(client);
    }

    @Override
    public void delete(Long id) {
        try {
            clientRepository.deleteById(id);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Client not found with id: " + id);
        }
    }

    @Override
    public Client findById(long id) {
        return clientRepository.findById(id);
    }

    @Override
    public Long login(Client client) {
        validateLoginInfo(client);
        Client savedClient = clientRepository.findByEmailAndPassword(client.getEmail(), client.getPassword());
        if (savedClient == null)
            throw new ResourceNotFoundException("Client not found with email: " + client.getEmail());
        return savedClient.getId();
    }

    private void validateClientoInfo(Client client) {
        if (clientRepository.existsByEmail(client.getEmail())) {
            throw new ValidationException("Email already exists");
        }
        if (client.getName().isEmpty()) {
            throw new ValidationException("Name is required");
        }
        if (client.getLastname().isEmpty()) {
            throw new ValidationException("Lastname is required");
        }
        if (client.getEmail().isEmpty()) {
            throw new ValidationException("Email is required");
        }
        if (client.getPassword().isEmpty()) {
            throw new ValidationException("Password is required");
        }
    }

    private void updateClientFromDto(Client client, ClientDto clientDto) {
        if (!clientDto.getName().isEmpty()) {
            client.setName(clientDto.getName());
        }
        if (!clientDto.getLastname().isEmpty()) {
            client.setLastname(clientDto.getLastname());
        }
        if (!clientDto.getEmail().isEmpty()) {
            if (clientRepository.existsByEmail(clientDto.getEmail())) {
                throw new ValidationException("Email already exists");
            }
            client.setEmail(clientDto.getEmail());
        }
        if (!clientDto.getPassword().isEmpty()) {
            client.setPassword(clientDto.getPassword());
        }
    }

    private void validateLoginInfo(Client client) {
        if (client.getEmail().isEmpty()) {
            throw new ValidationException("Email is required");
        }
        if (client.getPassword().isEmpty()) {
            throw new ValidationException("Password is required");
        }
    }
}
