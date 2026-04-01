package com.hutech.coca.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PetTypeResponse {
    private Long id;
    private String name;
    
    private String description;
    private String image;
    
    @JsonProperty("isActive")
    private boolean isActive;
    
    private LocalDateTime createAt;
}