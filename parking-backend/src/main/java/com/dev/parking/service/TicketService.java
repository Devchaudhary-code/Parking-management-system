package com.dev.parking.service;


import com.dev.parking.entity.Ticket;
import com.dev.parking.entity.TicketStatus;
import com.dev.parking.entity.VehicleType;
import com.dev.parking.repository.TicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public Ticket manualEntry(String plate, VehicleType type) {
        String normalizedPlate = normalizePlate(plate);

        boolean alreadyOpen = ticketRepository.existsByPlateIgnoreCaseAndStatus(normalizedPlate, TicketStatus.OPEN);
        if (alreadyOpen) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ticket already OPEN for plate " + normalizedPlate);
        }

        Ticket t = new Ticket();
        t.setPlate(normalizedPlate);
        t.setVehicleType(type);
        t.setStatus(TicketStatus.OPEN);
        t.setEntryTime(LocalDateTime.now());

        return ticketRepository.save(t);
    }

    @Transactional
    public Ticket manualExit(String plate) {
        String normalizedPlate = normalizePlate(plate);

        Ticket t = ticketRepository
                .findFirstByPlateIgnoreCaseAndStatusOrderByEntryTimeDesc(normalizedPlate, TicketStatus.OPEN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No OPEN ticket found for plate " + normalizedPlate));

        t.setExitTime(LocalDateTime.now());
        t.setStatus(TicketStatus.CLOSED);

        return ticketRepository.save(t);
    }

    private String normalizePlate(String plate) {
        return plate == null ? null : plate.trim().toUpperCase();
    }
}
