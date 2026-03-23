package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MomoWebhookRequest {
    private String transactionRef;
    private String providerTransactionId;
    private Boolean success;
    private String rawPayload;

    // Real MoMo IPN fields
    private String orderId;
    private String requestId;
    private Integer resultCode;
    private Long transId;
    private String signature;
    private String amount;
    private String extraData;
    private String message;
    private String orderInfo;
    private String orderType;
    private String partnerCode;
    private String payType;
    private String responseTime;
}
