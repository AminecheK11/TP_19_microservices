package com.example.voiture.controllers;
import com.example.voiture.models.*;
import com.example.voiture.services.VoitureService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/voitures")
public class VoitureController {
    private final VoitureService service;
    public VoitureController(VoitureService service) { this.service = service; }
    @GetMapping public List<VoitureDetails> findAll() { return service.findAll(); }
    @GetMapping("/{id}") public VoitureDetails findById(@PathVariable Long id) { return service.findById(id); }
    @GetMapping("/client/{id}") public List<VoitureDetails> findByClient(@PathVariable Long id) {
        return service.findByClientId(id);
    }
    @PostMapping("/{clientId}")
    public ResponseEntity<VoitureDetails> create(@PathVariable Long clientId, @Valid @RequestBody VoitureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(clientId, request));
    }
    @PutMapping("/{id}")
    public VoitureDetails update(@PathVariable Long id, @Valid @RequestBody VoitureRequest request) {
        return service.update(id, request);
    }
}
