package com.hutech.coca.repository;

import com.hutech.coca.common.VoucherStatus;
import com.hutech.coca.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IVoucherRepository extends JpaRepository<Voucher, Long> {
    List<Voucher> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, VoucherStatus status);
    Optional<Voucher> findByCodeAndUserId(String code, Long userId);
    Optional<Voucher> findBySourceBookingId(Long bookingId);
}
