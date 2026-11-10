package com.example.client.controllers;

import com.example.client.entities.Client;
import com.example.client.services.ClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class ClientController {
    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @GetMapping("/clients")
    public List<Client> findAll() {
        return service.findAll();
    }

    @GetMapping("/client/{id}")
    public ResponseEntity<Client> findById(@PathVariable Long id) {
        return ResponseEntity.of(service.findById(id));
    }
}
