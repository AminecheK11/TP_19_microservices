package com.example.client.services;

import com.example.client.entities.Client;
import com.example.client.repositories.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ClientService {
    private final ClientRepository repository;

    public ClientService(ClientRepository repository) {
        this.repository = repository;
    }

    public List<Client> findAll() {
        return repository.findAll(Sort.by("id"));
    }

    public Optional<Client> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public void initialiserDonnees() {
        ajouterSiAbsent("Rabab SELIMANI", 23f);
        ajouterSiAbsent("Amal RAMI", 22f);
        ajouterSiAbsent("Samir SAFI", 22f);
    }

    private void ajouterSiAbsent(String nom, Float age) {
        if (!repository.existsByNom(nom)) {
            repository.save(new Client(nom, age));
        }
    }
}
