package com.example.voiture.models;
public record VoitureDetails(Long id, String marque, String modele, String matricule, Long clientId, Client client) {}
