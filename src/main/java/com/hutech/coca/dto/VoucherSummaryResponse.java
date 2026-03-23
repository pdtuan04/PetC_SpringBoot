package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class VoucherSummaryResponse {
    private Long id;
    private String code;
    private Double amount;
    private Double remainingAmount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;
}
