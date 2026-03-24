package com.hutech.coca.controller;

import com.hutech.coca.dto.CreatePetRequest;
import com.hutech.coca.dto.PetResponse;
import com.hutech.coca.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pet")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PetController {

    private final PetService petService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getPetsByUserId(@PathVariable Long userId) {
        try {
            List<PetResponse> result = petService.getUserPets(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lấy danh sách thú cưng thành công");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi tải danh sách thú cưng");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> createPetForUser(
            @PathVariable Long userId,
            @RequestBody CreatePetRequest request) {
        try {
            PetResponse result = petService.createUserPet(userId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Thêm thú cưng cho khách hàng thành công.");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/user/{userId}/{petId}")
    public ResponseEntity<Map<String, Object>> updatePetForUser(
            @PathVariable Long userId,
            @PathVariable Long petId,
            @RequestBody CreatePetRequest request) {
        try {
            PetResponse result = petService.updateUserPet(userId, petId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật thú cưng thành công.");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/user/{userId}/{petId}")
    public ResponseEntity<Map<String, Object>> deletePetForUser(
            @PathVariable Long userId,
            @PathVariable Long petId) {
        try {
            petService.deleteUserPet(userId, petId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Xóa thú cưng thành công.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}