package com.hutech.coca.controller;

import com.hutech.coca.dto.ServiceResponse;
import com.hutech.coca.service.ServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/service") // Đã đồng bộ chữ thường theo ý bạn
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ServiceController {

    private final ServiceService serviceService;

    // API: GET /api/service
    @GetMapping
    public ResponseEntity<Map<String, Object>> getActiveServices() {
        try {
            List<ServiceResponse> result = serviceService.getAllActiveServices();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lấy danh sách dịch vụ thành công.");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi tải dịch vụ: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    @GetMapping("/pet-type/{petTypeId}")
    public ResponseEntity<Map<String, Object>> getServicesByPetType(@PathVariable Long petTypeId) {
        try {
            List<ServiceResponse> result = serviceService.getActiveServicesByPetType(petTypeId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lấy dịch vụ theo loại thú cưng thành công.");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}