package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmBookingRequest {
    private Long bookingId; // Chuyển từ Guid sang Long
}