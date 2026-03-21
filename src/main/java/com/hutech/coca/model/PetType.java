package com.hutech.coca.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pet_types")
@SQLDelete(sql = "UPDATE pet_types SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class PetType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên loại thú cưng là bắt buộc")
    private String name;

    private boolean isActive = true;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;
}