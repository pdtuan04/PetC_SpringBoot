package com.hutech.coca.controller;

import com.hutech.coca.model.User;
import com.hutech.coca.service.UserService;
import com.hutech.coca.utils.JwtUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;


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
                .orElse(ResponseEntity.status(401).body(Map.of("error", "Sai tài khoản hoặc mật khẩu")));
    }
}