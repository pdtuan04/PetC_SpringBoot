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
public class StaffSummaryResponse {
    private Long id;
    private String staffCode;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String department;
    private String position;
    private String profilePictureUrl;
    private Boolean isActive;
}
