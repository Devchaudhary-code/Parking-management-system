package src.main.java.com.dev.parking.repository;

import com.dev.parking.entity.Ticket;
import com.dev.parking.entity.TicketStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Find the currently OPEN ticket for a given plate (used when closing a ticket)
    Optional<Ticket> findByPlateAndStatus(String plate, TicketStatus status);

    // Check if an OPEN ticket already exists for a plate (used when creating entry)
    boolean existsByPlateAndStatus(String plate, TicketStatus status);

    // Basic search examples (we'll use these for filter endpoints)
    List<Ticket> findByPlate(String plate);
    List<Ticket> findByStatus(TicketStatus status);

    // Same as findByStatus, but allows sorting from the controller (e.g. entryTime desc)
    List<Ticket> findByStatus(TicketStatus status, Sort sort);
}
