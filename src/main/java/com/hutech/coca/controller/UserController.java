package com.hutech.coca.controller;

import com.hutech.coca.dto.UpdateProfileRequest;
import com.hutech.coca.dto.UpdateRoleRequest;
import com.hutech.coca.dto.UserProfileResponse;
import com.hutech.coca.dto.UserSummaryResponse;
import com.hutech.coca.model.Role;
import com.hutech.coca.model.User;
import com.hutech.coca.service.CurrentUserService;
import com.hutech.coca.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final CurrentUserService currentUserService;

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
            return ResponseEntity.status(404).body(response);
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

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyProfile(
            @RequestHeader("Authorization") String authorization) {
        try {
            UserProfileResponse result = userService.toUserProfile(currentUserService.getCurrentUser(authorization));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateMyProfile(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody UpdateProfileRequest request) {
        try {
            UserProfileResponse result = userService.updateMyProfile(
                    currentUserService.getCurrentUser(authorization),
                    request
            );
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật hồ sơ thành công");
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserDetails(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId);
            List<Long> currentRoleIds = user.getRoles().stream()
                    .map(Role::getId)
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("email", user.getEmail());
            data.put("currentRoleIds", currentRoleIds);

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