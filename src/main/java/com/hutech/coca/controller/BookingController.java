package com.hutech.coca.controller;

import com.hutech.coca.dto.BookingRequest;
import com.hutech.coca.model.Booking;
import com.hutech.coca.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<List<Booking>> getAll() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> getById(@PathVariable Long id) {
        return bookingService.getBookingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Booking> create(@RequestBody BookingRequest request) {
        // Nhận JSON từ React -> Đưa cho Service xử lý -> Trả về kết quả
        throw new RuntimeException();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Booking> update(@PathVariable Long id, @RequestBody Booking booking) {
        return bookingService.getBookingById(id).map(existing -> {
            booking.setId(id);
            return ResponseEntity.ok(bookingService.saveBooking(booking));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }
}