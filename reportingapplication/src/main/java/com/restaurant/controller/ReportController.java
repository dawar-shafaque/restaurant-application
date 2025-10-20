package com.restaurant.controller;

import com.restaurant.service.ReportSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportSchedulerService reportSchedulerService;

    @GetMapping
    public String home() {
        return "Application is running correctly";
    }

    @PostMapping("/send-reports")
    public ResponseEntity<Map<String, String>> sendWeeklyDeltaReport() {
        reportSchedulerService.generateAndSendWeeklyReport();
        return ResponseEntity.ok(Map.of("message", "Report sent successfully"));
    }
}