package com.dev.parking.controller;

import com.dev.parking.dto.EntryRequest;
import com.dev.parking.entity.Ticket;
import com.dev.parking.entity.TicketStatus;
import com.dev.parking.repository.TicketRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketRepository repo;

    public TicketController(TicketRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/entry")
    public ResponseEntity<?> entry(@RequestBody EntryRequest req) {
        if (req.getPlate() == null || req.getPlate().isBlank())
            return ResponseEntity.badRequest().body("Plate is required");
        if (req.getType() == null)
            return ResponseEntity.badRequest().body("Vehicle type is required");

        boolean alreadyOpen = repo.existsByPlateAndStatus(req.getPlate(), TicketStatus.OPEN);
        if (alreadyOpen) return ResponseEntity.badRequest().body("Ticket already OPEN for this plate");

        Ticket t = new Ticket();
        t.setPlate(req.getPlate().trim().toUpperCase());
        t.setVehicleType(req.getType());
        t.setStatus(TicketStatus.OPEN);
        t.setEntryTime(LocalDateTime.from(Instant.now()));

        return ResponseEntity.ok(repo.save(t));
    }
}
