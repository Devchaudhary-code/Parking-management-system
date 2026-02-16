package com.dev.parking.controller;

import com.dev.parking.entity.Ticket;
import com.dev.parking.entity.TicketStatus;
import com.dev.parking.repository.TicketRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import com.dev.parking.entity.VehicleType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class ticketApiController {

    private final TicketRepository repo;

    public ticketApiController(TicketRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Ticket> list(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) String sort
    ) {
        Sort s = parseSort(sort);

        if (status == null) {
            return repo.findAll(s);
        }
        return repo.findByStatus(status, s);
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.unsorted();

        // supports: "entryTime,desc" OR "exitTime,asc"
        String[] parts = sort.split(",");
        if (parts.length == 2) {
            Sort.Direction dir = "desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC;
            return Sort.by(dir, parts[0].trim());
        }
        return Sort.by(sort.trim());
    }

    @PostMapping("/entry")

    public Ticket entry(@RequestParam String plate, @RequestParam VehicleType type) {
        // code...


        TicketRepository ticketRepository = null;
        if (ticketRepository.existsByPlateAndStatus(plate, TicketStatus.OPEN)) {
            throw new IllegalArgumentException("Ticket already OPEN for plate " + plate);
        }

        Ticket t = new Ticket();
        t.setPlate(plate.trim().toUpperCase());
        t.setVehicleType(type);
        t.setStatus(TicketStatus.OPEN);
        t.setEntryTime(LocalDateTime.from(Instant.now()));
        return ticketRepository.save(t);
    }
}
