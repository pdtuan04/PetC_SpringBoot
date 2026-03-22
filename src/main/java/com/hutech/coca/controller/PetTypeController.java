package com.hutech.coca.controller;

import com.hutech.coca.dto.PetTypeResponse;
import com.hutech.coca.service.PetTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pet-type") // Đường dẫn độc lập
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PetTypeController {

    private final PetTypeService petTypeService;

    // API: GET /api/pet-type
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPetTypes() {
        try {
            List<PetTypeResponse> result = petTypeService.getActivePetTypes();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lấy danh sách loại thú cưng thành công.");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi tải loại thú cưng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}