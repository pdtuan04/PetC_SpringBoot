package com.hutech.coca.controller;

import com.hutech.coca.dto.PetTypeRequest;
import com.hutech.coca.dto.PetTypeResponse;
import com.hutech.coca.service.PetTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pet-type")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PetTypeController {

    private final PetTypeService petTypeService;

    // GET /api/pet-type
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPetTypes() {
        try {
            List<PetTypeResponse> result = petTypeService.getActivePetTypes();
            return buildResponse(true, "Lấy danh sách loại thú cưng thành công.", result);
        } catch (Exception e) {
            return buildErrorResponse("Lỗi khi tải loại thú cưng: " + e.getMessage());
        }
    }

    // API: GET /api/pet-type/paginated
    @GetMapping("/paginated")
    public ResponseEntity<Map<String, Object>> getPaginatedList(
            @RequestParam(required = false) String Search,
            @RequestParam(defaultValue = "id") String SortBy,
            @RequestParam(defaultValue = "Ascending") String SortDir,
            @RequestParam(defaultValue = "1") int PageNumber,
            @RequestParam(defaultValue = "10") int PageSize) {
        try {
            Map<String, Object> result = petTypeService.getPaginatedList(Search, SortBy, SortDir, PageNumber, PageSize);
            return buildResponse(true, "Lấy dữ liệu thành công.", result);
        } catch (Exception e) {
            return buildErrorResponse("Lỗi khi lấy dữ liệu: " + e.getMessage());
        }
    }

    // API: GET /api/pet-type/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPetTypeById(@PathVariable Long id) {
        try {
            PetTypeResponse result = petTypeService.getPetTypeById(id);
            return buildResponse(true, "Lấy dữ liệu thành công.", result);
        } catch (Exception e) {
            return buildErrorResponse("Lỗi khi lấy dữ liệu: " + e.getMessage());
        }
    }

    // API: POST /api/pet-type
    @PostMapping
    public ResponseEntity<Map<String, Object>> createPetType(@RequestBody PetTypeRequest request) {
        try {
            PetTypeResponse result = petTypeService.createPetType(request);
            return buildResponse(true, "Thêm loại thú cưng thành công.", result);
        } catch (Exception e) {
            return buildErrorResponse("Lỗi khi thêm: " + e.getMessage());
        }
    }

    // API: PUT /api/pet-type
    @PutMapping
    public ResponseEntity<Map<String, Object>> updatePetType(@RequestBody PetTypeRequest request) {
        try {
            PetTypeResponse result = petTypeService.updatePetType(request);
            return buildResponse(true, "Cập nhật loại thú cưng thành công.", result);
        } catch (Exception e) {
            return buildErrorResponse("Lỗi khi cập nhật: " + e.getMessage());
        }
    }

    // API: PATCH /api/pet-type/soft-delete
    @PatchMapping("/soft-delete")
    public ResponseEntity<Map<String, Object>> softDeletePetType(@RequestParam Long id) {
        try {
            petTypeService.softDeletePetType(id);
            return buildResponse(true, "Vô hiệu hóa thành công.", null);
        } catch (Exception e) {
            return buildErrorResponse("Lỗi khi vô hiệu hóa: " + e.getMessage());
        }
    }

    // API: PATCH /api/pet-type/active
    @PatchMapping("/active")
    public ResponseEntity<Map<String, Object>> activePetType(@RequestParam Long id) {
        try {
            petTypeService.activePetType(id);
            return buildResponse(true, "Kích hoạt thành công.", null);
        } catch (Exception e) {
            return buildErrorResponse("Lỗi khi kích hoạt: " + e.getMessage());
        }
    }

    // API: DELETE /api/pet-type/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePetType(@PathVariable Long id) {
        try {
            petTypeService.deletePetType(id);
            return buildResponse(true, "Xóa loại thú cưng thành công.", null);
        } catch (Exception e) {
            return buildErrorResponse("Lỗi khi xóa: " + e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> buildResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.badRequest().body(response);
    }
}