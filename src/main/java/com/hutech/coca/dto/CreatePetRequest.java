package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePetRequest {
    private String name;
    private Long petTypeId;
    private int age;
    private String imageUrl;
}
