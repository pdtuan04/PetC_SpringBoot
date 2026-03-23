package com.hutech.coca.service;

import com.hutech.coca.repository.IPaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RevenueService {

    private final IPaymentTransactionRepository paymentTransactionRepository;

    /**
     * Lấy tổng quan doanh thu: tổng kỳ này, tổng kỳ trước, % tăng trưởng
     */
    public Map<String, Object> getRevenueSummary(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.plusDays(1).atStartOfDay();

        long days = to.toEpochDay() - from.toEpochDay() + 1;
        LocalDateTime prevFromDt = from.minusDays(days).atStartOfDay();

        double currentRevenue = paymentTransactionRepository.sumRevenueByPeriod(fromDt, toDt);
        double prevRevenue    = paymentTransactionRepository.sumRevenueByPeriod(prevFromDt, fromDt);

        double growth = prevRevenue == 0 ? 0 : ((currentRevenue - prevRevenue) / prevRevenue) * 100;

        // Breakdown theo phương thức thanh toán
        List<Object[]> methodRows = paymentTransactionRepository.revenueGroupByPaymentMethod(fromDt, toDt);
        Map<String, Double> byMethod = new LinkedHashMap<>();
        for (Object[] row : methodRows) {
            byMethod.put(row[0].toString(), ((Number) row[1]).doubleValue());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRevenue", currentRevenue);
        result.put("prevPeriodRevenue", prevRevenue);
        result.put("growthPercent", Math.round(growth * 10.0) / 10.0);
        result.put("byPaymentMethod", byMethod);
        return result;
    }

    /**
     * Doanh thu theo ngày trong kỳ
     */
    public List<Map<String, Object>> getRevenueByDay(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.plusDays(1).atStartOfDay();

        List<Object[]> rows = paymentTransactionRepository.revenueGroupByDay(fromDt, toDt);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", row[0].toString());
            entry.put("revenue", ((Number) row[1]).doubleValue());
            result.add(entry);
        }
        return result;
    }

    /**
     * Doanh thu theo tháng trong kỳ
     */
    public List<Map<String, Object>> getRevenueByMonth(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.plusDays(1).atStartOfDay();

        List<Object[]> rows = paymentTransactionRepository.revenueGroupByMonth(fromDt, toDt);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month", row[0].toString());
            entry.put("revenue", ((Number) row[1]).doubleValue());
            result.add(entry);
        }
        return result;
    }

    /**
     * Danh sách giao dịch chi tiết
     */
    public List<Map<String, Object>> getTransactionDetails(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.plusDays(1).atStartOfDay();

        return paymentTransactionRepository.findSuccessTransactions(fromDt, toDt)
                .stream()
                .map(tx -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", tx.getId());
                    entry.put("transactionRef", tx.getTransactionRef());
                    entry.put("bookingId", tx.getBooking().getId());
                    entry.put("bookingCode", tx.getBooking().getBookingCode());
                    entry.put("paymentMethod", tx.getPaymentMethod().name());
                    entry.put("amount", tx.getAmount());
                    entry.put("voucherDiscount", tx.getVoucherDiscount() != null ? tx.getVoucherDiscount() : 0);
                    entry.put("netRevenue", tx.getAmount() - (tx.getVoucherDiscount() != null ? tx.getVoucherDiscount() : 0));
                    entry.put("completedAt", tx.getUpdatedAt() != null ? tx.getUpdatedAt().toString() : null);
                    return entry;
                })
                .toList();
    }
}
