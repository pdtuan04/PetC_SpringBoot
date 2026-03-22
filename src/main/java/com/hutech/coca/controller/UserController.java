package com.hutech.coca.controller;
import com.hutech.coca.dto.UserSummaryResponse;
import com.hutech.coca.service.UserService;
import com.hutech.coca.model.User;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUserByEmail(@RequestParam String email) {
        try {
            UserSummaryResponse result = userService.getUserByEmail(email);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tìm thấy khách hàng");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response); // Trả về 404 nếu không thấy
        }
    }
}