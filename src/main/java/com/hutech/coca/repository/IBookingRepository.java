package com.hutech.coca.repository;

import com.hutech.coca.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import com.hutech.coca.dto.MostBookedServiceResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IBookingRepository extends JpaRepository<Booking, Long> {
    // Tìm Booking trong khoảng thời gian (Dùng cho check trùng lịch và xem tuần)
    List<Booking> findByScheduledAtGreaterThanEqualAndScheduledAtLessThanAndIsDeletedFalse(LocalDateTime startTime, LocalDateTime endTime);

    // Lấy danh sách Booking của 1 User
    List<Booking> findByUserId(Long userId);
    
    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.user " +
            "JOIN FETCH b.pet " +
            "LEFT JOIN FETCH b.bookingDetails bd " +
            "LEFT JOIN FETCH bd.service " +
            "WHERE b.id = :id AND b.isDeleted = false")
    Optional<Booking> getBookingDetails(@Param("id") Long id);
    
    // Đếm số booking có sử dụng service (qua BookingDetail)
    @Query("SELECT COUNT(DISTINCT b) FROM Booking b " +
           "JOIN b.bookingDetails bd " +
           "WHERE bd.service.id = :serviceId AND b.isDeleted = false")
    long countByServiceId(@Param("serviceId") Long serviceId);
    Optional<Booking> findByBookingCode(String bookingCode);
    // Lấy danh sách dịch vụ được đặt nhiều nhất
    @Query("SELECT new com.hutech.coca.dto.MostBookedServiceResponse(s.id, s.name, COUNT(bd.id)) " +
            "FROM BookingDetail bd " +
            "JOIN bd.service s " +
            "JOIN bd.booking b " +
            "WHERE b.isDeleted = false " +
            "GROUP BY s.id, s.name " +
            "ORDER BY COUNT(bd.id) DESC")
    List<MostBookedServiceResponse> findMostBookedServices(Pageable pageable);
}
