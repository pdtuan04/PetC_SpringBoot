package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentInitResponse {
    private String transactionRef;
    private String paymentMethod;
    private String paymentStatus;
    private String paymentUrl;
    private String message;
    private Double totalAmount;
    private Double voucherDiscount;
    private Double payableAmount;
    private String voucherCode;
}
