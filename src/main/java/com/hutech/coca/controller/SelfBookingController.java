package com.hutech.coca.controller;

import com.hutech.coca.dto.BookingSummaryResponse;
import com.hutech.coca.dto.CancelMyBookingResponse;
import com.hutech.coca.dto.SelfBookingCheckoutRequest;
import com.hutech.coca.dto.SelfBookingCheckoutResponse;
import com.hutech.coca.dto.VoucherSummaryResponse;
import com.hutech.coca.service.SelfBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings/me")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SelfBookingController {

    private final SelfBookingService selfBookingService;

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(
            @RequestHeader("Authorization") String authorization,
            @RequestBody SelfBookingCheckoutRequest request) {
        try {
            SelfBookingCheckoutResponse result = selfBookingService.checkout(authorization, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Booking created successfully");
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Map<String, Object>> getMyBookingDetail(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long bookingId) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", selfBookingService.getMyBookingDetail(authorization, bookingId));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyBookings(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long petId) {
        try {
            List<BookingSummaryResponse> data = selfBookingService.getMyBookings(authorization, petId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelMyBooking(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long bookingId) {
        try {
            CancelMyBookingResponse result = selfBookingService.cancelMyBooking(authorization, bookingId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result.getMessage());
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/vouchers")
    public ResponseEntity<Map<String, Object>> getMyActiveVouchers(
            @RequestHeader("Authorization") String authorization) {
        try {
            List<VoucherSummaryResponse> data = selfBookingService.getMyActiveVouchers(authorization);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}