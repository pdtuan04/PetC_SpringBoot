package com.hutech.coca.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "booking_details")
public class BookingDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Min(value = 0, message = "Giá tiền tại thời điểm đặt không được âm")
    private double priceAtTime;

    @NotNull(message = "Hóa đơn là bắt buộc")
    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @NotNull(message = "Dịch vụ là bắt buộc")
    @ManyToOne
    @JoinColumn(name = "service_id")
    private Service service;
}