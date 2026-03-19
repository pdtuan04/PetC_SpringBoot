package com.hutech.coca.service;

import com.hutech.coca.model.Invitation;
import com.hutech.coca.model.InvitationStatus;
import com.hutech.coca.repository.IInvitationRepository;
import com.hutech.coca.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final IInvitationRepository invitationRepository;
    private final IUserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public Invitation sendInvitation(String inviterUsername, String inviteeEmail, String appBaseUrl) {
        // Không cho phép tự mời bản thân
        userRepository.findByUsername(inviterUsername).ifPresent(inviter -> {
            if (inviter.getEmail().equalsIgnoreCase(inviteeEmail)) {
                throw new RuntimeException("Bạn không thể tự mời chính mình!");
            }
        });

        // Kiểm tra lời mời đang chờ xử lý cho email này
        invitationRepository.findByInviteeEmailAndStatus(inviteeEmail, InvitationStatus.PENDING)
                .ifPresent(existing -> {
                    throw new RuntimeException("Đã có lời mời đang chờ xử lý cho email này!");
                });

        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        Invitation invitation = Invitation.builder()
                .inviterUsername(inviterUsername)
                .inviteeEmail(inviteeEmail)
                .token(token)
                .status(InvitationStatus.PENDING)
                .createdAt(now)
                .expiresAt(now.plusDays(7))
                .build();

        invitationRepository.save(invitation);

        String acceptUrl = appBaseUrl + "/api/invitations/accept/" + token;
        emailService.sendInvitation(inviteeEmail, inviterUsername, acceptUrl);

        return invitation;
    }

    @Transactional
    public Invitation acceptInvitation(String token) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Lời mời không tồn tại!"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new RuntimeException("Lời mời này đã được xử lý!");
        }

        if (LocalDateTime.now().isAfter(invitation.getExpiresAt())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new RuntimeException("Lời mời đã hết hạn!");
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        return invitationRepository.save(invitation);
    }

    @Transactional
    public Invitation declineInvitation(String token) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Lời mời không tồn tại!"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new RuntimeException("Lời mời này đã được xử lý!");
        }

        invitation.setStatus(InvitationStatus.DECLINED);
        return invitationRepository.save(invitation);
    }

    public List<Invitation> getPendingInvitations(String inviterUsername) {
        return invitationRepository.findByInviterUsernameAndStatus(inviterUsername, InvitationStatus.PENDING);
    }
}
