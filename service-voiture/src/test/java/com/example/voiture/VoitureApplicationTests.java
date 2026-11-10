package com.example.voiture;

import com.example.voiture.entities.Voiture;
import com.example.voiture.models.VoitureDetails;
import com.example.voiture.repositories.VoitureRepository;
import com.example.voiture.services.VoitureService;
import com.example.voiture.clients.ClientFeignClient;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.*;
import org.springframework.cloud.openfeign.FeignClient;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "eureka.client.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:voituretest;DB_CLOSE_DELAY=-1"
})
class VoitureApplicationTests {
    static final CopyOnWriteArrayList<String> calls = new CopyOnWriteArrayList<>();
    static final HttpServer upstream = start();
    static HttpServer start() {
        try {
            var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/client/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                calls.add(path);
                int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                int status = id == 500 ? 500 : id > 3 ? 404 : 200;
                String[] names = {"", "Rabab SELIMANI", "Amal RAMI", "Samir SAFI"};
                String body = status == 200 ? "{\"id\":" + id + ",\"nom\":\"" + names[id] + "\",\"age\":" + (id == 1 ? 23 : 22) + "}" : "{}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                try (var out = exchange.getResponseBody()) { out.write(bytes); }
            });
            server.start(); return server;
        } catch (java.io.IOException ex) { throw new ExceptionInInitializerError(ex); }
    }
    @DynamicPropertySource static void discovery(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.SERVICE-CLIENT[0].uri",
                () -> "http://localhost:" + upstream.getAddress().getPort());
    }
    @AfterAll static void stop() { upstream.stop(0); }
    @Autowired TestRestTemplate http;
    @Autowired VoitureRepository repository;
    @Autowired VoitureService service;

    @Test void listUsesRealFeignHttpRequests() {
        calls.clear();
        var response = http.getForEntity("/voitures", VoitureDetails[].class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(3);
        assertThat(response.getBody()).extracting(v -> v.client().nom())
                .containsExactly("Rabab SELIMANI", "Amal RAMI", "Samir SAFI");
        assertThat(calls).contains("/client/1", "/client/2", "/client/3");
    }
    @Test void detailsContainClient() {
        var response = http.getForEntity("/voitures/1", VoitureDetails.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().modele()).isEqualTo("Clio");
        assertThat(response.getBody().client().nom()).isEqualTo("Rabab SELIMANI");
    }
    @Test void missingCarReturns404WithoutCallingFeign() {
        calls.clear();
        assertThat(http.getForEntity("/voitures/999999", String.class).getStatusCode().value()).isEqualTo(404);
        assertThat(calls).isEmpty();
    }
    @Test void missingAssociatedClientReturns404() {
        var car = repository.save(new Voiture("Test", "Absent", "TEST-404", 999L));
        try {
            var response = http.getForEntity("/voitures/" + car.getId(), String.class);
            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).contains("Client associé introuvable");
        } finally { repository.deleteById(car.getId()); }
    }
    @Test void upstreamFailureReturns503() {
        var car = repository.save(new Voiture("Test", "Erreur", "TEST-503", 500L));
        try {
            assertThat(http.getForEntity("/voitures/" + car.getId(), String.class).getStatusCode().value()).isEqualTo(503);
        } finally { repository.deleteById(car.getId()); }
    }
    @Test void initializationDoesNotDuplicate() {
        service.initialiserDonnees(); service.initialiserDonnees();
        assertThat(repository.count()).isEqualTo(3);
    }
    @Test void feignHasServiceNameAndNoFixedUrl() {
        var annotation = ClientFeignClient.class.getAnnotation(FeignClient.class);
        assertThat(annotation.name()).isEqualTo("SERVICE-CLIENT");
        assertThat(annotation.url()).isEmpty();
    }

    @Test void clientFilterChecksClientAndReturnsOnlyHisCars() {
        calls.clear();
        var response = http.getForEntity("/voitures/client/1", VoitureDetails[].class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].clientId()).isEqualTo(1);
        assertThat(calls).contains("/client/1");
        assertThat(http.getForEntity("/voitures/client/999", String.class).getStatusCode().value()).isEqualTo(404);
    }
    org.springframework.http.HttpEntity<String> json(String body) {
        var headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new org.springframework.http.HttpEntity<>(body, headers);
    }
    @Test void createUsesUrlClientAndUpdatePreservesIt() {
        var response = http.postForEntity("/voitures/1", json(
            "{\"marque\":\"Test\",\"modele\":\"Modele\",\"matricule\":\"TEST-NEW\",\"clientId\":999,\"id\":999}"), VoitureDetails.class);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        Long id = response.getBody().id();
        try {
            assertThat(response.getBody().clientId()).isEqualTo(1);
            assertThat(response.getBody().client().nom()).isEqualTo("Rabab SELIMANI");
            var update = http.exchange("/voitures/" + id, org.springframework.http.HttpMethod.PUT,
                json("{\"marque\":\"Modifiee\",\"modele\":\"Nouveau\",\"matricule\":\"TEST-UPDATED\",\"clientId\":2}"),
                VoitureDetails.class);
            assertThat(update.getStatusCode().value()).isEqualTo(200);
            assertThat(update.getBody().clientId()).isEqualTo(1);
            assertThat(repository.findById(id).orElseThrow().getMarque()).isEqualTo("Modifiee");
            assertThat(repository.findById(id).orElseThrow().getMatricule()).isEqualTo("TEST-UPDATED");
        } finally { repository.deleteById(id); }
    }
    @Test void missingClientPostDoesNotPersist() {
        long before = repository.count();
        var response = http.postForEntity("/voitures/999", json(
            "{\"marque\":\"Test\",\"modele\":\"Test\",\"matricule\":\"TEST-MISSING\"}"), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(repository.count()).isEqualTo(before);
    }
    @Test void invalidBodiesReturn400() {
        assertThat(http.postForEntity("/voitures/1", json("{\"marque\":\" \"}"), String.class)
            .getStatusCode().value()).isEqualTo(400);
        assertThat(http.exchange("/voitures/1", org.springframework.http.HttpMethod.PUT,
            json("{"), String.class).getStatusCode().value()).isEqualTo(400);
        assertThat(repository.findById(1L).orElseThrow().getMatricule()).isEqualTo("DEMO-001");
    }
    @Test void missingCarPutReturns404() {
        assertThat(http.exchange("/voitures/999999", org.springframework.http.HttpMethod.PUT,
            json("{\"marque\":\"Test\",\"modele\":\"Test\",\"matricule\":\"TEST-NO-CAR\"}"),
            String.class).getStatusCode().value()).isEqualTo(404);
    }
    @Test void duplicateMatriculeReturns409() {
        assertThat(http.postForEntity("/voitures/1", json(
            "{\"marque\":\"Test\",\"modele\":\"Test\",\"matricule\":\"DEMO-001\"}"), String.class)
            .getStatusCode().value()).isEqualTo(409);
    }

}