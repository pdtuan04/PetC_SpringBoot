package com.hutech.coca.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStaffResponse {
    private Long staffId;
    private String staffCode;
    private String fullName;
    private String email;
    private String username;
    private String temporaryPassword;
    private Boolean mustChangePassword;
}
