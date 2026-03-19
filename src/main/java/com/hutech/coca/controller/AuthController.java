package com.hutech.coca.controller;

import com.hutech.coca.model.User;
import com.hutech.coca.service.UserService;
import com.hutech.coca.utils.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody User user) {
        try {
            userService.save(user);
            userService.setDefaultRole(user.getUsername());
            return ResponseEntity.ok(Map.of("message", "Đăng ký thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Đăng ký thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        // Tìm user và kiểm tra mật khẩu
        return userService.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(user -> {
                    String token = jwtUtils.generateToken(user.getUsername());
                    return ResponseEntity.ok(Map.of(
                            "token", token,
                            "username", user.getUsername(),
                            "roles", user.getRoles().stream().map(r -> r.getName()).toList()
                    ));
                })
                .orElse(ResponseEntity.status(401).body(Map.of("error", "Sai tài khoản hoặc mật khẩu")));
    }
}