package com.hutech.coca.controller;

import com.hutech.coca.model.Service;
import com.hutech.coca.service.ServiceManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ServiceManagementController {
    
    private final ServiceManagementService serviceManagementService;
    
    @GetMapping("/paginated")
    public ResponseEntity<Map<String, Object>> getAllServices(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "Ascending") String sortDir) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Page<Service> servicePage = serviceManagementService.getAllServices(
                search, pageNumber, pageSize, sortBy, sortDir);
            
            Map<String, Object> data = new HashMap<>();
            data.put("items", servicePage.getContent());
            data.put("totalCount", servicePage.getTotalElements());
            data.put("pageNumber", pageNumber);
            data.put("pageSize", pageSize);
            
            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getServiceById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Service service = serviceManagementService.getServiceById(id);
            response.put("success", true);
            response.put("data", service);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        }
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createService(@RequestBody Service service) {
        Map<String, Object> response = new HashMap<>();
        try {
            Service created = serviceManagementService.createService(service);
            response.put("success", true);
            response.put("message", "Thêm dịch vụ thành công");
            response.put("data", created);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PutMapping
    public ResponseEntity<Map<String, Object>> updateService(@RequestBody Service service) {
        Map<String, Object> response = new HashMap<>();
        try {
            Service updated = serviceManagementService.updateService(service);
            response.put("success", true);
            response.put("message", "Cập nhật dịch vụ thành công");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PatchMapping("/toggle-active")
    public ResponseEntity<Map<String, Object>> toggleActive(@RequestParam Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            serviceManagementService.toggleActive(id);
            response.put("success", true);
            response.put("message", "Cập nhật trạng thái thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PatchMapping("/soft-delete")
    public ResponseEntity<Map<String, Object>> softDelete(@RequestParam Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            serviceManagementService.softDelete(id);
            response.put("success", true);
            response.put("message", "Vô hiệu hóa dịch vụ thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
