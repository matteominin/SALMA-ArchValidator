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
import com.matteominin.pdf_extractor.model.content.ContentReport;
import com.matteominin.pdf_extractor.model.content.Requirement;
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
            // Parse the raw_report as a list of lists of maps (matching the input structure)
            List<List<Map<String, Object>>> rawLists = objectMapper.convertValue(rawReportObj,
                    new TypeReference<List<List<Map<String, Object>>>>() {});

            // Assuming all inner lists contain Requirement objects, collect them all
            List<Requirement> allRequirements = new ArrayList<>();
            for (List<Map<String, Object>> innerList : rawLists) {
                List<Requirement> reqs = objectMapper.convertValue(innerList,
                        new TypeReference<List<Requirement>>() {});
                allRequirements.addAll(reqs);
            }

            // Consolidate into a single ContentReport (with all as requirements, others empty)
            ContentReport consolidatedReport = contentReportService.consolidateReport(
                    new ArrayList<>(), allRequirements, new ArrayList<>(), new ArrayList<>());
            return ResponseEntity.ok(consolidatedReport);
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.badRequest().body("Invalid raw_report format: " + e.getMessage());
        }
    }
}
