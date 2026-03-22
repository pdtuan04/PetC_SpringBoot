package com.hutech.coca.controller;

import com.hutech.coca.dto.*;
import com.hutech.coca.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;
    @GetMapping("/available-slots")
    public ResponseEntity<Map<String, Object>> getAvailableBookingSlots(
            @RequestParam int durationInMinutes,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedDay) {
        try {
            List<AvailableSlotResponse> result = bookingService.getAvailableBookingSlots(durationInMinutes, selectedDay.atStartOfDay());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lấy danh sách giờ trống thành công.");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    @PostMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> createBookingForUser(
            @PathVariable Long userId,
            @RequestBody CreateBookingRequest request) {
        try {
            BookingDetailsResponse result = bookingService.createBooking(request, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tạo lịch hẹn cho khách thành công.");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserBookings(@PathVariable Long userId) {
        try {
            List<BookingSummaryResponse> result = bookingService.getUserBookings(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lấy lịch sử đặt lịch thành công.");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<Map<String, Object>> confirmBooking(@PathVariable Long bookingId) {
        try {
            bookingService.confirmBooking(bookingId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã xác nhận lịch hẹn.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBooking(@PathVariable Long bookingId) {
        try {
            bookingService.cancelBooking(bookingId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã hủy lịch hẹn.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
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
            BookingDetailsResponse booking = bookingService.getBookingDetail(id);

            response.put("success", true);
            response.put("message", "Get booking successfully.");
            response.put("data", booking);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Booking not found.");
            return ResponseEntity.status(404).body(response);
        }
    }
    @PutMapping("/{bookingId}")
    public ResponseEntity<Map<String, Object>> updateBooking(
            @PathVariable Long bookingId,
            @RequestBody UpdateBookingRequest request) {
        try {
            BookingDetailsResponse result = bookingService.updateBooking(bookingId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật lịch hẹn thành công.");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/code/{bookingCode}")
    public ResponseEntity<Map<String, Object>> getBookingByCode(@PathVariable String bookingCode) {
        try {
            BookingDetailsResponse result = bookingService.getBookingDetailByCode(bookingCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tìm thấy lịch hẹn.");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Map<String, Object>> deleteBooking(@PathVariable Long bookingId) {
        try {
            bookingService.deleteBooking(bookingId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Xóa lịch hẹn thành công.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    @PostMapping("/{bookingId}/start")
    public ResponseEntity<Map<String, Object>> startBooking(@PathVariable Long bookingId) {
        try {
            bookingService.startBooking(bookingId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã bắt đầu tiến hành dịch vụ.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{bookingId}/complete")
    public ResponseEntity<Map<String, Object>> completeBooking(@PathVariable Long bookingId) {
        try {
            bookingService.completeBooking(bookingId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã hoàn thành dịch vụ.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}