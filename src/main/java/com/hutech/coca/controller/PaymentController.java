package com.hutech.coca.controller;

import com.hutech.coca.dto.BookingDetailsResponse;
import com.hutech.coca.dto.MomoWebhookRequest;
import com.hutech.coca.dto.PaymentBookingSummaryResponse;
import com.hutech.coca.dto.PaymentInitRequest;
import com.hutech.coca.dto.PaymentInitResponse;
import com.hutech.coca.model.User;
import com.hutech.coca.service.CurrentUserService;
import com.hutech.coca.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;

    @PostMapping("/booking/{bookingId}/init")
    public ResponseEntity<Map<String, Object>> initPayment(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long bookingId,
            @RequestBody PaymentInitRequest request
    ) {
        try {
            User currentUser = currentUserService.getCurrentUser(authorization);
            PaymentInitResponse initResult = paymentService.initializePayment(
                    bookingId,
                    currentUser.getId(),
                    request.getPaymentMethod(),
                    request.getVoucherCode()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment initialized");
            response.put("data", initResult);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/momo/webhook")
    public ResponseEntity<Map<String, Object>> momoWebhook(@RequestBody MomoWebhookRequest request) {
        try {
            BookingDetailsResponse booking = paymentService.handleMomoWebhook(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Webhook processed");
            response.put("data", booking);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/momo/return")
    public ResponseEntity<Map<String, Object>> momoReturn(@RequestParam Map<String, String> params) {
        try {
            BookingDetailsResponse booking = paymentService.handleMomoReturn(params);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "MoMo return processed");
            response.put("data", booking);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<Map<String, Object>> vnpayReturn(@RequestParam Map<String, String> params) {
        try {
            BookingDetailsResponse booking = paymentService.handleVnpayReturn(params);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "VNPay callback processed");
            response.put("data", booking);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/booking/{bookingId}/summary")
    public ResponseEntity<Map<String, Object>> getBookingPaymentSummary(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long bookingId) {
        try {
            User currentUser = currentUserService.getCurrentUser(authorization);
            PaymentBookingSummaryResponse summary = paymentService.getBookingPaymentSummary(bookingId, currentUser.getId());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", summary);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
