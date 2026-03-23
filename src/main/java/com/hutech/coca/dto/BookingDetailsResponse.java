package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class BookingDetailsResponse {
    private Long id;
    private String bookingCode;
    private LocalDateTime scheduledAt;
    private LocalDateTime expectedEndTime;
    private double totalPrice;
    private String notes;
    private int bookingStatus;
    private Long userId;
    private String userName;

    private Long petId;
    private String petName;
    private Boolean paid;
    private String paymentMethod;
    private List<ServiceInBookingResponse> services;
}