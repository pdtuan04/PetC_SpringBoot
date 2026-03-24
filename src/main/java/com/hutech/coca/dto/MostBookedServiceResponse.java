package com.hutech.coca.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MostBookedServiceResponse {
    private Long serviceId;
    private String serviceName;
    private Long bookingCount;
}
