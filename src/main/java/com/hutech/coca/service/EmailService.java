package com.hutech.coca.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
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
        msg.setText("Bạn có lịch hẹn vào ngày mai");
        mailSender.send(msg);
    }
}