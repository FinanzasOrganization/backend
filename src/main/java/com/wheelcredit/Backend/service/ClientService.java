package com.wheelcredit.Backend.service;

import com.wheelcredit.Backend.dto.ClientDto;
import com.wheelcredit.Backend.model.Client;

public interface ClientService {
    public abstract Long save(Client client);

    public abstract Client update(Long id, ClientDto clientDto);

    public abstract void delete(Long id);

    public abstract Client findById(long id);

    public abstract Long login(Client client);
}
