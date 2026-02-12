package com.dev.parking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public String home() {
        return "Parking backend is running ✅";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
