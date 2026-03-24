package com.hutech.coca.service;

import com.hutech.coca.common.BookingStatus;
import com.hutech.coca.common.PaymentMethod;
import com.hutech.coca.common.PaymentTransactionStatus;
import com.hutech.coca.config.BookingOptions;
import com.hutech.coca.dto.*;
import com.hutech.coca.model.*;
import com.hutech.coca.repository.IBookingRepository;
import com.hutech.coca.repository.IPaymentTransactionRepository;
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
    private final IPaymentTransactionRepository paymentTransactionRepository;
    private final BookingOptions bookingOptions;
    private final EmailService emailService;
    private final JobScheduler jobScheduler;

    // =======================================================================
    // THUẬT TOÁN ĐẾM THÔNG MINH: CẮT NHỎ THỜI GIAN MỖI 5 PHÚT ĐỂ KIỂM TRA
    // =======================================================================
    private boolean isSlotAvailable(LocalDateTime start, LocalDateTime end, List<Booking> bookingsInDay, Long excludeBookingId) {
        LocalDateTime checkPoint = start;

        // Bước nhảy 5 phút đảm bảo độ chính xác tuyệt đối
        while (checkPoint.isBefore(end)) {
            final LocalDateTime currentCheck = checkPoint;

            long concurrentBookings = bookingsInDay.stream()
                    .filter(b -> b.getBookingStatus() != BookingStatus.CANCELLED)
                    .filter(b -> excludeBookingId == null || !b.getId().equals(excludeBookingId))
                    // Chỉ đếm khách ĐANG LÀM DỊCH VỤ tại chính xác phút này
                    .filter(b -> !b.getScheduledAt().isAfter(currentCheck) && b.getExpectedEndTime().isAfter(currentCheck))
                    .count();

            if (concurrentBookings >= bookingOptions.getMaxBookingsPerSlot()) {
                return false; // Phát hiện quá tải tại thời điểm này -> Báo lỗi ngay
            }
            checkPoint = checkPoint.plusMinutes(5);
        }
        return true;
    }

    @Transactional
    public BookingDetailsResponse createBooking(CreateBookingRequest request, Long userId) {
        if (request.getScheduledAt() == null) {
            throw new RuntimeException("Scheduled time is required");
        }

        if (!request.getScheduledAt().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Không thể đặt lịch ở thời điểm đã qua.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account null"));

        Pet pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new RuntimeException("Pet not found"));

        List<com.hutech.coca.model.Service> services = serviceRepository.findAllById(request.getServices())
                .stream().filter(com.hutech.coca.model.Service::getIsActive).toList();

        if (services.size() != request.getServices().size()) {
            throw new RuntimeException("One or more services not found");
        }

        int totalMinutes = services.stream().mapToInt(com.hutech.coca.model.Service::getDurationInMinutes).sum();
        LocalDateTime expectedEndTime = request.getScheduledAt().plusMinutes(totalMinutes);

        // --- GỌI HÀM KIỂM TRA THÔNG MINH ---
        LocalDateTime startOfDay = request.getScheduledAt().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        List<Booking> bookingsInDay = bookingRepository.findByScheduledAtGreaterThanEqualAndScheduledAtLessThanAndIsDeletedFalse(startOfDay, endOfDay);

        if (!isSlotAvailable(request.getScheduledAt(), expectedEndTime, bookingsInDay, null)) {
            throw new RuntimeException("Khung giờ này đã kín lịch, vui lòng chọn giờ khác.");
        }

        double totalPrice = services.stream().mapToDouble(com.hutech.coca.model.Service::getPrice).sum();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime holdExpiredAt = now.plusMinutes(bookingOptions.getHoldMinutes());

        String bookingCode = "BK" + now.format(DateTimeFormatter.ofPattern("yyMMdd"))
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

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

        bookingRepository.save(booking);
        jobScheduler.schedule(
                holdExpiredAt,
                () -> this.expiredBooking(booking.getId())
        );

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
        User user = booking.getUser();
        if (user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
                String formattedTime = booking.getScheduledAt().format(formatter);
                emailService.sendBookingConfirmationWithQR(
                        user.getEmail(),
                        user.getUsername(),
                        booking.getBookingCode(),
                        formattedTime
                );
                System.out.println("✅ Đã kích hoạt gửi mail xác nhận cho Booking: " + booking.getBookingCode());
            } catch (Exception e) {
                System.err.println("❌ Lỗi khi gọi gửi mail: " + e.getMessage());
            }
        }
        return true;
    }

    @Transactional
    public void expiredBooking(Long id) {
        Booking booking = bookingRepository.findById(id).orElse(null);
        if (booking != null
                && (booking.getBookingStatus() == BookingStatus.PENDING || booking.getBookingStatus() == BookingStatus.PENDING_PAYMENT)
                && booking.getHoldExpiredAt().isBefore(LocalDateTime.now())) {
            booking.setBookingStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
        }
    }

    @Transactional
    public void markBookingPendingPayment(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Booking is not in pending state to start payment.");
        }

        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        bookingRepository.save(booking);
    }

    @Transactional
    public void confirmBookingAfterPayment(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking was cancelled and cannot be confirmed.");
        }

        if (booking.getHoldExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Booking hold time has expired.");
        }

        booking.setBookingStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        User user = booking.getUser();
        if (user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
                String formattedTime = booking.getScheduledAt().format(formatter);
                emailService.sendBookingConfirmationWithQR(
                        user.getEmail(),
                        user.getUsername(),
                        booking.getBookingCode(),
                        formattedTime
                );
            } catch (Exception e) {
                System.err.println("Failed to send confirmation email after payment: " + e.getMessage());
            }
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

    // --- LẤY DANH SÁCH GIỜ TRỐNG ---
    public List<AvailableSlotResponse> getAvailableBookingSlots(int durationInMinutes, LocalDateTime selectedDay) {
        LocalDateTime startOfDayQuery = selectedDay.toLocalDate().atStartOfDay();
        LocalDateTime endOfDayQuery = startOfDayQuery.plusDays(1);
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = bookingRepository.findByScheduledAtGreaterThanEqualAndScheduledAtLessThanAndIsDeletedFalse(startOfDayQuery, endOfDayQuery);
        List<AvailableSlotResponse> availableSlots = new ArrayList<>();

        LocalDateTime currentTime = startOfDayQuery.plusHours(bookingOptions.getOpeningHour());
        LocalDateTime endOfDay = startOfDayQuery.plusHours(bookingOptions.getClosingHour());

        while (!currentTime.isAfter(endOfDay)) {
            LocalDateTime slotEndTime = currentTime.plusMinutes(durationInMinutes);

            // BẢO VỆ NHÂN VIÊN: Nếu giờ kết thúc dự kiến lố qua giờ đóng cửa -> Bỏ qua, không hiển thị nút
            if (slotEndTime.isAfter(endOfDay)) {
                currentTime = currentTime.plusMinutes(bookingOptions.getSlotDurationMinutes());
                continue;
            }

            // GỌI HÀM CHECK THÔNG MINH Ở ĐÂY
            if (currentTime.isAfter(now) && isSlotAvailable(currentTime, slotEndTime, bookings, null)) {
                AvailableSlotResponse slot = new AvailableSlotResponse();
                slot.setStartAt(currentTime);
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

            List<PaymentTransaction> txList = b.getPaymentTransactions();
            if (txList != null && !txList.isEmpty()) {
                PaymentTransaction latestTx = txList.stream()
                        .filter(tx -> tx.getPaymentStatus() == PaymentTransactionStatus.SUCCESS)
                        .findFirst()
                        .orElse(txList.get(txList.size() - 1));

                response.setPaid(latestTx.getPaymentStatus() == PaymentTransactionStatus.SUCCESS);
                if (latestTx.getPaymentMethod() != null) {
                    response.setPaymentMethod(latestTx.getPaymentMethod().name());
                }
            } else {
                response.setPaid(false);
            }
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

        if (booking.getUser() != null) {
            response.setUserId(booking.getUser().getId());
            response.setUserName(booking.getUser().getUsername());
        }

        if (booking.getPet() != null) {
            response.setPetId(booking.getPet().getId());
            response.setPetName(booking.getPet().getName());
        }
        if (booking.getBookingDetails() != null) {
            response.setServices(booking.getBookingDetails().stream().map(bd -> {
                ServiceInBookingResponse sRes = new ServiceInBookingResponse();
                sRes.setName(bd.getService().getName());
                sRes.setId(bd.getService().getId());
                sRes.setPrice(bd.getPriceAtTime());
                return sRes;
            }).collect(Collectors.toList()));
        }
        paymentTransactionRepository
                .findTopByBookingIdAndPaymentStatusOrderByUpdatedAtDesc(id, PaymentTransactionStatus.SUCCESS)
                .ifPresentOrElse(
                        successTx -> {
                            response.setPaid(true);
                            if (successTx.getPaymentMethod() != null) {
                                response.setPaymentMethod(successTx.getPaymentMethod().name());
                            }
                        },
                        () -> {
                            paymentTransactionRepository.findTopByBookingIdOrderByUpdatedAtDesc(id)
                                    .ifPresentOrElse(latestTx -> {
                                        response.setPaid(false);
                                        if (latestTx.getPaymentMethod() != null) {
                                            response.setPaymentMethod(latestTx.getPaymentMethod().name());
                                        }
                                    }, () -> {
                                        response.setPaid(false);
                                        response.setPaymentMethod(null);
                                    });
                        }
                );
        return response;
    }

    public List<BookingSummaryResponse> getUserBookings(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        return bookings.stream().map(b -> {
            BookingSummaryResponse response = new BookingSummaryResponse();
            response.setId(b.getId());
            response.setBookingCode(b.getBookingCode());
            response.setPetId(b.getPet() != null ? b.getPet().getId() : null);
            response.setPetName(b.getPet() != null ? b.getPet().getName() : null);
            response.setUserName(b.getUser() != null ? b.getUser().getUsername() : null);
            response.setBookingStatus(b.getBookingStatus().ordinal());
            response.setCreateAt(b.getCreateAt());
            response.setExpectedEndTime(b.getExpectedEndTime());
            response.setScheduledAt(b.getScheduledAt());
            response.setTotalPrice(b.getTotalPrice());
            response.setNotes(b.getNotes());
            List<PaymentTransaction> txList = b.getPaymentTransactions();
            if (txList != null && !txList.isEmpty()) {
                PaymentTransaction latestTx = txList.stream()
                        .filter(tx -> tx.getPaymentStatus() == PaymentTransactionStatus.SUCCESS)
                        .findFirst()
                        .orElse(txList.get(txList.size() - 1));

                response.setPaid(latestTx.getPaymentStatus() == PaymentTransactionStatus.SUCCESS);
                if (latestTx.getPaymentMethod() != null) {
                    response.setPaymentMethod(latestTx.getPaymentMethod().name());
                }
            } else {
                response.setPaid(false);
            }
            return response;
        }).collect(Collectors.toList());
    }

    public List<BookingSummaryResponse> getUserBookingsByPetId(Long userId, Long petId) {
        List<Booking> bookings = bookingRepository.findByUserIdAndPetId(userId, petId);
        return bookings.stream().map(b -> {
            BookingSummaryResponse response = new BookingSummaryResponse();
            response.setId(b.getId());
            response.setBookingCode(b.getBookingCode());
            response.setPetId(b.getPet() != null ? b.getPet().getId() : null);
            response.setPetName(b.getPet() != null ? b.getPet().getName() : null);
            response.setUserName(b.getUser() != null ? b.getUser().getUsername() : null);
            response.setBookingStatus(b.getBookingStatus().ordinal());
            response.setCreateAt(b.getCreateAt());
            response.setExpectedEndTime(b.getExpectedEndTime());
            response.setScheduledAt(b.getScheduledAt());
            response.setTotalPrice(b.getTotalPrice());
            response.setNotes(b.getNotes());

            List<PaymentTransaction> txList = b.getPaymentTransactions();
            if (txList != null && !txList.isEmpty()) {
                PaymentTransaction latestTx = txList.stream()
                        .filter(tx -> tx.getPaymentStatus() == PaymentTransactionStatus.SUCCESS)
                        .findFirst()
                        .orElse(txList.get(txList.size() - 1));

                response.setPaid(latestTx.getPaymentStatus() == PaymentTransactionStatus.SUCCESS);
                if (latestTx.getPaymentMethod() != null) {
                    response.setPaymentMethod(latestTx.getPaymentMethod().name());
                }
            } else {
                response.setPaid(false);
            }
            return response;
        }).collect(Collectors.toList());
    }

    @Transactional
    public BookingDetailsResponse updateBooking(Long bookingId, UpdateBookingRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (request.getScheduledAt() == null) {
            throw new RuntimeException("Scheduled time is required");
        }

        if (!request.getScheduledAt().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Không thể cập nhật lịch về thời điểm đã qua.");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED || booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Không thể sửa lịch hẹn đã hoàn thành hoặc đã hủy.");
        }

        Pet pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new RuntimeException("Pet not found"));

        List<com.hutech.coca.model.Service> services = serviceRepository.findAllById(request.getServices())
                .stream().filter(com.hutech.coca.model.Service::getIsActive).toList();

        if (services.size() != request.getServices().size()) {
            throw new RuntimeException("One or more services not found");
        }

        int totalMinutes = services.stream().mapToInt(com.hutech.coca.model.Service::getDurationInMinutes).sum();
        LocalDateTime expectedEndTime = request.getScheduledAt().plusMinutes(totalMinutes);

        // --- GỌI HÀM KIỂM TRA THÔNG MINH (TRUYỀN ID ĐỂ NGOẠI TRỪ LỊCH ĐANG SỬA) ---
        LocalDateTime startOfDay = request.getScheduledAt().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        List<Booking> bookingsInDay = bookingRepository.findByScheduledAtGreaterThanEqualAndScheduledAtLessThanAndIsDeletedFalse(startOfDay, endOfDay);

        if (!isSlotAvailable(request.getScheduledAt(), expectedEndTime, bookingsInDay, bookingId)) {
            throw new RuntimeException("Khung giờ này đã kín lịch, vui lòng chọn giờ khác.");
        }

        double totalPrice = services.stream().mapToDouble(com.hutech.coca.model.Service::getPrice).sum();

        booking.setPet(pet);
        booking.setScheduledAt(request.getScheduledAt());
        booking.setExpectedEndTime(expectedEndTime);
        booking.setTotalPrice(totalPrice);
        booking.setNotes(request.getNotes());

        // --- CÁCH SỬA LỖI NHÂN ĐÔI DỊCH VỤ (UPDATE THÔNG MINH) ---
        List<Long> newServiceIds = services.stream().map(com.hutech.coca.model.Service::getId).toList();
        booking.getBookingDetails().removeIf(detail -> !newServiceIds.contains(detail.getService().getId()));

        for (com.hutech.coca.model.Service s : services) {
            boolean alreadyExists = booking.getBookingDetails().stream()
                    .anyMatch(detail -> detail.getService().getId().equals(s.getId()));

            if (!alreadyExists) {
                BookingDetail newDetail = new BookingDetail();
                newDetail.setBooking(booking);
                newDetail.setService(s);
                newDetail.setPriceAtTime(s.getPrice());
                booking.getBookingDetails().add(newDetail);
            }
        }

        bookingRepository.save(booking);
        return getBookingDetail(bookingId);
    }

    @Transactional
    public boolean deleteBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setDeleted(true);
        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        return true;
    }

    public BookingDetailsResponse getBookingDetailByCode(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn/lịch hẹn với mã này!"));

        return getBookingDetail(booking.getId());
    }

    @Transactional
    public boolean startBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn."));

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Chỉ có thể tiến hành khi lịch hẹn đang ở trạng thái 'Đã xác nhận'.");
        }

        booking.setBookingStatus(BookingStatus.IN_PROGRESS);
        bookingRepository.save(booking);
        return true;
    }

    @Transactional
    public boolean completeBooking(Long bookingId, PaymentMethod finalPaymentMethod) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn."));

        if (booking.getBookingStatus() != BookingStatus.IN_PROGRESS) {
            throw new RuntimeException("Chỉ có thể hoàn thành lịch hẹn đang được tiến hành.");
        }

        booking.setBookingStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        java.util.Optional<PaymentTransaction> existingTx = paymentTransactionRepository.findTopByBookingIdOrderByUpdatedAtDesc(bookingId);

        if (existingTx.isPresent()) {
            PaymentTransaction tx = existingTx.get();
            if (tx.getPaymentStatus() != PaymentTransactionStatus.SUCCESS) {
                if (finalPaymentMethod != null) {
                    tx.setPaymentMethod(finalPaymentMethod);
                }
                tx.setPaymentStatus(PaymentTransactionStatus.SUCCESS);
                tx.setAmount(booking.getTotalPrice());
                paymentTransactionRepository.save(tx);
            }
        } else {
            PaymentTransaction newTx = new PaymentTransaction();
            newTx.setBooking(booking);
            newTx.setUser(booking.getUser());
            newTx.setPaymentMethod(finalPaymentMethod != null ? finalPaymentMethod : PaymentMethod.PAY_LATER);
            newTx.setPaymentStatus(PaymentTransactionStatus.SUCCESS);
            newTx.setAmount(booking.getTotalPrice() != null ? booking.getTotalPrice() : 0.0);
            newTx.setTransactionRef("COUNTER_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
            newTx.setPaymentProvider("AT_COUNTER");
            paymentTransactionRepository.save(newTx);
        }

        return true;
    }

    @Transactional
    public boolean markCashPaid(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn."));

        if (booking.getBookingStatus() == BookingStatus.CANCELLED
                || booking.getBookingStatus() == BookingStatus.NO_SHOW) {
            throw new RuntimeException("Không thể ghi nhận thanh toán tiền mặt cho lịch đã hủy/vắng mặt.");
        }

        java.util.Optional<PaymentTransaction> existingTx =
                paymentTransactionRepository.findTopByBookingIdOrderByUpdatedAtDesc(bookingId);

        if (existingTx.isPresent()) {
            PaymentTransaction tx = existingTx.get();
            tx.setPaymentMethod(PaymentMethod.CASH);
            tx.setPaymentStatus(PaymentTransactionStatus.SUCCESS);
            tx.setAmount(booking.getTotalPrice() != null ? booking.getTotalPrice() : 0.0);
            tx.setPaymentProvider("AT_COUNTER");
            paymentTransactionRepository.save(tx);
        } else {
            PaymentTransaction newTx = new PaymentTransaction();
            newTx.setBooking(booking);
            newTx.setUser(booking.getUser());
            newTx.setPaymentMethod(PaymentMethod.CASH);
            newTx.setPaymentStatus(PaymentTransactionStatus.SUCCESS);
            newTx.setAmount(booking.getTotalPrice() != null ? booking.getTotalPrice() : 0.0);
            newTx.setTransactionRef(
                    "CASH_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase()
            );
            newTx.setPaymentProvider("AT_COUNTER");
            paymentTransactionRepository.save(newTx);
        }

        if (booking.getBookingStatus() == BookingStatus.PENDING
                || booking.getBookingStatus() == BookingStatus.PENDING_PAYMENT) {
            confirmBookingAfterPayment(bookingId);
        }

        return true;
    }

    @Transactional
    public boolean markNoShow(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn."));

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Chỉ có thể báo vắng mặt khi lịch hẹn đang ở trạng thái 'Đã xác nhận'.");
        }

        booking.setBookingStatus(BookingStatus.NO_SHOW);
        bookingRepository.save(booking);
        return true;
    }
}