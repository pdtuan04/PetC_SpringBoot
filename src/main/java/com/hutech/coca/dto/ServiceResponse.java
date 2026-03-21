package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceResponse {
    private Long id;
    private String name;
    private String description;
    private double price;
    private int durationInMinutes;
}