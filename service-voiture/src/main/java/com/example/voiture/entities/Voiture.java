package com.example.voiture.entities;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor
public class Voiture {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String marque;
    private String modele;
    @Column(unique = true, nullable = false)
    private String matricule;
    @Column(nullable = false)
    private Long clientId;

    public Voiture(String marque, String modele, String matricule, Long clientId) {
        this.marque = marque; this.modele = modele; this.matricule = matricule; this.clientId = clientId;
    }
}
