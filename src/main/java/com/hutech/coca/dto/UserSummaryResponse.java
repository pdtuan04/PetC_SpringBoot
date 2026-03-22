package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSummaryResponse {
    private Long id;
    private String username;
    private String phone;
    private String email;
}