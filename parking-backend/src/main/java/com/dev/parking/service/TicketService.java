package com.dev.parking.service;

import com.dev.parking.entity.Ticket;
import com.dev.parking.entity.TicketStatus;
import com.dev.parking.entity.VehicleType;
import com.dev.parking.repository.TicketRepository;
import com.dev.parking.repository.TicketSpecifications;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public  Page<Ticket> findTickets(
            TicketStatus status,
            String plate,
            VehicleType vehicleType,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size,
            Sort sort
    ) {
        Specification<Ticket> spec = Specification.where(TicketSpecifications.hasStatus(status))
                .and(TicketSpecifications.plateContains(plate))
                .and(TicketSpecifications.hasVehicleType(vehicleType))
                .and(TicketSpecifications.entryTimeFrom(from))
                .and(TicketSpecifications.entryTimeTo(to));

        Pageable pageable = PageRequest.of(page, size, sort);
        return ticketRepository.findAll(spec, pageable);
    }
}

