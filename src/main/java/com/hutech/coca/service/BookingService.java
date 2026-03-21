package com.hutech.coca.service;

import com.hutech.coca.common.BookingStatus;
import com.hutech.coca.config.BookingOptions;
import com.hutech.coca.dto.*;
import com.hutech.coca.model.*;
import com.hutech.coca.repository.IBookingRepository;
import com.hutech.coca.repository.IPetRepository;
import com.hutech.coca.repository.IServiceRepository;
import com.hutech.coca.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final IBookingRepository bookingRepository;
    private final IPetRepository petRepository;
    private final IServiceRepository serviceRepository;
    private final IUserRepository userRepository;
    private final BookingOptions bookingOptions;

    // Sử dụng JobRunr thay thế cho Hangfire
    private final JobScheduler jobScheduler;

    @Transactional
    public BookingDetailsResponse createBooking(CreateBookingRequest request, Long userId) {

        // 1. Kiểm tra tài khoản và thú cưng
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account null"));

        Pet pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new RuntimeException("Pet not found"));

        // 2. Kiểm tra danh sách dịch vụ
        List<com.hutech.coca.model.Service> services = serviceRepository.findAllById(request.getServices())
                .stream().filter(com.hutech.coca.model.Service::isActive).toList();

        if (services.size() != request.getServices().size()) {
            throw new RuntimeException("One or more services not found");
        }

        // 3. Tính toán thời lượng và giờ kết thúc
        int totalMinutes = services.stream().mapToInt(com.hutech.coca.model.Service::getDurationInMinutes).sum();
        LocalDateTime expectedEndTime = request.getScheduledAt().plusMinutes(totalMinutes);

        // 4. Kiểm tra slot trống
        LocalDateTime startOfDay = request.getScheduledAt().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        List<Booking> bookingsInDay = bookingRepository.findByScheduledAtGreaterThanEqualAndScheduledAtLessThanAndIsDeletedFalse(startOfDay, endOfDay);

        long overlapCount = bookingsInDay.stream()
                .filter(b -> b.getScheduledAt().isBefore(expectedEndTime)
                        && b.getExpectedEndTime().isAfter(request.getScheduledAt())
                        && b.getBookingStatus() != BookingStatus.CANCELLED)
                .count();

        if (overlapCount >= bookingOptions.getMaxBookingsPerSlot()) {
            throw new RuntimeException("BookingNoAvailableSlotException"); // Bạn có thể thay bằng Custom Exception của bạn
        }

        // 5. Tính tổng tiền và cấu hình thời gian Hold
        double totalPrice = services.stream().mapToDouble(com.hutech.coca.model.Service::getPrice).sum();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime holdExpiredAt = now.plusMinutes(bookingOptions.getHoldMinutes());

        String bookingCode = "BK" + now.format(DateTimeFormatter.ofPattern("yyMMdd"))
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        // 6. Tạo Entity
        Booking booking = new Booking();
        booking.setBookingCode(bookingCode);
        booking.setScheduledAt(request.getScheduledAt());
        booking.setExpectedEndTime(expectedEndTime);
        booking.setTotalPrice(totalPrice);
        booking.setNotes(request.getNotes());
        booking.setBookingStatus(BookingStatus.PENDING);
        booking.setHoldExpiredAt(holdExpiredAt);
        booking.setUser(user);
        booking.setPet(pet);

        List<BookingDetail> details = services.stream().map(s -> {
            BookingDetail detail = new BookingDetail();
            detail.setBooking(booking);
            detail.setService(s);
            detail.setPriceAtTime(s.getPrice());
            return detail;
        }).collect(Collectors.toList());

        booking.setBookingDetails(details);

        // Lưu xuống DB để có ID
        bookingRepository.save(booking);

        // 7. JobRunr lên lịch tự động hủy nếu quá giờ (Giống hệt Hangfire)
        jobScheduler.schedule(
                holdExpiredAt,
                () -> this.expiredBooking(booking.getId())
        );

        // 8. Map ra Response trả về
        BookingDetailsResponse response = new BookingDetailsResponse();
        response.setId(booking.getId());
        response.setBookingCode(booking.getBookingCode());
        response.setScheduledAt(booking.getScheduledAt());
        response.setExpectedEndTime(expectedEndTime);
        response.setTotalPrice(totalPrice);
        response.setNotes(booking.getNotes());
        response.setBookingStatus(booking.getBookingStatus().ordinal());
        response.setUserId(user.getId());
        response.setUserName(user.getUsername());
        response.setPetName(pet.getName());

        response.setServices(services.stream().map(s -> {
            ServiceInBookingResponse sRes = new ServiceInBookingResponse();
            sRes.setName(s.getName());
            sRes.setPrice(s.getPrice());
            return sRes;
        }).collect(Collectors.toList()));

        return response;
    }

    @Transactional
    public boolean confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getHoldExpiredAt().isBefore(LocalDateTime.now()) && booking.getBookingStatus() == BookingStatus.PENDING) {
            throw new RuntimeException("Booking hold time has expired. You haven't confirmed yet :((");
        }

        booking.setBookingStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        return true;
    }

    // Hàm chạy ngầm của JobRunr
    @Transactional
    public void expiredBooking(Long id) {
        Booking booking = bookingRepository.findById(id).orElse(null);
        if (booking != null && booking.getBookingStatus() == BookingStatus.PENDING && booking.getHoldExpiredAt().isBefore(LocalDateTime.now())) {
            booking.setBookingStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
        }
    }

    @Transactional
    public boolean cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        return true;
    }

    public List<AvailableSlotResponse> getAvailableBookingSlots(int durationInMinutes, LocalDateTime selectedDay) {
        LocalDateTime startOfDayQuery = selectedDay.toLocalDate().atStartOfDay();
        LocalDateTime endOfDayQuery = startOfDayQuery.plusDays(1);

        List<Booking> bookings = bookingRepository.findByScheduledAtGreaterThanEqualAndScheduledAtLessThanAndIsDeletedFalse(startOfDayQuery, endOfDayQuery);
        List<AvailableSlotResponse> availableSlots = new ArrayList<>();

        LocalDateTime currentTime = startOfDayQuery.plusHours(bookingOptions.getOpeningHour());
        LocalDateTime endOfDay = startOfDayQuery.plusHours(bookingOptions.getClosingHour());

        while (!currentTime.isAfter(endOfDay)) {
            LocalDateTime slotEndTime = currentTime.plusMinutes(durationInMinutes);
            final LocalDateTime currentCheckTime = currentTime;

            long overlapCount = bookings.stream()
                    .filter(b -> currentCheckTime.isBefore(b.getExpectedEndTime())
                            && slotEndTime.isAfter(b.getScheduledAt())
                            && b.getBookingStatus() != BookingStatus.CANCELLED)
                    .count();

            if (overlapCount < bookingOptions.getMaxBookingsPerSlot()) {
                AvailableSlotResponse slot = new AvailableSlotResponse();
                slot.setStartAt(currentCheckTime);
                slot.setEndAt(slotEndTime);
                availableSlots.add(slot);
            }
            currentTime = currentTime.plusMinutes(bookingOptions.getSlotDurationMinutes());
        }
        return availableSlots;
    }

    public List<BookingSummaryResponse> getAllBookingsInWeek(LocalDateTime startTime) {
        LocalDateTime start = startTime.toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(7);

        List<Booking> bookings = bookingRepository.findByScheduledAtGreaterThanEqualAndScheduledAtLessThanAndIsDeletedFalse(start, end);

        return bookings.stream().map(b -> {
            BookingSummaryResponse response = new BookingSummaryResponse();
            response.setId(b.getId());
            response.setBookingCode(b.getBookingCode());
            response.setUserName(b.getUser() != null ? b.getUser().getUsername() : "N/A");
            response.setScheduledAt(b.getScheduledAt());
            response.setExpectedEndTime(b.getExpectedEndTime());
            response.setBookingStatus(b.getBookingStatus().ordinal());
            response.setCreateAt(b.getCreateAt());
            return response;
        }).collect(Collectors.toList());
    }

    public BookingDetailsResponse getBookingDetail(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        BookingDetailsResponse response = new BookingDetailsResponse();
        response.setId(booking.getId());
        response.setBookingCode(booking.getBookingCode());
        response.setScheduledAt(booking.getScheduledAt());
        response.setExpectedEndTime(booking.getExpectedEndTime());
        response.setTotalPrice(booking.getTotalPrice());
        response.setNotes(booking.getNotes());
        response.setBookingStatus(booking.getBookingStatus().ordinal());
        response.setUserId(booking.getUser().getId());
        response.setPetId(booking.getPet().getId());
        response.setUserName(booking.getUser().getUsername());
        response.setPetName(booking.getPet().getName());

        response.setServices(booking.getBookingDetails().stream().map(bd -> {
            ServiceInBookingResponse sRes = new ServiceInBookingResponse();
            sRes.setName(bd.getService().getName());
            sRes.setId(bd.getService().getId());
            sRes.setPrice(bd.getPriceAtTime());
            return sRes;
        }).collect(Collectors.toList()));

        return response;
    }

    public List<BookingSummaryResponse> getUserBookings(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        return bookings.stream().map(b -> {
            BookingSummaryResponse response = new BookingSummaryResponse();
            response.setId(b.getId());
            response.setBookingCode(b.getBookingCode());
            response.setBookingStatus(b.getBookingStatus().ordinal());
            response.setCreateAt(b.getCreateAt());
            response.setExpectedEndTime(b.getExpectedEndTime());
            response.setScheduledAt(b.getScheduledAt());
            return response;
        }).collect(Collectors.toList());
    }
    @Transactional
    public BookingDetailsResponse updateBooking(Long bookingId, UpdateBookingRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Chỉ cho phép sửa nếu lịch đang chờ hoặc đã xác nhận
        if (booking.getBookingStatus() == BookingStatus.CANCELLED || booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Không thể sửa lịch hẹn đã hoàn thành hoặc đã hủy.");
        }

        Pet pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new RuntimeException("Pet not found"));

        List<com.hutech.coca.model.Service> services = serviceRepository.findAllById(request.getServices())
                .stream().filter(com.hutech.coca.model.Service::isActive).toList();

        if (services.size() != request.getServices().size()) {
            throw new RuntimeException("One or more services not found");
        }

        int totalMinutes = services.stream().mapToInt(com.hutech.coca.model.Service::getDurationInMinutes).sum();
        LocalDateTime expectedEndTime = request.getScheduledAt().plusMinutes(totalMinutes);

        // Kiểm tra xem slot giờ mới có trống không (Phải loại trừ chính ID của booking hiện tại ra)
        LocalDateTime startOfDay = request.getScheduledAt().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        List<Booking> bookingsInDay = bookingRepository.findByScheduledAtGreaterThanEqualAndScheduledAtLessThanAndIsDeletedFalse(startOfDay, endOfDay);

        long overlapCount = bookingsInDay.stream()
                .filter(b -> !b.getId().equals(bookingId)) // LOẠI TRỪ CHÍNH NÓ
                .filter(b -> b.getScheduledAt().isBefore(expectedEndTime)
                        && b.getExpectedEndTime().isAfter(request.getScheduledAt())
                        && b.getBookingStatus() != BookingStatus.CANCELLED)
                .count();

        if (overlapCount >= bookingOptions.getMaxBookingsPerSlot()) {
            throw new RuntimeException("Khung giờ này đã kín lịch, vui lòng chọn giờ khác.");
        }

        double totalPrice = services.stream().mapToDouble(com.hutech.coca.model.Service::getPrice).sum();

        // Cập nhật các thông tin cơ bản
        booking.setPet(pet);
        booking.setScheduledAt(request.getScheduledAt());
        booking.setExpectedEndTime(expectedEndTime);
        booking.setTotalPrice(totalPrice);
        booking.setNotes(request.getNotes());

        // Cập nhật danh sách dịch vụ (Xóa cũ, đắp mới)
        booking.getBookingDetails().clear(); // Nhờ orphanRemoval=true, dòng này sẽ xóa data trong DB

        List<BookingDetail> newDetails = services.stream().map(s -> {
            BookingDetail detail = new BookingDetail();
            detail.setBooking(booking);
            detail.setService(s);
            detail.setPriceAtTime(s.getPrice());
            return detail;
        }).collect(Collectors.toList());

        booking.getBookingDetails().addAll(newDetails);

        bookingRepository.save(booking);

        // Tái sử dụng hàm getBookingDetail để trả về data mới nhất
        return getBookingDetail(bookingId);
    }
    @Transactional
    public boolean deleteBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Đánh dấu là đã xóa và set trạng thái thành Hủy
        booking.setDeleted(true);
        booking.setBookingStatus(BookingStatus.CANCELLED);

        bookingRepository.save(booking);
        return true;
    }
}