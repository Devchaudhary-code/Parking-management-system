package com.dev.parking.service;

import com.dev.parking.entity.Ticket;
import com.dev.parking.entity.TicketStatus;
import com.dev.parking.entity.VehicleType;
import com.dev.parking.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class TicketServices {

    private final TicketRepository ticketRepository;

    // Constructor injection: Spring will provide the TicketRepository automatically
    public TicketServices(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    // ENTRY: create a new OPEN ticket
    public Ticket createEntry(String plate, VehicleType vehicleType) {
        String normalizedPlate = normalizePlate(plate);

        boolean alreadyOpen = ticketRepository.existsByPlateAndStatus(normalizedPlate, TicketStatus.OPEN);
        if (alreadyOpen) {
            throw new IllegalStateException("An OPEN ticket already exists for plate: " + normalizedPlate);
        }

        Ticket ticket = new Ticket();
        ticket.setPlate(normalizedPlate);
        ticket.setVehicleType(vehicleType);
        ticket.setEntryTime(LocalDateTime.now());
        ticket.setStatus(TicketStatus.OPEN);

        return ticketRepository.save(ticket);
    }

    // EXIT: close the OPEN ticket for this plate, calculate fee
    public Ticket closeTicket(String plate) {
        String normalizedPlate = normalizePlate(plate);

        Ticket ticket = ticketRepository
                .findByPlateAndStatus(normalizedPlate, TicketStatus.OPEN)
                .orElseThrow(() -> new IllegalStateException("No OPEN ticket found for plate: " + normalizedPlate));

        LocalDateTime exitTime = LocalDateTime.now();
        ticket.setExitTime(exitTime);
        ticket.setStatus(TicketStatus.CLOSED);

        BigDecimal amount = calculateAmount(ticket.getEntryTime(), exitTime);
        ticket.setAmount(amount);

        return ticketRepository.save(ticket);
    }

    // Pricing rule:
    // - First hour costs €3
    // - Each additional started hour costs €2
    private BigDecimal calculateAmount(LocalDateTime entryTime, LocalDateTime exitTime) {
        long totalMinutes = Duration.between(entryTime, exitTime).toMinutes();

        // If someone exits instantly, still charge minimum first hour
        if (totalMinutes <= 60) {
            return BigDecimal.valueOf(3);
        }

        long extraMinutes = totalMinutes - 60;

        // ceil(extraMinutes / 60.0)
        long extraHours = (extraMinutes + 59) / 60;

        BigDecimal base = BigDecimal.valueOf(3);
        BigDecimal extra = BigDecimal.valueOf(2).multiply(BigDecimal.valueOf(extraHours));

        return base.add(extra).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizePlate(String plate) {
        if (plate == null) return null;
        return plate.trim().toUpperCase();
    }
}
