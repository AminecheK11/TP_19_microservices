package com.example.voiture.repositories;
import com.example.voiture.entities.Voiture;
import org.springframework.data.jpa.repository.JpaRepository;
public interface VoitureRepository extends JpaRepository<Voiture, Long> {
    java.util.List<Voiture> findByClientId(Long clientId);
    boolean existsByMatriculeAndIdNot(String matricule, Long id);
    boolean existsByMatricule(String matricule);
}
