package com.example.eurekaserver;

import com.netflix.discovery.EurekaClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EurekaServerApplicationTests {
    @Autowired private TestRestTemplate http;
    @Autowired private EurekaClientConfig config;

    @Test void standaloneServerDoesNotRegisterOrFetch() {
        assertThat(config.shouldRegisterWithEureka()).isFalse();
        assertThat(config.shouldFetchRegistry()).isFalse();
    }

    @Test void dashboardIsAvailable() {
        ResponseEntity<String> response = http.getForEntity("/", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Eureka", "No instances available");
    }

    @Test void registryIsAvailableAndEmpty() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        ResponseEntity<String> response = http.exchange("/eureka/apps", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"applications\"").doesNotContain("\"instance\"");
    }
}
