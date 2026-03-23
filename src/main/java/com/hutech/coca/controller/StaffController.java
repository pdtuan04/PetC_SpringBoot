package com.hutech.coca.controller;

import com.hutech.coca.dto.CreateStaffResponse;
import com.hutech.coca.dto.StaffRequest;
import com.hutech.coca.dto.StaffResponse;
import com.hutech.coca.dto.StaffSummaryResponse;
import com.hutech.coca.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StaffController {

    private final StaffService staffService;

    @GetMapping("/paginated")
    public ResponseEntity<Map<String, Object>> getAllStaff(
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "Ascending") String sortDir) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Page<StaffSummaryResponse> staffPage = staffService.getAllStaff(
                pageNumber, pageSize, search, sortBy, sortDir);

            Map<String, Object> data = new HashMap<>();
            data.put("items", staffPage.getContent());
            data.put("totalCount", staffPage.getTotalElements());
            data.put("pageNumber", pageNumber);
            data.put("pageSize", pageSize);
            data.put("totalPages", staffPage.getTotalPages());

            response.put("success", true);
            response.put("message", "Lấy danh sách nhân viên thành công");
            response.put("data", data);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi tải danh sách nhân viên: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getStaffById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            StaffResponse staff = staffService.getStaffById(id);

            response.put("success", true);
            response.put("message", "Lấy thông tin nhân viên thành công");
            response.put("data", staff);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createStaff(@Valid @RequestBody StaffRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            CreateStaffResponse result = staffService.createStaff(request);

            response.put("success", true);
            response.put("message", "Tạo tài khoản nhân viên thành công. Email đăng nhập đã được gửi.");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateStaff(
            @PathVariable Long id,
            @Valid @RequestBody StaffRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            StaffResponse result = staffService.updateStaff(id, request);

            response.put("success", true);
            response.put("message", "Cập nhật thông tin nhân viên thành công");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PatchMapping("/toggle-active")
    public ResponseEntity<Map<String, Object>> toggleActive(@RequestParam Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            staffService.toggleActive(id);

            response.put("success", true);
            response.put("message", "Cập nhật trạng thái nhân viên thành công");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteStaff(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            staffService.deleteStaff(id);

            response.put("success", true);
            response.put("message", "Xóa nhân viên thành công");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/count-active")
    public ResponseEntity<Map<String, Object>> countActiveStaff() {
        Map<String, Object> response = new HashMap<>();
        try {
            long count = staffService.countActiveStaff();

            response.put("success", true);
            response.put("message", "Lấy số lượng nhân viên đang hoạt động thành công");
            response.put("data", count);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Test endpoint to check roles
    @GetMapping("/test/check-roles")
    public ResponseEntity<Map<String, Object>> checkRoles() {
        Map<String, Object> response = new HashMap<>();
        try {
            // This will help debug role issues
            response.put("success", true);
            response.put("message", "Check console for role information");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
