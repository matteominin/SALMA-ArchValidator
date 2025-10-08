package com.matteominin.pdf_extractor.controller;

import com.matteominin.pdf_extractor.model.pdf.Feature;
import com.matteominin.pdf_extractor.model.coverage.Coverage;
import com.matteominin.pdf_extractor.model.coverage.CoverageReport;
import com.matteominin.pdf_extractor.model.coverage.CoveredFeature;
import com.matteominin.pdf_extractor.model.coverage.UncoveredFeature;
import com.matteominin.pdf_extractor.model.feature.MatchedFeature;
import com.matteominin.pdf_extractor.model.feature.SummaryFeature;
import com.matteominin.pdf_extractor.service.FeatureService;
import com.matteominin.pdf_extractor.service.FeatureClusteringService;
import com.matteominin.pdf_extractor.service.SummaryFeatureService;
import com.matteominin.pdf_extractor.service.ClusteringService;
import com.matteominin.pdf_extractor.service.CoverageReportService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/features")
public class FeatureController {
    
    private static final Logger logger = LoggerFactory.getLogger(FeatureController.class);
    
    @Autowired
    private FeatureService featureService;
    
    @Autowired
    private FeatureClusteringService clusteringService;
    
    @Autowired
    private SummaryFeatureService summaryFeatureService;
    
    @Autowired
    private CoverageReportService coverageReportService;
    
    @Value("${spring.ai.openai.api-key:#{null}}")
    private String apiKey;
    
    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addFeature(@RequestBody Feature feature) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Feature savedFeature = featureService.addFeature(feature);
            
            response.put("success", true);
            response.put("id", savedFeature.getId());
            response.put("feature", savedFeature);
            response.put("message", "Feature added successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid feature data: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("Error adding feature: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Failed to add feature: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/add-batch")
    public ResponseEntity<Map<String, Object>> addBatch(@RequestBody Map<String, List<List<Feature>>> body) {
        List<List<Feature>> features = body.get("features");
        Map<String, Object> response = new HashMap<>();

        try {
            List<Feature> embeddedFeatures = featureService.addBatchFeatures(features);
            response.put("success", true);
            response.put("processed", embeddedFeatures.size());
            response.put("message", String.format("Batch embedding completed for %d features", embeddedFeatures.size()));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid batch data: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error in batch embedding: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Batch embedding failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/embed-batch")
    public ResponseEntity<Map<String, Object>> embedBatch(@RequestBody Map<String, List<List<Feature>>> body) {
        List<List<Feature>> features = body.get("features");
        Map<String, Object> response = new HashMap<>();

        try {
            List<Feature> embeddedFeatures = featureService.addBatchFeatures(features);
            response.put("features", embeddedFeatures);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid batch data: {}", e.getMessage());
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("Error in batch embedding: {}", e.getMessage(), e);
            response.put("error", "Batch embedding failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Feature>> getAllFeatures() {
        ResponseEntity<List<Feature>> response;
        try {
            List<Feature> features = featureService.getAllFeatures();
            response = ResponseEntity.ok(features);
        } catch (Exception e) {
            logger.error("Error retrieving features: {}", e.getMessage(), e);
            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        return response;
    }

    /**
     * Clusters features based on cosine similarity of their embeddings.
     * Default threshold is 0.85 as requested.
     */
    @PostMapping("/cluster")
    public ResponseEntity<?> clusterFeatures(@RequestParam(defaultValue = "0.85") double threshold) {
        try {
            logger.info("Starting feature clustering with threshold: {}", threshold);
            
            if (threshold < 0.0 || threshold > 1.0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Threshold must be between 0.0 and 1.0");
                return ResponseEntity.badRequest().body(error);
            }

            List<List<Feature>> clusters = clusteringService.clusterFeatures(threshold);

            // Create response with clusters and statistics
            List<List<Feature>> response = new ArrayList<>();
            response.addAll(clusters);
            logger.info("Clustering completed. Found {} clusters", clusters.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error during feature clustering: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to cluster features: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Checks coverage of summary features against provided features using cosine similarity.
     * Returns which summary features are covered and which are missing.
     */
    @PostMapping("/validate-features")
    public ResponseEntity<?> validateFeatures(
            @RequestBody Map<String, Object> body) {
        
        try {
            Double threshold = body.get("threshold") != null ? 
                              ((Number) body.get("threshold")).doubleValue() : 0.85;
            
            // Safely convert the features list
            Object featuresObj = body.get("features");
            List<Feature> providedFeatures = new ArrayList<>();
            
            if (featuresObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> featuresList = (List<Object>) featuresObj;
                
                for (Object featureObj : featuresList) {
                    if (featureObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> featureMap = (Map<String, Object>) featureObj;
                        
                        // Convert the map to a Feature object
                        Feature feature = objectMapper.convertValue(featureMap, Feature.class);
                        providedFeatures.add(feature);
                    }
                }
            }
            
            if (providedFeatures.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Features list cannot be null or empty");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (threshold < 0.0 || threshold > 1.0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Threshold must be between 0.0 and 1.0");
                return ResponseEntity.badRequest().body(error);
            }
            
            logger.info("Checking summary feature coverage with threshold: {}", threshold);
            
            // Get all summary features from database
            var summaryFeatures = summaryFeatureService.getAllSummaryFeatures();
            
            // Perform coverage analysis
            Coverage coverageResult = analyzeCoverage(summaryFeatures, providedFeatures, threshold);
            
            // Create and save coverage report
            CoverageReport report = CoverageReport.builder()
                .coverage(coverageResult)
                .threshold(threshold)
                .totalSummaryFeatures(summaryFeatures.size())
                .providedFeatures(providedFeatures.size())
                .success(true)
                .reportName("feature_coverage")
                .description("Coverage analysis for feature validation")
                .build();
            
            // Save the report in validation_reports collection with feature_coverage document
            try {
                CoverageReport savedReport = coverageReportService.saveCoverageReport(report);
                logger.info("Coverage report saved with name: {}", savedReport.getReportName());
            } catch (Exception e) {
                logger.warn("Failed to save coverage report: {}", e.getMessage());
                // Continue execution even if saving fails
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("threshold", threshold);
            response.put("totalSummaryFeatures", summaryFeatures.size());
            response.put("providedFeatures", providedFeatures.size());
            response.put("coverage", coverageResult);
            response.put("reportSaved", true);
            
            logger.info("Summary coverage analysis completed and report saved");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during summary coverage check: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to check summary coverage: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    private Coverage analyzeCoverage(List<SummaryFeature> summaryFeatures,
                                                       List<Feature> providedFeatures, double threshold) {
        
        Coverage coverage = Coverage.builder().build();
        
        for (var summaryFeature : summaryFeatures) {
            Feature bestMatch = null;
            double bestSimilarity = 0.0;
            
            // Find the best matching feature
            for (Feature providedFeature : providedFeatures) {
                if (providedFeature.getEmbedding() != null && !providedFeature.getEmbedding().isEmpty() &&
                    summaryFeature.getEmbedding() != null && !summaryFeature.getEmbedding().isEmpty()) {
                    
                    double similarity = ClusteringService.calculateCosineSimilarity(
                        summaryFeature.getEmbedding(), 
                        providedFeature.getEmbedding()
                    );
                    
                    if (similarity > bestSimilarity) {
                        bestSimilarity = similarity;
                        bestMatch = providedFeature;
                    }
                } else {
                    logger.warn("Skipping similarity check for empty embedding in feature: {}", providedFeature.getFeature());
                }
            }

            if (bestMatch != null && bestSimilarity >= threshold) {
                // Create matched feature from best match
                MatchedFeature matchedFeature = MatchedFeature.builder()
                    .feature(bestMatch.getFeature())
                    .description(bestMatch.getDescription())
                    .sectionText(bestMatch.getSection_text())
                    .build();
                
                // Create covered feature
                CoveredFeature coveredFeature = CoveredFeature.builder()
                        .referenceFeatureId(summaryFeature.getId())
                    .matchedFeature(matchedFeature)
                    .similarity(bestSimilarity)
                    .build();
                
                coverage.addCoveredFeature(coveredFeature);
            } else {
                // Create uncovered feature  
                UncoveredFeature uncoveredFeature = UncoveredFeature.builder()
                        .referenceFeatureId(summaryFeature.getId())
                    .similarity(bestSimilarity)
                    .build();
                
                coverage.addUncoveredFeature(uncoveredFeature);
            }
        }
        
        coverage.calculateCoveragePercentage();
        return coverage;
    }
}
