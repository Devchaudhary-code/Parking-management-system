package com.dev.parking.controller;

import com.dev.parking.entity.Ticket;
import com.dev.parking.entity.TicketStatus;
import com.dev.parking.entity.VehicleType;
import com.dev.parking.repository.TicketRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specification;
import com.dev.parking.repository.TicketSpecifications;



import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketApiController {

    private final TicketRepository repo;

    public TicketApiController(TicketRepository repo) {
        this.repo = repo;
    }

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


    @PostMapping("/entry")
    public Ticket entry(@RequestParam String plate, @RequestParam VehicleType type) {
        String normalizedPlate = plate.trim().toUpperCase();

        boolean alreadyOpen = repo.existsByPlateIgnoreCaseAndStatus(normalizedPlate, TicketStatus.OPEN);
        if (alreadyOpen) {
            throw new IllegalArgumentException("Ticket already OPEN for plate " + normalizedPlate);
        }

        Ticket t = new Ticket();
        t.setPlate(normalizedPlate);
        t.setVehicleType(type);
        t.setStatus(TicketStatus.OPEN);
        t.setEntryTime(LocalDateTime.now());
        return repo.save(t);
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
