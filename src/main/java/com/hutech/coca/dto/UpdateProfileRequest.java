package com.hutech.coca.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
public class UpdateProfileRequest {
    @Email(message = "Email không hợp lệ")
    @Size(min = 1, max = 50, message = "Email phải từ 1 đến 50 ký tự")
    private String email;

    @Length(min = 10, max = 10, message = "Số điện thoại phải có 10 chữ số")
    @Pattern(regexp = "^[0-9]*$", message = "Số điện thoại chỉ được chứa số")
    private String phone;
}
