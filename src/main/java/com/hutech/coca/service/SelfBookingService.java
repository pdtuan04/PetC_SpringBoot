package com.hutech.coca.service;

import com.hutech.coca.common.BookingStatus;
import com.hutech.coca.common.PaymentTransactionStatus;
import com.hutech.coca.dto.*;
import com.hutech.coca.model.Booking;
import com.hutech.coca.model.PaymentTransaction;
import com.hutech.coca.model.Pet;
import com.hutech.coca.model.User;
import com.hutech.coca.repository.IBookingRepository;
import com.hutech.coca.repository.IPaymentTransactionRepository;
import com.hutech.coca.repository.IPetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SelfBookingService {

    private final CurrentUserService currentUserService;
    private final IPetRepository petRepository;
    private final IBookingRepository bookingRepository;
    private final IPaymentTransactionRepository paymentTransactionRepository;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final VoucherService voucherService;

    @Transactional
    public SelfBookingCheckoutResponse checkout(String authorizationHeader, SelfBookingCheckoutRequest request) {
        User currentUser = currentUserService.getCurrentUser(authorizationHeader);

        Pet pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new RuntimeException("Pet not found"));

        if (!pet.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only create booking for your own pet");
        }

        CreateBookingRequest createRequest = new CreateBookingRequest();
        createRequest.setPetId(request.getPetId());
        createRequest.setScheduledAt(request.getScheduledAt());
        createRequest.setNotes(request.getNotes());
        createRequest.setServices(request.getServices());

        BookingDetailsResponse booking = bookingService.createBooking(createRequest, currentUser.getId());

        PaymentInitResponse payment = paymentService.initializePayment(
                booking.getId(),
                currentUser.getId(),
            request.getPaymentMethod(),
            null
        );

        BookingDetailsResponse freshBooking = bookingService.getBookingDetail(booking.getId());

        SelfBookingCheckoutResponse response = new SelfBookingCheckoutResponse();
        response.setBooking(freshBooking);
        response.setPayment(payment);
        return response;
    }

    public BookingDetailsResponse getMyBookingDetail(String authorizationHeader, Long bookingId) {
        User currentUser = currentUserService.getCurrentUser(authorizationHeader);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You cannot access another user's booking");
        }

        return bookingService.getBookingDetail(bookingId);
    }

    public List<BookingSummaryResponse> getMyBookings(String authorizationHeader) {
        User currentUser = currentUserService.getCurrentUser(authorizationHeader);
        return bookingService.getUserBookings(currentUser.getId());
    }

    @Transactional
    public CancelMyBookingResponse cancelMyBooking(String authorizationHeader, Long bookingId) {
        User currentUser = currentUserService.getCurrentUser(authorizationHeader);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You cannot cancel another user's booking");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking is already cancelled");
        }

        if (booking.getBookingStatus() == BookingStatus.COMPLETED || booking.getBookingStatus() == BookingStatus.IN_PROGRESS) {
            throw new RuntimeException("Cannot cancel booking in current status");
        }

        bookingService.cancelBooking(bookingId);

        CancelMyBookingResponse response = new CancelMyBookingResponse();
        response.setBooking(bookingService.getBookingDetail(bookingId));
        response.setVoucherCreated(false);

        PaymentTransaction successfulTx = paymentTransactionRepository
                .findTopByBookingIdAndPaymentStatusOrderByUpdatedAtDesc(bookingId, PaymentTransactionStatus.SUCCESS)
                .orElse(null);

        if (successfulTx != null
                && paymentService.isPrepaidMethod(successfulTx.getPaymentMethod())
                && successfulTx.getAmount() != null
                && successfulTx.getAmount() > 0) {
            var voucher = voucherService.createCancellationVoucher(currentUser, booking, successfulTx.getAmount());
            response.setVoucherCreated(true);
            response.setVoucherCode(voucher.getCode());
            response.setVoucherAmount(voucher.getRemainingAmount());
            response.setMessage("Booking cancelled. A voucher has been created for your next booking.");
        } else {
            response.setMessage("Booking cancelled successfully.");
        }

        return response;
    }

    public List<VoucherSummaryResponse> getMyActiveVouchers(String authorizationHeader) {
        User currentUser = currentUserService.getCurrentUser(authorizationHeader);
        return voucherService.getActiveVouchers(currentUser.getId());
    }
}
