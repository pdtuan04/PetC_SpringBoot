package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SelfBookingCheckoutResponse {
    private BookingDetailsResponse booking;
    private PaymentInitResponse payment;
}
