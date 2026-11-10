package com.example.voiture.services;
import com.example.voiture.clients.ClientFeignClient;
import com.example.voiture.entities.Voiture;
import com.example.voiture.models.*;
import com.example.voiture.repositories.VoitureRepository;
import feign.FeignException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class VoitureService {
    private final VoitureRepository repository;
    private final ClientFeignClient clients;
    public VoitureService(VoitureRepository repository, ClientFeignClient clients) {
        this.repository = repository; this.clients = clients;
    }
    public List<VoitureDetails> findAll() {
        return repository.findAll(Sort.by("id")).stream().map(this::enrichir).toList();
    }
    public VoitureDetails findById(Long id) { return enrichir(requireVoiture(id)); }
    public List<VoitureDetails> findByClientId(Long id) {
        Client client = requireClient(id);
        return repository.findByClientId(id).stream().map(v -> details(v, client)).toList();
    }
    @Transactional
    public VoitureDetails create(Long clientId, VoitureRequest request) {
        Client client = requireClient(clientId);
        if (repository.existsByMatricule(request.matricule()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Matricule déjà utilisé");
        Voiture voiture = repository.saveAndFlush(new Voiture(request.marque(), request.modele(), request.matricule(), clientId));
        return details(voiture, client);
    }
    @Transactional
    public VoitureDetails update(Long id, VoitureRequest request) {
        Voiture voiture = requireVoiture(id);
        Client client = requireClient(voiture.getClientId());
        if (repository.existsByMatriculeAndIdNot(request.matricule(), id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Matricule déjà utilisé");
        voiture.setMarque(request.marque());
        voiture.setModele(request.modele());
        voiture.setMatricule(request.matricule());
        return details(repository.saveAndFlush(voiture), client);
    }
    private Voiture requireVoiture(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Voiture introuvable : " + id));
    }
    private Client requireClient(Long id) {
        try { return clients.findById(id); }
        catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client associé introuvable : " + id, ex);
        } catch (FeignException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE-CLIENT indisponible", ex);
        }
    }
    private VoitureDetails enrichir(Voiture v) { return details(v, requireClient(v.getClientId())); }
    private VoitureDetails details(Voiture v, Client client) {
        return new VoitureDetails(v.getId(), v.getMarque(), v.getModele(), v.getMatricule(), v.getClientId(), client);
    }
    @Transactional
    public void initialiserDonnees() {
        ajouter("Renault", "Clio", "DEMO-001", 1L);
        ajouter("Dacia", "Logan", "DEMO-002", 2L);
        ajouter("Peugeot", "208", "DEMO-003", 3L);
    }
    private void ajouter(String marque, String modele, String matricule, Long clientId) {
        if (!repository.existsByMatricule(matricule))
            repository.save(new Voiture(marque, modele, matricule, clientId));
    }
}
