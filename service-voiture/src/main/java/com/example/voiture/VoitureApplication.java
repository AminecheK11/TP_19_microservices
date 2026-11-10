package com.example.voiture;
import com.example.voiture.services.VoitureService;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableFeignClients
public class VoitureApplication {
    public static void main(String[] args) { SpringApplication.run(VoitureApplication.class, args); }
    @Bean CommandLineRunner initialiserVoitures(VoitureService service) {
        return args -> service.initialiserDonnees();
    }
}
