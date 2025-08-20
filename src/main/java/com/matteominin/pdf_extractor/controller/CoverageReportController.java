package com.matteominin.pdf_extractor.controller;

import com.matteominin.pdf_extractor.model.*;
import com.matteominin.pdf_extractor.service.CoverageReportService;
import com.matteominin.pdf_extractor.service.SummaryFeatureService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/coverage-reports")
public class CoverageReportController {
    
    private static final Logger logger = LoggerFactory.getLogger(CoverageReportController.class);
    
    @Autowired
    private CoverageReportService coverageReportService;
    
    @Autowired
    private SummaryFeatureService summaryFeatureService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Save a coverage report
     * POST /api/coverage-reports
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveCoverageReport(@RequestBody Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Extract data from request
            String reportName = (String) requestBody.get("reportName");
            String description = (String) requestBody.get("description");
            Double threshold = requestBody.get("threshold") != null ? 
                              ((Number) requestBody.get("threshold")).doubleValue() : 0.85;
            
            // Extract and convert features
            Object featuresObj = requestBody.get("features");
            List<Feature> providedFeatures = extractFeatures(featuresObj);
            
            if (providedFeatures.isEmpty()) {
                response.put("success", false);
                response.put("error", "Features list cannot be null or empty");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (reportName == null || reportName.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Report name is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Get summary features and perform analysis
            var summaryFeatures = summaryFeatureService.getAllSummaryFeatures();
            Coverage coverage = coverageReportService.convertToCoverageModel(
                summaryFeatures, providedFeatures, threshold
            );
            
            // Create and save coverage report
            CoverageReport coverageReport = coverageReportService.createCoverageReport(
                coverage,
                providedFeatures.size(),
                threshold,
                summaryFeatures.size(),
                reportName,
                description
            );
            
            CoverageReport savedReport = coverageReportService.saveCoverageReport(coverageReport);
            
            response.put("success", true);
            response.put("id", savedReport.getId());
            response.put("reportName", savedReport.getReportName());
            response.put("coveragePercentage", coverage.getCoveragePercentage());
            response.put("coveredCount", coverage.getCoveredCount());
            response.put("uncoveredCount", coverage.getUncoveredCount());
            response.put("message", "Coverage report saved successfully");
            
            logger.info("Coverage report '{}' saved successfully with ID: {}", reportName, savedReport.getId());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid coverage report data: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("Error saving coverage report: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Failed to save coverage report: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get all coverage reports
     * GET /api/coverage-reports
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCoverageReports() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<CoverageReport> reports = coverageReportService.getAllCoverageReports();
            
            response.put("success", true);
            response.put("count", reports.size());
            response.put("reports", reports);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving coverage reports: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Failed to retrieve coverage reports: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get coverage report by ID
     * GET /api/coverage-reports/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCoverageReportById(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            CoverageReport report = coverageReportService.getCoverageReportById(id);
            
            response.put("success", true);
            response.put("report", report);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid coverage report ID: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                response.put("success", false);
                response.put("error", e.getMessage());
                return ResponseEntity.notFound().build();
            }
            logger.error("Error retrieving coverage report: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Failed to retrieve coverage report: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Validate and analyze features (without saving)
     * POST /api/coverage-reports/analyze
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeCoverage(@RequestBody Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Double threshold = requestBody.get("threshold") != null ? 
                              ((Number) requestBody.get("threshold")).doubleValue() : 0.85;
            
            // Extract and convert features
            Object featuresObj = requestBody.get("features");
            List<Feature> providedFeatures = extractFeatures(featuresObj);
            
            if (providedFeatures.isEmpty()) {
                response.put("success", false);
                response.put("error", "Features list cannot be null or empty");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Get summary features and perform analysis
            var summaryFeatures = summaryFeatureService.getAllSummaryFeatures();
            Coverage coverage = coverageReportService.convertToCoverageModel(
                summaryFeatures, providedFeatures, threshold
            );
            
            response.put("success", true);
            response.put("threshold", threshold);
            response.put("totalSummaryFeatures", summaryFeatures.size());
            response.put("providedFeatures", providedFeatures.size());
            response.put("coverage", coverage);
            
            logger.info("Coverage analysis completed");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during coverage analysis: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Failed to analyze coverage: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    private List<Feature> extractFeatures(Object featuresObj) {
        List<Feature> providedFeatures = new ArrayList<>();
        
        if (featuresObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> featuresList = (List<Object>) featuresObj;
            
            for (Object featureObj : featuresList) {
                if (featureObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> featureMap = (Map<String, Object>) featureObj;
                    
                    Feature feature = objectMapper.convertValue(featureMap, Feature.class);
                    providedFeatures.add(feature);
                }
            }
        }
        
        return providedFeatures;
    }
}
