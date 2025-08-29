package com.matteominin.pdf_extractor.controller;

import com.matteominin.pdf_extractor.model.pdf.Feature;
import com.matteominin.pdf_extractor.model.coverage.Coverage;
import com.matteominin.pdf_extractor.model.coverage.CoverageReport;
import com.matteominin.pdf_extractor.model.coverage.CoveredFeature;
import com.matteominin.pdf_extractor.model.feature.SummaryFeature;
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
     * Get covered features by report Id
     * GET /api/coverage-reports/{id}/covered-features
     */
    @GetMapping("/{id}/covered-features")
    public ResponseEntity<Map<String, Object>> getCoveredFeaturesByReportId(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            CoverageReport report = coverageReportService.getCoverageReportById(id);
            if (report == null) {
                response.put("success", false);
                response.put("error", "Coverage report not found with ID: " + id);
                return ResponseEntity.notFound().build();
            }

            if (report.getCoverage() == null) {
                response.put("success", false);
                response.put("error", "No coverage data found for report ID: " + id);
                return ResponseEntity.badRequest().body(response);
            }

            List<Map<String, Object>> enrichedCoveredFeatures = new ArrayList<>();
            if (report.getCoverage().getCoveredFeatures() != null) {
                for (CoveredFeature coveredFeature : report.getCoverage().getCoveredFeatures()) {
                    Map<String, Object> enrichedFeature = new HashMap<>();

                    // Include the matched feature and similarity
                    enrichedFeature.put("matchedFeature", coveredFeature.getMatchedFeature());
                    enrichedFeature.put("similarity", coveredFeature.getSimilarity());

                    // Include reference feature (already has ID inside)
                    if (coveredFeature.getReferenceFeatureId() != null) {
                        Map<String, Object> referenceFeature = new HashMap<>();
                        String featureId = coveredFeature.getReferenceFeatureId();
                        SummaryFeature refFeature = summaryFeatureService.getSummaryFeatureById(featureId);
                        referenceFeature.put("referenceFeatureId", featureId);
                        referenceFeature.put("feature", refFeature.getFeature());
                        referenceFeature.put("description", refFeature.getDescription());
                        referenceFeature.put("checklist", refFeature.getChecklist());
                        enrichedFeature.put("referenceFeature", referenceFeature);
                    }

                    enrichedCoveredFeatures.add(enrichedFeature);
                }
            }

            response.put("success", true);
            response.put("reportId", id);
            response.put("coveredFeatures", enrichedCoveredFeatures);
            response.put("count", enrichedCoveredFeatures.size());

            logger.info("Retrieved {} covered features for report ID: {}", enrichedCoveredFeatures.size(), id);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("Error retrieving covered features for report ID {}: {}", id, e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Failed to retrieve covered features: " + e.getMessage());
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
