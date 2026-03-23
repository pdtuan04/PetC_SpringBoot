package com.hutech.coca.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "staff")
@SQLDelete(sql = "UPDATE staff SET is_deleted = true WHERE id = ?")
@SQLRestriction("(is_deleted = false OR is_deleted IS NULL)")
public class Staff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_code", length = 20, unique = true, nullable = false)
    @NotBlank(message = "Mã nhân viên là bắt buộc")
    private String staffCode;

    @Column(name = "full_name", length = 100, nullable = false)
    @NotBlank(message = "Họ tên là bắt buộc")
    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    private String fullName;

    @Column(name = "email", length = 100, unique = true, nullable = false)
    @NotBlank(message = "Email là bắt buộc")
    @Email(message = "Email không hợp lệ")
    private String email;

    @Column(name = "phone_number", length = 10, unique = true)
    @Length(min = 10, max = 10, message = "Số điện thoại phải có 10 chữ số")
    @Pattern(regexp = "^[0-9]*$", message = "Số điện thoại chỉ được chứa số")
    private String phoneNumber;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "position", length = 50)
    private String position;

    @Column(name = "specialization", length = 255)
    private String specialization;

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
