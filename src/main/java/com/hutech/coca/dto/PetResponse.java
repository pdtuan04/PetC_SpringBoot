package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PetResponse {
    private Long id;
    private String name;
    private int age;
    private String imageUrl;
}
