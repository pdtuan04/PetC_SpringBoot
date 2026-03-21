package com.hutech.coca.repository;

import com.hutech.coca.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface IBookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingCode(String bookingCode);
}
