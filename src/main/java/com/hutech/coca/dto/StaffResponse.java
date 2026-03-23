package com.hutech.coca.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffResponse {
    private Long id;
    private String staffCode;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private LocalDate dateOfBirth;
    private LocalDate hireDate;
    private String department;
    private String position;
    private String specialization;
    private String profilePictureUrl;
    private Boolean isActive;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
