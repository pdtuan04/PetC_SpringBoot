package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class AvailableSlotResponse {
    private LocalDateTime startAt;
    private LocalDateTime endAt;
}