package com.hutech.coca.service;

import com.hutech.coca.model.Booking;
import com.hutech.coca.repository.IBookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final IBookingRepository bookingRepository;

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    public Booking saveBooking(Booking booking) {
        if (booking.getBookingDetails() != null) {
            booking.getBookingDetails().forEach(detail -> detail.setBooking(booking));
        }
        return bookingRepository.save(booking);
    }

    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }
}
