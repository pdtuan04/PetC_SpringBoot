package com.hutech.coca.model;

import com.hutech.coca.common.BookingStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Mã booking là bắt buộc")
    private String bookingCode;

    @NotNull(message = "Thời gian đặt lịch là bắt buộc")
    private LocalDateTime scheduledAt;

    @NotNull(message = "Thời gian dự kiến kết thúc là bắt buộc")
    private LocalDateTime expectedEndTime;

    @Min(value = 0, message = "Tổng tiền không được âm")
    private Double totalPrice;

    private String notes;

    @Enumerated(EnumType.STRING)
    private BookingStatus bookingStatus = BookingStatus.PENDING;

    @NotNull(message = "Thời gian hết hạn giữ chỗ là bắt buộc")
    private LocalDateTime holdExpiredAt;

    @NotNull(message = "Thông tin khách hàng là bắt buộc")
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull(message = "Thông tin thú cưng là bắt buộc")
    @ManyToOne
    @JoinColumn(name = "pet_id")
    private Pet pet;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<BookingDetail> bookingDetails;
    @Column(name = "is_deleted")
    private boolean isDeleted = false;
    @Column(name = "create_at", updatable = false)
    private LocalDateTime createAt;

    @PrePersist
    protected void onCreate() {
        this.createAt = LocalDateTime.now();
    }
}