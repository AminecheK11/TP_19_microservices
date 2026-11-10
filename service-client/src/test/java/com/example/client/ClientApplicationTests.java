package com.example.client;

import com.example.client.entities.Client;
import com.example.client.repositories.ClientRepository;
import com.example.client.services.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "eureka.client.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:clienttest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class ClientApplicationTests {
    @Autowired TestRestTemplate http;
    @Autowired ClientService service;
    @Autowired ClientRepository repository;

    @Test void listContainsTheThreeDemoClients() {
        var response = http.getForEntity("/clients", Client[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(Client::getNom)
                .containsExactly("Rabab SELIMANI", "Amal RAMI", "Samir SAFI");
        assertThat(response.getBody()).extracting(Client::getAge).containsExactly(23f, 22f, 22f);
    }

    @Test void firstClientIsAvailable() {
        var response = http.getForEntity("/client/1", Client.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getNom()).isEqualTo("Rabab SELIMANI");
    }

    @Test void missingClientReturns404() {
        assertThat(http.getForEntity("/client/999999", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test void repeatedInitializationDoesNotDuplicateClients() {
        service.initialiserDonnees();
        service.initialiserDonnees();
        assertThat(repository.count()).isEqualTo(3L);
    }

    @Test void actuatorReportsHealthy() {
        var response = http.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test void dataRestHasItsOwnRoute() {
        var response = http.getForEntity("/data-rest/clients", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("_embedded", "Rabab SELIMANI");
    }
}
