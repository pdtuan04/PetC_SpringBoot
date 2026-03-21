package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class BookingDetailsResponse {
    private Long id; // Vẫn xài Long tự tăng theo db hiện tại nhé
    private String bookingCode;
    private LocalDateTime scheduledAt;
    private LocalDateTime expectedEndTime;
    private double totalPrice;
    private String notes;
    private int bookingStatus; // Trả về dạng số (0: Pending, 1: Confirmed...)
    private Long userId;
    private String userName;
    private String petName;
    private List<ServiceInBookingResponse> services;
}