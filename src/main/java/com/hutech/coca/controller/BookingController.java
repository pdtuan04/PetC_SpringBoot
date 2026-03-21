package com.hutech.coca.controller;

import com.hutech.coca.dto.BookingDetailsResponse;
import com.hutech.coca.dto.BookingRequest;
import com.hutech.coca.dto.BookingSummaryResponse;
import com.hutech.coca.model.Booking;
import com.hutech.coca.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;
    @GetMapping("/bookings-in-week")
    public ResponseEntity<List<BookingSummaryResponse>> getAllBookingsInWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        List<BookingSummaryResponse> bookings = bookingService.getAllBookingsInWeek(startDate.atStartOfDay());
        return ResponseEntity.ok(bookings);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBookingById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Lấy data từ service
            BookingDetailsResponse booking = bookingService.getBookingDetail(id);

            response.put("success", true);
            response.put("message", "Get booking successfully.");
            response.put("data", booking);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // Xử lý khi không tìm thấy booking
            response.put("success", false);
            response.put("message", "Booking not found.");
            return ResponseEntity.status(404).body(response);
        }
    }
}