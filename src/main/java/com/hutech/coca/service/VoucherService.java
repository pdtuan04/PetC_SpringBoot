package com.hutech.coca.service;

import com.hutech.coca.common.VoucherStatus;
import com.hutech.coca.dto.VoucherSummaryResponse;
import com.hutech.coca.model.Booking;
import com.hutech.coca.model.User;
import com.hutech.coca.model.Voucher;
import com.hutech.coca.repository.IVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final IVoucherRepository voucherRepository;

    @Transactional
    public Voucher createCancellationVoucher(User user, Booking sourceBooking, Double amount) {
        if (amount == null || amount <= 0) {
            throw new RuntimeException("Voucher amount must be greater than zero");
        }

        Voucher existing = voucherRepository.findBySourceBookingId(sourceBooking.getId()).orElse(null);
        if (existing != null) {
            return existing;
        }

        Voucher voucher = new Voucher();
        voucher.setCode(generateCode());
        voucher.setUser(user);
        voucher.setSourceBooking(sourceBooking);
        voucher.setAmount(amount);
        voucher.setRemainingAmount(amount);
        voucher.setStatus(VoucherStatus.ACTIVE);
        voucher.setExpiredAt(LocalDateTime.now().plusMonths(3));
        return voucherRepository.save(voucher);
    }

    public List<VoucherSummaryResponse> getActiveVouchers(Long userId) {
        return voucherRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, VoucherStatus.ACTIVE)
                .stream()
                .filter(v -> v.getExpiredAt() == null || v.getExpiredAt().isAfter(LocalDateTime.now()))
                .map(this::mapToSummary)
                .toList();
    }

    public Voucher getValidVoucherForPayment(Long userId, String voucherCode) {
        Voucher voucher = voucherRepository.findByCodeAndUserId(voucherCode, userId)
                .orElseThrow(() -> new RuntimeException("Voucher does not exist"));

        if (voucher.getStatus() != VoucherStatus.ACTIVE) {
            throw new RuntimeException("Voucher is not active");
        }

        if (voucher.getExpiredAt() != null && !voucher.getExpiredAt().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Voucher has expired");
        }

        if (voucher.getRemainingAmount() == null || voucher.getRemainingAmount() <= 0) {
            throw new RuntimeException("Voucher has no remaining balance");
        }

        return voucher;
    }

    @Transactional
    public void consumeVoucherAmount(Long userId, String voucherCode, Double amount) {
        if (voucherCode == null || voucherCode.isBlank() || amount == null || amount <= 0) {
            return;
        }

        Voucher voucher = getValidVoucherForPayment(userId, voucherCode);
        double remaining = voucher.getRemainingAmount() == null ? 0 : voucher.getRemainingAmount();
        double deduction = Math.min(remaining, amount);
        if (deduction <= 0) {
            return;
        }

        voucher.setRemainingAmount(remaining - deduction);
        if (voucher.getRemainingAmount() <= 0.0001) {
            voucher.setRemainingAmount(0.0);
            voucher.setStatus(VoucherStatus.USED);
        }

        voucherRepository.save(voucher);
    }

    private VoucherSummaryResponse mapToSummary(Voucher voucher) {
        VoucherSummaryResponse response = new VoucherSummaryResponse();
        response.setId(voucher.getId());
        response.setCode(voucher.getCode());
        response.setAmount(voucher.getAmount());
        response.setRemainingAmount(voucher.getRemainingAmount());
        response.setStatus(voucher.getStatus().name());
        response.setCreatedAt(voucher.getCreatedAt());
        response.setExpiredAt(voucher.getExpiredAt());
        return response;
    }

    private String generateCode() {
        return "VC" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}
