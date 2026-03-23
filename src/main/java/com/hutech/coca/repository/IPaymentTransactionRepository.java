package com.hutech.coca.repository;

import com.hutech.coca.common.PaymentTransactionStatus;
import com.hutech.coca.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IPaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByTransactionRef(String transactionRef);
    Optional<PaymentTransaction> findTopByBookingIdOrderByUpdatedAtDesc(Long bookingId);
    Optional<PaymentTransaction> findTopByBookingIdAndPaymentStatusOrderByUpdatedAtDesc(Long bookingId, PaymentTransactionStatus paymentStatus);
    boolean existsByBookingIdAndPaymentStatus(Long bookingId, PaymentTransactionStatus paymentStatus);

    /** Tổng doanh thu các giao dịch SUCCESS trong khoảng thời gian */
    @Query("SELECT COALESCE(SUM(t.amount - COALESCE(t.voucherDiscount, 0)), 0) FROM PaymentTransaction t " +
           "WHERE t.paymentStatus = 'SUCCESS' AND t.updatedAt >= :from AND t.updatedAt < :to")
    Double sumRevenueByPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Doanh thu theo ngày: trả về [date_string, total] */
    @Query("SELECT FUNCTION('DATE', t.updatedAt), " +
           "COALESCE(SUM(t.amount - COALESCE(t.voucherDiscount, 0)), 0) " +
           "FROM PaymentTransaction t " +
           "WHERE t.paymentStatus = 'SUCCESS' AND t.updatedAt >= :from AND t.updatedAt < :to " +
           "GROUP BY FUNCTION('DATE', t.updatedAt) ORDER BY FUNCTION('DATE', t.updatedAt)")
    List<Object[]> revenueGroupByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Doanh thu theo tháng: trả về [year_month_string, total] */
    @Query("SELECT CONCAT(FUNCTION('YEAR', t.updatedAt), '-', LPAD(FUNCTION('MONTH', t.updatedAt), 2, '0')), " +
           "COALESCE(SUM(t.amount - COALESCE(t.voucherDiscount, 0)), 0) " +
           "FROM PaymentTransaction t " +
           "WHERE t.paymentStatus = 'SUCCESS' AND t.updatedAt >= :from AND t.updatedAt < :to " +
           "GROUP BY FUNCTION('YEAR', t.updatedAt), FUNCTION('MONTH', t.updatedAt) " +
           "ORDER BY FUNCTION('YEAR', t.updatedAt), FUNCTION('MONTH', t.updatedAt)")
    List<Object[]> revenueGroupByMonth(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Doanh thu theo phương thức thanh toán */
    @Query("SELECT t.paymentMethod, COALESCE(SUM(t.amount - COALESCE(t.voucherDiscount, 0)), 0) " +
           "FROM PaymentTransaction t " +
           "WHERE t.paymentStatus = 'SUCCESS' AND t.updatedAt >= :from AND t.updatedAt < :to " +
           "GROUP BY t.paymentMethod")
    List<Object[]> revenueGroupByPaymentMethod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Danh sách giao dịch thành công trong kỳ */
    @Query("SELECT t FROM PaymentTransaction t " +
           "WHERE t.paymentStatus = 'SUCCESS' AND t.updatedAt >= :from AND t.updatedAt < :to " +
           "ORDER BY t.updatedAt DESC")
    List<PaymentTransaction> findSuccessTransactions(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
