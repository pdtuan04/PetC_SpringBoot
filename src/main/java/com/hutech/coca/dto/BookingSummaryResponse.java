package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BookingSummaryResponse {
    private Long id;
    private String bookingCode;
    private String userName;
    private LocalDateTime scheduledAt;
    private LocalDateTime expectedEndTime;
    private int bookingStatus;
    private LocalDateTime createAt;
    private boolean isPaid;
    private String paymentMethod;
}