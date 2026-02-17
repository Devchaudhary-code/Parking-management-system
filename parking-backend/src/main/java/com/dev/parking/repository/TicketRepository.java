package com.dev.parking.repository;

import com.dev.parking.entity.Ticket;
import com.dev.parking.entity.TicketStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    // for: repo.findByStatus(status, sort)
    List<Ticket> findByStatus(TicketStatus status, Sort sort);

    // for: repo.existsByPlateIgnoreCaseAndStatus(plate, OPEN)
    boolean existsByPlateIgnoreCaseAndStatus(String plate, TicketStatus status);
}
