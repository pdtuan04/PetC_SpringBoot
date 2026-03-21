package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class BookingRequest {
    private LocalDateTime scheduledAt;
    private String notes;
    private Long petId;
    private List<Long> services;
}