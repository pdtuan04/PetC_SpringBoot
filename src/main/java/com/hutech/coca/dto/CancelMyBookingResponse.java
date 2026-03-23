package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelMyBookingResponse {
    private BookingDetailsResponse booking;
    private boolean voucherCreated;
    private String voucherCode;
    private Double voucherAmount;
    private String message;
}
