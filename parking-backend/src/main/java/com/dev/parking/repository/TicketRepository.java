package com.dev.parking.repository;

import com.dev.parking.entity.Ticket;
import com.dev.parking.entity.TicketStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    boolean existsByPlateIgnoreCaseAndStatus(String plate, TicketStatus status);

    Optional<Ticket> findFirstByPlateIgnoreCaseAndStatusOrderByEntryTimeDesc(String plate, TicketStatus status);


    List<Ticket> findByStatus(TicketStatus status, Sort sort);
}
