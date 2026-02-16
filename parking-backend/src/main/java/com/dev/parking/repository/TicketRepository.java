package com.dev.parking.repository;

import com.dev.parking.entity.Ticket;
import com.dev.parking.entity.TicketStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    boolean existsByPlateAndStatus(String plate, TicketStatus status);

    Optional<Ticket> findByPlateAndStatus(String plate, TicketStatus status);

    List<Ticket> findByStatus(TicketStatus status, Sort sort);
}
