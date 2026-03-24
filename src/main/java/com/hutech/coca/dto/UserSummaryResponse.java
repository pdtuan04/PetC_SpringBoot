package com.hutech.coca.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class UserSummaryResponse {
    private Long id;
    private String username;
    private String phone;
    private String email;
    private List<Map<String, Object>> roles;
}