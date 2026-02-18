package com.dev.parking.controller;

import com.dev.parking.entity.Ticket;
import com.dev.parking.entity.TicketStatus;
import com.dev.parking.entity.VehicleType;
import com.dev.parking.repository.TicketRepository;
import com.dev.parking.repository.TicketSpecifications;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketApiController {

    private final TicketRepository repo;

    public TicketApiController(TicketRepository repo) {
        this.repo = repo;
    }

    // ✅ Search + Filter + Sort endpoint
    @GetMapping
    public List<Ticket> list(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) VehicleType vehicleType,
            @RequestParam(required = false) String plate
    ) {
        Sort s = parseSort(sort);

        Specification<Ticket> spec = Specification.where(TicketSpecifications.hasStatus(status))
                .and(TicketSpecifications.hasVehicleType(vehicleType))
                .and(TicketSpecifications.plateContains(plate));

        return repo.findAll(spec, s);
    }

    // ✅ Manual Entry
    // Example:
    // POST /api/tickets/entry?plate=KA%20AB%201234&type=CAR
    @PostMapping("/entry")
    public Ticket entry(@RequestParam String plate, @RequestParam VehicleType type) {
        String normalizedPlate = normalizePlate(plate);

        boolean alreadyOpen = repo.existsByPlateIgnoreCaseAndStatus(normalizedPlate, TicketStatus.OPEN);
        if (alreadyOpen) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket already OPEN for plate " + normalizedPlate
            );
        }

        Ticket t = new Ticket();
        t.setPlate(normalizedPlate);
        t.setVehicleType(type);
        t.setStatus(TicketStatus.OPEN);
        t.setEntryTime(LocalDateTime.now());

        return repo.save(t);
    }

    // ✅ Manual Exit
    // Example:
    // POST /api/tickets/exit?plate=KA%20AB%201234
    @PostMapping("/exit")
    public Ticket exit(@RequestParam String plate) {
        String normalizedPlate = normalizePlate(plate);

        Ticket t = repo.findFirstByPlateIgnoreCaseAndStatusOrderByEntryTimeDesc(normalizedPlate, TicketStatus.OPEN)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No OPEN ticket found for plate " + normalizedPlate
                ));

        t.setExitTime(LocalDateTime.now());
        t.setStatus(TicketStatus.CLOSED);

        // pricing later: you can set amount here when pricing logic is ready
        // t.setAmount(BigDecimal.ZERO);

        return repo.save(t);
    }

    // -------------------- helpers --------------------

    private String normalizePlate(String plate) {
        if (plate == null) return null;
        return plate.trim().toUpperCase();
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.unsorted();

        String[] parts = sort.split(",");
        if (parts.length == 2) {
            Sort.Direction dir = "desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC;
            return Sort.by(dir, parts[0].trim());
        }
        return Sort.by(sort.trim());
    }
}
