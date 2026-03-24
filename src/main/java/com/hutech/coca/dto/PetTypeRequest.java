package com.hutech.coca.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class PetTypeRequest {
    private Long id;

    @NotBlank(message = "Tên loại thú cưng là bắt buộc")
    private String name;

    @JsonProperty("isActive")
    private boolean isActive = true;
}
