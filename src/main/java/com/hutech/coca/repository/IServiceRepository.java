package com.hutech.coca.repository;

import com.hutech.coca.model.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByIsActiveTrue();
    
    @Query("SELECT s FROM Service s WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Service> findAllWithSearch(@Param("search") String search, Pageable pageable);
    
    // Kiểm tra tên dịch vụ đã tồn tại (không phân biệt hoa thường)
    boolean existsByNameIgnoreCase(String name);
    
    // Kiểm tra tên dịch vụ trùng khi update (loại trừ ID hiện tại)
    @Query("SELECT COUNT(s) > 0 FROM Service s WHERE LOWER(s.name) = LOWER(:name) AND s.id != :id")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Long id);
    @Query("SELECT s FROM Service s JOIN s.petTypes pt WHERE pt.id = :petTypeId AND s.isActive = true AND s.isDeleted = false")
    List<Service> findByPetTypeIdAndIsActiveTrue(@Param("petTypeId") Long petTypeId);
}
