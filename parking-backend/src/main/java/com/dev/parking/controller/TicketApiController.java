package com.dev.parking.controller;

import com.dev.parking.entity.Ticket;
import com.dev.parking.entity.TicketStatus;
import com.dev.parking.entity.VehicleType;
import com.dev.parking.repository.TicketRepository;
import com.dev.parking.repository.TicketSpecifications;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;
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



    //helpers

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
