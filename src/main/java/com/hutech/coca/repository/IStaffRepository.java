package com.hutech.coca.repository;

import com.hutech.coca.model.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IStaffRepository extends JpaRepository<Staff, Long> {
    
    Optional<Staff> findByStaffCode(String staffCode);
    
    Optional<Staff> findByEmail(String email);
    
    Optional<Staff> findByPhoneNumber(String phoneNumber);
    
    Optional<Staff> findByUserId(Long userId);
    
    @Query("SELECT s FROM Staff s WHERE " +
           "(LOWER(s.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.staffCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "s.isDeleted = false")
    Page<Staff> searchStaff(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT COUNT(s) FROM Staff s WHERE s.isActive = true AND s.isDeleted = false")
    long countActiveStaff();
}
