package com.hutech.coca.controller;

import com.hutech.coca.model.Invitation;
import com.hutech.coca.service.InvitationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InvitationController {

    private final InvitationService invitationService;

    /**
     * Gửi lời mời tham gia ứng dụng tới một địa chỉ email.
     * Yêu cầu người dùng đã đăng nhập.
     * Body: { "email": "invitee@example.com" }
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendInvitation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Bạn cần đăng nhập để gửi lời mời!"));
        }

        String inviteeEmail = body.get("email");
        if (inviteeEmail == null || inviteeEmail.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email người được mời là bắt buộc!"));
        }

        try {
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + (request.getServerPort() != 80 && request.getServerPort() != 443
                    ? ":" + request.getServerPort() : "");
            Invitation invitation = invitationService.sendInvitation(
                    userDetails.getUsername(), inviteeEmail, baseUrl);
            return ResponseEntity.ok(Map.of(
                    "message", "Lời mời đã được gửi thành công!",
                    "token", invitation.getToken(),
                    "inviteeEmail", invitation.getInviteeEmail(),
                    "expiresAt", invitation.getExpiresAt().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Chấp nhận lời mời bằng token.
     * Không yêu cầu đăng nhập.
     */
    @PostMapping("/accept/{token}")
    public ResponseEntity<?> acceptInvitation(@PathVariable String token) {
        try {
            Invitation invitation = invitationService.acceptInvitation(token);
            return ResponseEntity.ok(Map.of(
                    "message", "Lời mời đã được chấp nhận!",
                    "inviteeEmail", invitation.getInviteeEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Từ chối lời mời bằng token.
     * Không yêu cầu đăng nhập.
     */
    @PostMapping("/decline/{token}")
    public ResponseEntity<?> declineInvitation(@PathVariable String token) {
        try {
            Invitation invitation = invitationService.declineInvitation(token);
            return ResponseEntity.ok(Map.of(
                    "message", "Lời mời đã bị từ chối.",
                    "inviteeEmail", invitation.getInviteeEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy danh sách lời mời đang chờ xử lý của người dùng hiện tại.
     * Yêu cầu người dùng đã đăng nhập.
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingInvitations(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Bạn cần đăng nhập!"));
        }

        List<Invitation> invitations = invitationService.getPendingInvitations(userDetails.getUsername());
        return ResponseEntity.ok(invitations);
    }
}
