package com.hutech.coca.model;

import com.hutech.coca.common.VoucherStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "source_booking_id")
    private Booking sourceBooking;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private Double remainingAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VoucherStatus status = VoucherStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime expiredAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
