package com.dev.parking.repository;

import com.dev.parking.entity.Ticket;
import com.dev.parking.entity.TicketStatus;
import com.dev.parking.entity.VehicleType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class TicketSpecifications {

    public static Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasVehicleType(VehicleType vehicleType) {
        return (root, query, cb) -> vehicleType == null ? cb.conjunction()
                : cb.equal(root.get("vehicleType"), vehicleType);
    }

    // plate contains (case-insensitive)
    public static Specification<Ticket> plateContains(String plate) {
        return (root, query, cb) -> {
            if (plate == null || plate.trim().isEmpty()) return cb.conjunction();
            String like = "%" + plate.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("plate")), like);
        };
    }

    // Applies to entryTime by default
    public static Specification<Ticket> entryTimeFrom(LocalDateTime from) {
        return (root, query, cb) -> from == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("entryTime"), from);
    }

    public static Specification<Ticket> entryTimeTo(LocalDateTime to) {
        return (root, query, cb) -> to == null ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("entryTime"), to);
    }
}
