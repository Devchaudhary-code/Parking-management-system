//
//package com.dev.parking.controller;
//
//import com.dev.parking.entity.Ticket;
//import com.dev.parking.entity.TicketStatus;
//import com.dev.parking.entity.VehicleType;
//import com.dev.parking.service.TicketService;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Sort;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDateTime;
//
//    @RestController
//    @RequestMapping("/api/tickets")
//    public class TicketController {
//
//        private final TicketService ticketService;
//
//        public TicketController(TicketService ticketService) {
//            this.ticketService = ticketService;
//        }
//
//        // GET /api/tickets?status=OPEN&plate=MH&vehicleType=CAR&from=2026-02-01T00:00:00&to=2026-02-17T23:59:59&sort=entryTime,desc&page=0&size=50
//        @GetMapping
//        public Page<Ticket> listTickets(
//                @RequestParam(required = false) TicketStatus status,
//                @RequestParam(required = false) String plate,
//                @RequestParam(required = false) VehicleType vehicleType,
//
//                @RequestParam(required = false)
//                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
//                LocalDateTime from,
//
//                @RequestParam(required = false)
//                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
//                LocalDateTime to,
//
//                @RequestParam(defaultValue = "0") int page,
//                @RequestParam(defaultValue = "50") int size,
//
//                // Spring supports sort like: ?sort=entryTime,desc
//                @RequestParam(defaultValue = "entryTime,desc") String sort
//        ) {
//            Sort parsedSort = parseSort(sort);
//            return ticketService.findTickets
//            (status, plate, vehicleType, from, to, page, size, parsedSort);
//        }
//
//        // Basic parser: "field,dir"
//        private Sort parseSort(String sort) {
//            if (sort == null || sort.isBlank()) return Sort.by(Sort.Direction.DESC, "entryTime");
//
//            String[] parts = sort.split(",");
//            String field = parts[0].trim();
//
//            Sort.Direction direction = Sort.Direction.DESC;
//            if (parts.length > 1) {
//                String dir = parts[1].trim().toLowerCase();
//                if (dir.equals("asc")) direction = Sort.Direction.ASC;
//                else if (dir.equals("desc")) direction = Sort.Direction.DESC;
//            }
//            return Sort.by(direction, field);
//        }
//    }
//
//
