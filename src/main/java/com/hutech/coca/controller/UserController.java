package com.hutech.coca.controller;
import com.hutech.coca.dto.UpdateRoleRequest;
import com.hutech.coca.dto.UserSummaryResponse;
import com.hutech.coca.model.Role;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @GetMapping("/roles")
    public ResponseEntity<Map<String, Object>> getAllRoles() {
        try {
            List<Map<String, Object>> roles = userService.getAllRolesClean();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", roles);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // API 2: Xem chi tiết 1 User (kèm theo các quyền họ ĐANG CÓ)
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserDetails(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId);

            // Ép kiểu danh sách Role thành danh sách ID để Frontend dễ check (VD: [1, 2])
            List<Long> currentRoleIds = user.getRoles().stream()
                    .map(Role::getId)
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("email", user.getEmail());
            data.put("currentRoleIds", currentRoleIds); // Trả về cho Frontend tick sẵn checkbox

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // API 3: Lưu các quyền mới cho User
    @PutMapping("/{userId}/roles")
    public ResponseEntity<Map<String, Object>> updateUserRoles(
            @PathVariable Long userId,
            @RequestBody UpdateRoleRequest request) {
        try {
            userService.updateUserRoles(userId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã cập nhật quyền thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        try {
            // Gọi qua Service
            List<UserSummaryResponse> users = userService.getAllUsers();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", users);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}