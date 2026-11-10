package com.example.voiture.clients;
import com.example.voiture.models.Client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "SERVICE-CLIENT")
public interface ClientFeignClient {
    @GetMapping("/client/{id}")
    Client findById(@PathVariable("id") Long id);
}
