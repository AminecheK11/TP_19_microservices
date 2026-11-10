package com.example.gateway;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "eureka.client.enabled=false")
class GatewayApplicationTests {
    // Serveur HTTP indépendant réservé aux tests : aucune réponse locale dans la Gateway.
    static final AtomicReference<String> received = new AtomicReference<>();
    static final HttpServer upstream = startUpstream();

    static HttpServer startUpstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                received.set(exchange.getRequestURI().toString());
                byte[] body = ("upstream:" + exchange.getRequestURI()).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(exchange.getRequestURI().getPath().equals("/client/999") ? 404 : 200, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            server.start();
            return server;
        } catch (java.io.IOException ex) { throw new ExceptionInInitializerError(ex); }
    }

    @DynamicPropertySource
    static void upstreamAddress(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.SERVICE-CLIENT[0].uri",
                () -> "http://localhost:" + upstream.getAddress().getPort());
    }
    @AfterAll static void stopUpstream() { upstream.stop(0); }
    @Autowired TestRestTemplate http;
    @Autowired Environment environment;

    void assertForwarded(String path, int status) {
        received.set(null);
        var response = http.getForEntity(path, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getBody()).isEqualTo("upstream:" + path);
        assertThat(received.get()).isEqualTo(path);
    }

    @Test void servicePrefixIsRemoved() {
        received.set(null);
        var response = http.getForEntity("/SERVICE-CLIENT/clients", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("upstream:/clients");
        assertThat(received.get()).isEqualTo("/clients");
    }
    @Test void prefixedClientKeepsQuery() {
        received.set(null);
        var response = http.getForEntity("/SERVICE-CLIENT/client/1?detail=true", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(received.get()).isEqualTo("/client/1?detail=true");
    }
    @Test void lowercasePrefixDoesNotMatch() {
        received.set(null);
        assertThat(http.getForEntity("/service-client/clients", String.class).getStatusCode().value()).isEqualTo(404);
        assertThat(received.get()).isNull();
    }
    @Test void forwardsClients() { assertForwarded("/clients", 200); }
    @Test void forwardsNestedClientsAndQuery() { assertForwarded("/clients/example?name=Rabab", 200); }
    @Test void forwardsClientById() { assertForwarded("/client/1", 200); }
    @Test void preservesUpstream404() { assertForwarded("/client/999", 404); }
    @Test void discoveryAndLoadBalancedRoutesAreEnabled() {
        assertThat(environment.getProperty("spring.cloud.discovery.enabled", Boolean.class)).isTrue();
        for (int i = 0; i < 3; i++) {
            assertThat(environment.getProperty("spring.cloud.gateway.server.webmvc.routes[" + i + "].uri"))
                    .isEqualTo("lb://SERVICE-CLIENT");
        }
    }
    @Test void unmatchedPathIsNotForwarded() {
        received.set(null);
        assertThat(http.getForEntity("/unmatched", String.class).getStatusCode().value()).isEqualTo(404);
        assertThat(received.get()).isNull();
    }
}
