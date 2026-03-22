package com.hutech.coca.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendWelcome(String to, String fullName) {
        try {
        Context ctx = new Context();
        ctx.setVariable("fullName", fullName);

        // nếu file nằm ở: src/main/resources/templates/welcome.html
        String html = templateEngine.process("mail/welcome", ctx);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

        helper.setTo(to);
        helper.setSubject("[Welcome!]");
        helper.setText(html, true); // true => HTML

        mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Send email failed", e);
        }
    }
    public void sendRemindSchedule(String to, String fullName) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("[Nhắc lịch]");
        msg.setText("Bạn có lịch hẹn vào ngày");
        mailSender.send(msg);
    }
    @Async
    public void sendBookingConfirmationWithQR(String toEmail, String userName, String bookingCode, String scheduledTime) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(bookingCode, BarcodeFormat.QR_CODE, 250, 250);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] qrCodeImage = pngOutputStream.toByteArray();
            Context ctx = new Context();
            ctx.setVariable("userName", userName);
            ctx.setVariable("scheduledTime", scheduledTime);
            ctx.setVariable("bookingCode", bookingCode);
            String htmlMsg = templateEngine.process("mail/booking-confirm", ctx);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("🎉 Đặt lịch PetSpa thành công - Mã: " + bookingCode);
            helper.setText(htmlMsg, true);
            helper.addInline("qrCodeImage", new ByteArrayResource(qrCodeImage), "image/png");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi email QR: " + e.getMessage());
        }
    }
}