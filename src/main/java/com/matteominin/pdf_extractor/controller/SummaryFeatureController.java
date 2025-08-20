package com.matteominin.pdf_extractor.controller;

import com.matteominin.pdf_extractor.model.SummaryFeature;
import com.matteominin.pdf_extractor.service.SummaryFeatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/summary-features")
public class SummaryFeatureController {
    
    private static final Logger logger = LoggerFactory.getLogger(SummaryFeatureController.class);
    
    @Autowired
    private SummaryFeatureService summaryFeatureService;
    
    /**
     * Add a batch of summary features.
     * Expected JSON: [ { "feature": "...", "description": "...", "count": "...", "example": "..." }, ... ]
     */
    @PostMapping("/add-batch")
    public ResponseEntity<Map<String, Object>> addBatch(@RequestBody Map<String, List<SummaryFeature>> body) {
        Map<String, Object> response = new HashMap<>();
        List<SummaryFeature> summaryFeatures = body.get("summarized_features");

        try {
            List<String> ids = summaryFeatureService.saveSummaryFeatures(summaryFeatures);
            
            response.put("success", true);
            response.put("processed", summaryFeatures.size());
            response.put("ids", ids);
            response.put("message", String.format("Successfully saved %d summary features", summaryFeatures.size()));
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid summary features data: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("Error saving summary features: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Failed to save summary features: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get all summary features.
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllSummaryFeatures() {
        try {
            List<SummaryFeature> summaryFeatures = summaryFeatureService.getAllSummaryFeatures();
            return ResponseEntity.ok(summaryFeatures);
            
        } catch (Exception e) {
            logger.error("Error retrieving summary features: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to retrieve summary features: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
