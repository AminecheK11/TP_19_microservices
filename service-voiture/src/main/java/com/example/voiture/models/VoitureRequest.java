package com.example.voiture.models;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// L'identifiant de voiture et le clientId ne sont jamais repris du corps JSON.
public record VoitureRequest(
    @NotBlank @Size(max = 255) String marque,
    @NotBlank @Size(max = 255) String modele,
    @NotBlank @Size(max = 255) String matricule) {}
