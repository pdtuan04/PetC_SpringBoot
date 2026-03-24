package com.hutech.coca.service;

import com.hutech.coca.common.BookingStatus;
import com.hutech.coca.common.PaymentMethod;
import com.hutech.coca.common.PaymentTransactionStatus;
import com.hutech.coca.dto.BookingDetailsResponse;
import com.hutech.coca.dto.MomoWebhookRequest;
import com.hutech.coca.dto.PaymentBookingSummaryResponse;
import com.hutech.coca.dto.PaymentInitResponse;
import com.hutech.coca.model.Booking;
import com.hutech.coca.model.PaymentTransaction;
import com.hutech.coca.model.User;
import com.hutech.coca.model.Voucher;
import com.hutech.coca.repository.IBookingRepository;
import com.hutech.coca.repository.IPaymentTransactionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final int RAW_PAYLOAD_SAFE_LENGTH = 4000;

    private final IBookingRepository bookingRepository;
    private final IPaymentTransactionRepository paymentTransactionRepository;
    private final BookingService bookingService;
    private final VoucherService voucherService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${payment.momo.partner-code:}")
    private String momoPartnerCode;

    @Value("${payment.momo.access-key:}")
    private String momoAccessKey;

    @Value("${payment.momo.secret-key:}")
    private String momoSecretKey;

    @Value("${payment.momo.request-type:captureWallet}")
    private String momoRequestType;

    @Value("${payment.momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String momoEndpoint;

    @Value("${payment.momo.redirect-url:https://localhost:5173/payment/momo/return}")
    private String momoRedirectUrl;

    @Value("${payment.momo.ipn-url:http://localhost:8080/api/payments/momo/webhook}")
    private String momoIpnUrl;

    @Value("${payment.momo.partner-name:PetC}")
    private String momoPartnerName;

    @Value("${payment.momo.store-id:PetCStore}")
    private String momoStoreId;

    @Value("${payment.vnpay.tmn-code:}")
    private String vnpTmnCode;

    @Value("${payment.vnpay.hash-secret:}")
    private String vnpHashSecret;

    @Value("${payment.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPayUrl;

    @Value("${payment.vnpay.return-url:https://localhost:5173/payment/vnpay/return}")
    private String vnpReturnUrl;

    @Transactional
    public PaymentInitResponse initializePayment(Long bookingId, Long userId, PaymentMethod paymentMethod, String voucherCode) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUser().getId().equals(userId)) {
            throw new RuntimeException("You cannot pay for another user's booking");
        }

        if (paymentMethod == null) {
            throw new RuntimeException("Payment method is required");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking is cancelled");
        }

        PaymentTransaction tx = new PaymentTransaction();
        tx.setBooking(booking);
        tx.setUser(booking.getUser());
        tx.setPaymentMethod(paymentMethod);
        tx.setTransactionRef("PAY" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());

        double totalAmount = booking.getTotalPrice() == null ? 0 : booking.getTotalPrice();
        double voucherDiscount = 0;
        String normalizedVoucherCode = null;

        if (!isBlank(voucherCode)) {
            if (!isPrepaidMethod(paymentMethod)) {
                throw new RuntimeException("Voucher can only be used with prepaid payment methods");
            }

            Voucher voucher = voucherService.getValidVoucherForPayment(userId, voucherCode.trim());
            voucherDiscount = Math.min(totalAmount, voucher.getRemainingAmount());
            if (voucherDiscount <= 0) {
                throw new RuntimeException("Voucher has no usable balance");
            }
            normalizedVoucherCode = voucher.getCode();
            tx.setVoucherCode(normalizedVoucherCode);
            tx.setVoucherDiscount(voucherDiscount);
        }

        double payableAmount = Math.max(0, totalAmount - voucherDiscount);
        tx.setAmount(payableAmount);

        PaymentInitResponse response = new PaymentInitResponse();
        response.setTransactionRef(tx.getTransactionRef());
        response.setPaymentMethod(paymentMethod.name());
        response.setTotalAmount(totalAmount);
        response.setVoucherDiscount(voucherDiscount);
        response.setPayableAmount(payableAmount);
        response.setVoucherCode(normalizedVoucherCode);

        if (paymentMethod == PaymentMethod.PAY_LATER) {
            tx.setPaymentProvider("INTERNAL");
            tx.setPaymentStatus(PaymentTransactionStatus.DEFERRED);
            paymentTransactionRepository.save(tx);

            bookingService.confirmBookingAfterPayment(bookingId);

            response.setPaymentStatus(PaymentTransactionStatus.DEFERRED.name());
            response.setPaymentUrl(null);
            response.setMessage("Booking confirmed. Customer will pay after service completion.");
            return response;
        }

        // Keep PENDING for prepaid initialization to remain compatible with legacy schemas
        // where booking_status has not been migrated to include PENDING_PAYMENT yet.

        if (paymentMethod == PaymentMethod.MOMO_PREPAID) {
            tx.setPaymentProvider("MOMO");
            tx.setPaymentStatus(PaymentTransactionStatus.INITIATED);
            paymentTransactionRepository.save(tx);

            String payUrl = initializeMomo(tx, booking, payableAmount);
            response.setPaymentStatus(PaymentTransactionStatus.INITIATED.name());
            response.setPaymentUrl(payUrl);
            response.setMessage("MOMO payment initialized");
            return response;
        }

        if (paymentMethod == PaymentMethod.VNPAY_PREPAID) {
            tx.setPaymentProvider("VNPAY");
            tx.setPaymentStatus(PaymentTransactionStatus.INITIATED);
            paymentTransactionRepository.save(tx);

            String payUrl = buildVnpayPaymentUrl(tx, booking, payableAmount);
            response.setPaymentStatus(PaymentTransactionStatus.INITIATED.name());
            response.setPaymentUrl(payUrl);
            response.setMessage("VNPay payment initialized");
            return response;
        }

        throw new RuntimeException("Unsupported payment method");
    }

    public PaymentBookingSummaryResponse getBookingPaymentSummary(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        User bookingOwner = booking.getUser();
        if (bookingOwner == null || !bookingOwner.getId().equals(userId)) {
            throw new RuntimeException("You cannot access another user's payment details");
        }

        PaymentTransaction latest = paymentTransactionRepository
                .findTopByBookingIdOrderByUpdatedAtDesc(bookingId)
                .orElse(null);

        boolean hasSuccessfulPayment = paymentTransactionRepository
                .existsByBookingIdAndPaymentStatus(bookingId, PaymentTransactionStatus.SUCCESS);

        boolean canRetryPayment = !hasSuccessfulPayment
                && booking.getBookingStatus() != BookingStatus.CANCELLED
                && booking.getBookingStatus() != BookingStatus.COMPLETED
                && booking.getBookingStatus() != BookingStatus.IN_PROGRESS
                && booking.getBookingStatus() != BookingStatus.NO_SHOW;

        PaymentBookingSummaryResponse response = new PaymentBookingSummaryResponse();
        response.setBookingId(bookingId);
        response.setHasSuccessfulPayment(hasSuccessfulPayment);
        response.setCanRetryPayment(canRetryPayment);
        response.setLatestPaymentStatus(latest == null ? null : latest.getPaymentStatus().name());
        response.setLatestPaymentMethod(latest == null ? null : latest.getPaymentMethod().name());
        response.setLatestPaymentProvider(latest == null ? null : latest.getPaymentProvider());
        return response;
    }

    @Transactional
    public BookingDetailsResponse handleMomoWebhook(MomoWebhookRequest request) {
        if (request == null) {
            throw new RuntimeException("Invalid webhook payload");
        }

        String txRef = request.getTransactionRef();
        if (isBlank(txRef)) {
            txRef = request.getOrderId();
        }
        if (isBlank(txRef)) {
            throw new RuntimeException("transactionRef/orderId is required");
        }

        if (!isBlank(request.getSignature())
                && !isBlank(request.getRequestId())
                && request.getResultCode() != null
                && !isBlank(request.getPartnerCode())) {
            String rawSignature = "accessKey=" + momoAccessKey
                    + "&amount=" + safe(request.getAmount())
                    + "&extraData=" + safe(request.getExtraData())
                    + "&message=" + safe(request.getMessage())
                    + "&orderId=" + safe(request.getOrderId())
                    + "&orderInfo=" + safe(request.getOrderInfo())
                    + "&orderType=" + safe(request.getOrderType())
                    + "&partnerCode=" + safe(request.getPartnerCode())
                    + "&payType=" + safe(request.getPayType())
                    + "&requestId=" + safe(request.getRequestId())
                    + "&responseTime=" + safe(request.getResponseTime())
                    + "&resultCode=" + request.getResultCode()
                    + "&transId=" + (request.getTransId() == null ? "" : request.getTransId());
            String calculatedSignature = hmacSha256(rawSignature, momoSecretKey);
            if (!calculatedSignature.equalsIgnoreCase(request.getSignature())) {
                throw new RuntimeException("Invalid MOMO signature");
            }
        }

        PaymentTransaction tx = paymentTransactionRepository.findByTransactionRef(txRef)
                .orElseThrow(() -> new RuntimeException("Payment transaction not found"));

        boolean success = Boolean.TRUE.equals(request.getSuccess()) || (request.getResultCode() != null && request.getResultCode() == 0);

        if (success) {
            if (tx.getPaymentStatus() != PaymentTransactionStatus.SUCCESS) {
                tx.setPaymentStatus(PaymentTransactionStatus.SUCCESS);
                tx.setProviderTransactionId(request.getProviderTransactionId() != null
                        ? request.getProviderTransactionId()
                        : (request.getTransId() == null ? null : String.valueOf(request.getTransId())));
                tx.setRawPayload(toSafeRawPayload(request.getRawPayload()));
                paymentTransactionRepository.save(tx);
                voucherService.consumeVoucherAmount(
                        tx.getUser().getId(),
                        tx.getVoucherCode(),
                        tx.getVoucherDiscount()
                );
                bookingService.confirmBookingAfterPayment(tx.getBooking().getId());
            }
        } else {
            tx.setPaymentStatus(PaymentTransactionStatus.FAILED);
            tx.setProviderTransactionId(request.getProviderTransactionId());
            tx.setRawPayload(toSafeRawPayload(request.getRawPayload()));
            paymentTransactionRepository.save(tx);
        }

        return bookingService.getBookingDetail(tx.getBooking().getId());
    }

    @Transactional
    public BookingDetailsResponse handleMomoReturn(Map<String, String> params) {
        String txRef = params.get("orderId");
        if (isBlank(txRef)) {
            txRef = params.get("transactionRef");
        }

        if (isBlank(txRef)) {
            throw new RuntimeException("orderId/transactionRef is required");
        }

        PaymentTransaction tx = paymentTransactionRepository.findByTransactionRef(txRef)
                .orElseThrow(() -> new RuntimeException("Payment transaction not found"));

        String resultCode = params.getOrDefault("resultCode", "99");
        String transId = params.get("transId");

        if ("0".equals(resultCode)) {
            if (tx.getPaymentStatus() != PaymentTransactionStatus.SUCCESS) {
                tx.setPaymentStatus(PaymentTransactionStatus.SUCCESS);
                tx.setProviderTransactionId(transId);
                tx.setRawPayload(toSafeRawPayload(params.toString()));
                paymentTransactionRepository.save(tx);
                voucherService.consumeVoucherAmount(
                        tx.getUser().getId(),
                        tx.getVoucherCode(),
                        tx.getVoucherDiscount()
                );
                bookingService.confirmBookingAfterPayment(tx.getBooking().getId());
            }
        } else {
            tx.setPaymentStatus(PaymentTransactionStatus.FAILED);
            tx.setProviderTransactionId(transId);
            tx.setRawPayload(toSafeRawPayload(params.toString()));
            paymentTransactionRepository.save(tx);
        }

        return bookingService.getBookingDetail(tx.getBooking().getId());
    }

    @Transactional
    public BookingDetailsResponse handleVnpayReturn(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        if (isBlank(secureHash)) {
            throw new RuntimeException("vnp_SecureHash is required");
        }

        Map<String, String> hashParams = new HashMap<>(params);
        hashParams.remove("vnp_SecureHash");
        hashParams.remove("vnp_SecureHashType");

        List<String> keys = new ArrayList<>(hashParams.keySet());
        keys.sort(Comparator.naturalOrder());

        StringBuilder hashData = new StringBuilder();
        boolean first = true;
        for (String key : keys) {
            String value = hashParams.get(key);
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (!first) {
                hashData.append('&');
            }
            first = false;
            hashData.append(key).append('=').append(urlEncode(value));
        }

        String calculatedHash = hmacSha512(hashData.toString(), vnpHashSecret);
        if (!calculatedHash.equalsIgnoreCase(secureHash)) {
            throw new RuntimeException("Invalid VNPAY signature");
        }

        String txRef = params.get("vnp_TxnRef");
        if (isBlank(txRef)) {
            throw new RuntimeException("vnp_TxnRef is required");
        }

        PaymentTransaction tx = paymentTransactionRepository.findByTransactionRef(txRef)
                .orElseThrow(() -> new RuntimeException("Payment transaction not found"));

        String responseCode = params.getOrDefault("vnp_ResponseCode", "99");
        String txnNo = params.get("vnp_TransactionNo");

        if ("00".equals(responseCode)) {
            if (tx.getPaymentStatus() != PaymentTransactionStatus.SUCCESS) {
                tx.setPaymentStatus(PaymentTransactionStatus.SUCCESS);
                tx.setProviderTransactionId(txnNo);
                tx.setRawPayload(toSafeRawPayload(params.toString()));
                paymentTransactionRepository.save(tx);
                voucherService.consumeVoucherAmount(
                        tx.getUser().getId(),
                        tx.getVoucherCode(),
                        tx.getVoucherDiscount()
                );
                bookingService.confirmBookingAfterPayment(tx.getBooking().getId());
            }
        } else {
            tx.setPaymentStatus(PaymentTransactionStatus.FAILED);
            tx.setProviderTransactionId(txnNo);
            tx.setRawPayload(toSafeRawPayload(params.toString()));
            paymentTransactionRepository.save(tx);
        }

        return bookingService.getBookingDetail(tx.getBooking().getId());
    }

    private String initializeMomo(PaymentTransaction tx, Booking booking, double payableAmount) {
        if (isBlank(momoPartnerCode) || isBlank(momoAccessKey) || isBlank(momoSecretKey)) {
            throw new RuntimeException("MoMo credentials are missing");
        }

        String requestId = "REQ" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        String orderId = tx.getTransactionRef();
        String amount = String.valueOf(Math.round(payableAmount));
        String orderInfo = "Thanh toan lich hen " + booking.getBookingCode();
        String extraData = "";

        String rawSignature = "accessKey=" + momoAccessKey
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + momoIpnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + momoPartnerCode
                + "&redirectUrl=" + momoRedirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + momoRequestType;

        String signature = hmacSha256(rawSignature, momoSecretKey);

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("partnerCode", momoPartnerCode);
            payload.put("partnerName", momoPartnerName);
            payload.put("storeId", momoStoreId);
            payload.put("requestId", requestId);
            payload.put("amount", amount);
            payload.put("orderId", orderId);
            payload.put("orderInfo", orderInfo);
            payload.put("redirectUrl", momoRedirectUrl);
            payload.put("ipnUrl", momoIpnUrl);
            payload.put("lang", "vi");
            payload.put("extraData", extraData);
            payload.put("requestType", momoRequestType);
            payload.put("signature", signature);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(momoEndpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("MoMo API returned HTTP " + response.statusCode());
            }

            Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {});
            Integer resultCode = ((Number) body.getOrDefault("resultCode", -1)).intValue();
            if (resultCode != 0) {
                throw new RuntimeException("MoMo create payment failed: " + body.getOrDefault("message", "unknown"));
            }

            Object payUrl = body.get("payUrl");
            if (payUrl == null) {
                throw new RuntimeException("MoMo payUrl is missing");
            }
            return String.valueOf(payUrl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MOMO payment: " + e.getMessage(), e);
        }
    }

    private String buildVnpayPaymentUrl(PaymentTransaction tx, Booking booking, double payableAmount) {
        if (isBlank(vnpTmnCode) || isBlank(vnpHashSecret)) {
            throw new RuntimeException("VNPay credentials are missing");
        }

        long amount = Math.round(payableAmount) * 100L;
        String createDate = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnpTmnCode);
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", tx.getTransactionRef());
        params.put("vnp_OrderInfo", "Thanh toan lich hen " + booking.getBookingCode());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnpReturnUrl);
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate", createDate);

        List<String> keys = new ArrayList<>(params.keySet());
        keys.sort(Comparator.naturalOrder());

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);
            String encodedValue = urlEncode(value);
            if (i > 0) {
                hashData.append('&');
                query.append('&');
            }
            hashData.append(key).append('=').append(encodedValue);
            query.append(key).append('=').append(encodedValue);
        }

        String secureHash = hmacSha512(hashData.toString(), vnpHashSecret);
        query.append("&vnp_SecureHash=").append(secureHash);
        return vnpPayUrl + "?" + query;
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign HMAC SHA256", e);
        }
    }

    private String hmacSha512(String data, String secret) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign HMAC SHA512", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public boolean isPrepaidMethod(PaymentMethod paymentMethod) {
        return paymentMethod == PaymentMethod.MOMO_PREPAID || paymentMethod == PaymentMethod.VNPAY_PREPAID;
    }

    private String toSafeRawPayload(String payload) {
        if (payload == null) {
            return null;
        }
        return payload.length() <= RAW_PAYLOAD_SAFE_LENGTH
                ? payload
                : payload.substring(0, RAW_PAYLOAD_SAFE_LENGTH);
    }
}