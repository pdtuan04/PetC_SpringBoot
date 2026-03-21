package com.hutech.coca.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "services")
@SQLDelete(sql = "UPDATE services SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên dịch vụ là bắt buộc")
    private String name;

    private String description;

    @Min(value = 0, message = "Giá tiền không được âm")
    private double price;

    @Min(value = 1, message = "Thời gian thực hiện tối thiểu là 1 phút")
    private int durationInMinutes;

    private boolean isActive = true;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;
}