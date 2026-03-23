package com.hutech.coca.dto;

import com.hutech.coca.common.PaymentMethod;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentInitRequest {
    private PaymentMethod paymentMethod;
    private String voucherCode;
}
