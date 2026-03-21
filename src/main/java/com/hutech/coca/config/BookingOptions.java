package com.hutech.coca.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.booking")
public class BookingOptions {
    private int maxBookingsPerSlot = 3;
    private int openingHour = 9;
    private int closingHour = 18;
    private int slotDurationMinutes = 30;
    private int holdMinutes = 10;
}