package com.hutech.coca.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pets")
@SQLDelete(sql = "UPDATE pets SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên thú cưng là bắt buộc")
    private String name;

    @Min(value = 0, message = "Tuổi không được âm")
    private int age;
    private String imageUrl;
    @NotNull(message = "Phải có chủ sở hữu (User)")
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    @NotNull(message = "Phải chọn loại thú cưng")
    @ManyToOne
    @JoinColumn(name = "pet_type_id")
    private PetType petType;
    @Column(name = "is_deleted")
    private boolean isDeleted = false;
}