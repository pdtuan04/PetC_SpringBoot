package com.hutech.coca.repository;

import com.hutech.coca.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
