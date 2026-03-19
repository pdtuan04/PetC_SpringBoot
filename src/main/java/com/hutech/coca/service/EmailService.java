package com.hutech.coca.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOTP(String to, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Xác nhận đổi điểm");
        message.setText("Mã xác nhận của bạn là: " + otp);

        mailSender.send(message);
    }

    public void sendInvitation(String to, String inviterUsername, String acceptUrl) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Lời mời tham gia ứng dụng PetC");
        message.setText("Xin chào,\n\n"
                + inviterUsername + " đã mời bạn tham gia ứng dụng PetC.\n\n"
                + "Nhấn vào liên kết sau để chấp nhận lời mời:\n"
                + acceptUrl + "\n\n"
                + "Liên kết có hiệu lực trong 7 ngày.\n\n"
                + "Trân trọng,\nĐội ngũ PetC");

        mailSender.send(message);
    }
}