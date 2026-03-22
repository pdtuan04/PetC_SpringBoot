package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class UpdateBookingRequest {
    private Long petId;
    private LocalDateTime scheduledAt;
    private String notes;
    private List<Long> services;
}