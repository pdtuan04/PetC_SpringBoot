package com.hutech.coca.controller;

import com.hutech.coca.dto.MostBookedServiceResponse;
import com.hutech.coca.service.StatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StatisticController {

    private final StatisticService statisticService;

    @GetMapping("/most-booked-services")
    public ResponseEntity<Map<String, Object>> getMostBookedServices(
            @RequestParam(defaultValue = "5") int limit) {
        try {
            List<MostBookedServiceResponse> result = statisticService.getMostBookedServices(limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lấy thống kê dịch vụ thành công.");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/most-booked-services/export")
    public ResponseEntity<byte[]> exportMostBookedServices(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "csv") String format) {
        try {
            byte[] fileData;
            String fileName;
            MediaType mediaType;

            if ("pdf".equalsIgnoreCase(format)) {
                fileData = statisticService.exportMostBookedServicesToPdf(limit);
                fileName = "most-booked-services.pdf";
                mediaType = MediaType.APPLICATION_PDF;
            } else {
                fileData = statisticService.exportMostBookedServicesToCsv(limit);
                fileName = "most-booked-services.csv";
                mediaType = MediaType.parseMediaType("text/csv");
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(mediaType)
                    .body(fileData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
