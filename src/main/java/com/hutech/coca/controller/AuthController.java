package com.hutech.coca.controller;

import com.hutech.coca.dto.GoogleLoginRequest;
import com.hutech.coca.model.Role;
import com.hutech.coca.model.User;
import com.hutech.coca.service.AuthService;
import com.hutech.coca.service.UserService;
import com.hutech.coca.utils.JwtUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private String googleClientId;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            userService.registerNewUser(user);
            return ResponseEntity.ok(Map.of("message", "Đăng ký thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request, HttpServletResponse response) {
        String username = request.get("username");
        String password = request.get("password");

        return userService.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(user -> {
                    // Truyền cả quyền hạn vào Token
                    String token = jwtUtils.generateToken(user.getUsername(), user.getAuthorities());
                    Cookie jwtCookie = new Cookie("jwt", token);
                    jwtCookie.setHttpOnly(true);     // Bảo mật: JS không đọc được
                    jwtCookie.setSecure(false);       // Đặt là true nếu dùng HTTPS
                    jwtCookie.setPath("/");          // Cookie có hiệu lực toàn trang
                    jwtCookie.setMaxAge(24 * 60 * 60); // Hết hạn sau 1 ngày (giây)

                    response.addCookie(jwtCookie);
                    return ResponseEntity.ok(Map.of(
                            "token", token,
                            "id", user.getId(),
                            "username", user.getUsername(),
                            "roles", user.getRoles().stream().map(r -> r.getName()).toList()
                    ));
                })
                .orElse(ResponseEntity.status(401).body(Map.of(
                        "error", "Sai tài khoản hoặc mật khẩu",
                        "message", "Sai tài khoản hoặc mật khẩu"
                )));
    }
    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> authenticateGoogleUser(@RequestBody GoogleLoginRequest request) {
        try {
            Map<String, Object> responseData = authService.authenticateWithGoogle(request.getToken());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đăng nhập Google thành công");
            response.put("data", responseData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi server: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}