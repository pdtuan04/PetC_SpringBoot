package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentBookingSummaryResponse {
    private Long bookingId;
    private String latestPaymentStatus;
    private String latestPaymentMethod;
    private String latestPaymentProvider;
    private boolean hasSuccessfulPayment;
    private boolean canRetryPayment;
}
