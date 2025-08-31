package com.matteominin.pdf_extractor.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matteominin.pdf_extractor.model.content.Architecture;
import com.matteominin.pdf_extractor.model.content.ContentReport;
import com.matteominin.pdf_extractor.model.content.Requirement;
import com.matteominin.pdf_extractor.model.content.UseCase;
import com.matteominin.pdf_extractor.service.ContentReportService;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/api/content")
public class ContentReportController {

    @Autowired
    private ContentReportService contentReportService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/consolidate")
    public ResponseEntity<?> consolidateExtractedContent(@RequestBody Map<String, Object> body) {
        Object rawReportObj = body.get("raw_report");
        if (rawReportObj == null) {
            return ResponseEntity.badRequest().body("Missing input raw_report");
        }

        try {
            // Properly convert LinkedHashMap objects to ContentReport objects
            List<ContentReport> content = objectMapper.convertValue(rawReportObj,
                    new TypeReference<List<ContentReport>>() {
                    });

            List<UseCase> useCases = new ArrayList<>();
            List<Requirement> requirements = new ArrayList<>();
            List<Architecture> architectures = new ArrayList<>();

            for (ContentReport c : content) {
                useCases.addAll(c.getUseCases());
                requirements.addAll(c.getRequirements());
                architectures.addAll(c.getArchitectures());
            }

            ContentReport consolidatedReport = contentReportService.consolidateReport(useCases, requirements,
                    architectures);
            return ResponseEntity.ok(consolidatedReport);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid raw_report format: " + e.getMessage());
        }
    }
}
