package com.hutech.coca.service;

import com.hutech.coca.common.BookingStatus;
import com.hutech.coca.config.BookingOptions;
import com.hutech.coca.dto.BookingDetailsResponse;
import com.hutech.coca.dto.BookingSummaryResponse;
import com.hutech.coca.dto.ServiceInBookingResponse;
import com.hutech.coca.model.Booking;
import com.hutech.coca.repository.IBookingRepository;
import com.hutech.coca.repository.IPetRepository;
import com.hutech.coca.repository.IServiceRepository;
import com.hutech.coca.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final IBookingRepository bookingRepository;
    private final IPetRepository petRepository;
    private final IServiceRepository serviceRepository;
    private final IUserRepository userRepository;
    private final BookingOptions bookingOptions;

    @Transactional
    public boolean confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if (booking.getHoldExpiredAt().isBefore(LocalDateTime.now()) && booking.getBookingStatus() == BookingStatus.PENDING) {
            throw new RuntimeException("Booking hold time has expired. You haven't confirmed yet.");
        }
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        return true;
    }

    @Transactional
    public boolean cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        return true;
    }

    @Transactional
    public void expiredBooking(Long id) {
        Booking booking = bookingRepository.findById(id).orElse(null);
        if (booking != null && booking.getBookingStatus() == BookingStatus.PENDING && booking.getHoldExpiredAt().isBefore(LocalDateTime.now())) {
            booking.setBookingStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
        }
    }

    public List<BookingSummaryResponse> getAllBookingsInWeek(LocalDateTime startTime) {
        LocalDateTime start = startTime.toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(7);

        List<Booking> bookings = bookingRepository.findByScheduledAtGreaterThanEqualAndScheduledAtLessThan(start, end);

        return bookings.stream().map(b -> {
            BookingSummaryResponse dto = new BookingSummaryResponse();
            dto.setId(b.getId());
            dto.setBookingCode(b.getBookingCode());
            dto.setUserName(b.getUser() != null ? b.getUser().getUsername() : "N/A");
            dto.setScheduledAt(b.getScheduledAt());
            dto.setExpectedEndTime(b.getExpectedEndTime());
            dto.setBookingStatus(b.getBookingStatus().ordinal());
            dto.setCreateAt(b.getCreateAt());
            return dto;
        }).collect(Collectors.toList());
    }
    public BookingDetailsResponse getBookingDetail(Long id) {
        // Gọi hàm getBookingDetails tối ưu mình vừa viết ở trên
        Booking booking = bookingRepository.getBookingDetails(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        BookingDetailsResponse dto = new BookingDetailsResponse();
        dto.setId(booking.getId());
        dto.setBookingCode(booking.getBookingCode());
        dto.setScheduledAt(booking.getScheduledAt());
        dto.setExpectedEndTime(booking.getExpectedEndTime());
        dto.setTotalPrice(booking.getTotalPrice());
        dto.setNotes(booking.getNotes());

        // .ordinal() giúp chuyển Enum thành số nguyên (PENDING -> 0, CONFIRMED -> 1...)
        dto.setBookingStatus(booking.getBookingStatus().ordinal());

        dto.setUserId(booking.getUser().getId());
        dto.setUserName(booking.getUser().getUsername());
        dto.setPetName(booking.getPet().getName());

        dto.setServices(booking.getBookingDetails().stream().map(bd -> {
            ServiceInBookingResponse sDto = new ServiceInBookingResponse();
            sDto.setName(bd.getService().getName());
            sDto.setPrice(bd.getPriceAtTime());
            return sDto;
        }).collect(Collectors.toList()));

        return dto;
    }
}
