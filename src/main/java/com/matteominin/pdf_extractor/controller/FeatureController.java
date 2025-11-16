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
    public ResponseEntity<Map<String, Object>> embedBatch(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();

        try {
            logger.info("Received embed-batch request with body keys: {}", body.keySet());

            // Safely convert the nested features list
            Object featuresObj = body.get("features");
            List<List<Feature>> features = new ArrayList<>();

            if (featuresObj == null) {
                logger.warn("Features object is null in embed-batch request");
                response.put("error", "Features list cannot be null");
                return ResponseEntity.badRequest().body(response);
            }

            logger.debug("Features object type: {}", featuresObj.getClass().getName());

            if (featuresObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> outerList = (List<Object>) featuresObj;
                logger.info("Processing features batch with {} groups", outerList.size());

                for (int i = 0; i < outerList.size(); i++) {
                    Object innerObj = outerList.get(i);
                    if (innerObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> innerList = (List<Object>) innerObj;
                        logger.debug("Group {} contains {} elements", i, innerList.size());

                        // Check if we have triple nesting [[[Feature]]] or double nesting [[Feature]]
                        if (!innerList.isEmpty() && innerList.get(0) instanceof List) {
                            // Triple nesting - flatten one level
                            logger.debug("Detected triple nesting at group {}, flattening...", i);
                            for (int j = 0; j < innerList.size(); j++) {
                                Object deepObj = innerList.get(j);
                                if (deepObj instanceof List) {
                                    @SuppressWarnings("unchecked")
                                    List<Object> deepList = (List<Object>) deepObj;
                                    List<Feature> featureGroup = new ArrayList<>();

                                    for (int k = 0; k < deepList.size(); k++) {
                                        Object featureObj = deepList.get(k);
                                        if (featureObj instanceof Map) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> featureMap = (Map<String, Object>) featureObj;
                                            try {
                                                Feature feature = objectMapper.convertValue(featureMap, Feature.class);
                                                featureGroup.add(feature);
                                            } catch (Exception e) {
                                                logger.error("Failed to convert feature at index [{}][{}][{}]: {}", i, j, k, e.getMessage());
                                                throw e;
                                            }
                                        } else {
                                            logger.warn("Feature element [{}][{}][{}] is not a Map, it's a {}", i, j, k,
                                                       featureObj != null ? featureObj.getClass().getName() : "null");
                                        }
                                    }
                                    if (!featureGroup.isEmpty()) {
                                        features.add(featureGroup);
                                        logger.debug("Added group from [{}][{}] with {} features", i, j, featureGroup.size());
                                    }
                                }
                            }
                        } else {
                            // Double nesting (normal case)
                            List<Feature> featureGroup = new ArrayList<>();
                            for (int j = 0; j < innerList.size(); j++) {
                                Object featureObj = innerList.get(j);
                                if (featureObj instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> featureMap = (Map<String, Object>) featureObj;
                                    try {
                                        Feature feature = objectMapper.convertValue(featureMap, Feature.class);
                                        featureGroup.add(feature);
                                    } catch (Exception e) {
                                        logger.error("Failed to convert feature at index [{}][{}]: {}", i, j, e.getMessage());
                                        throw e;
                                    }
                                } else {
                                    logger.warn("Feature element [{}][{}] is not a Map, it's a {}", i, j,
                                               featureObj != null ? featureObj.getClass().getName() : "null");
                                }
                            }
                            if (!featureGroup.isEmpty()) {
                                features.add(featureGroup);
                                logger.debug("Added group {} with {} features", i, featureGroup.size());
                            }
                        }
                    } else {
                        logger.warn("Batch element {} is not a List, it's a {}", i,
                                   innerObj != null ? innerObj.getClass().getName() : "null");
                    }
                }
            } else {
                logger.error("Features object is not a List, it's a {}", featuresObj.getClass().getName());
            }

            if (features.isEmpty()) {
                logger.warn("No feature groups were extracted from the request");
                response.put("error", "Features list cannot be null or empty");
                return ResponseEntity.badRequest().body(response);
            }

            logger.info("Successfully parsed {} feature groups with total {} features",
                       features.size(), features.stream().mapToInt(List::size).sum());

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
            logger.info("Received validate-features request with body keys: {}", body.keySet());

            // Log a sample of the structure for debugging
            try {
                String sample = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(body).substring(0, Math.min(500, objectMapper.writeValueAsString(body).length()));
                logger.debug("Request body sample (first 500 chars): {}", sample);
            } catch (Exception e) {
                logger.debug("Could not serialize body sample: {}", e.getMessage());
            }

            Double threshold = body.get("threshold") != null ?
                              ((Number) body.get("threshold")).doubleValue() : 0.85;

            logger.debug("Threshold set to: {}", threshold);

            // Safely convert the features list
            Object featuresObj = body.get("features");
            List<Feature> providedFeatures = new ArrayList<>();

            if (featuresObj == null) {
                logger.warn("Features object is null in request body");
                Map<String, String> error = new HashMap<>();
                error.put("error", "Features list cannot be null");
                return ResponseEntity.badRequest().body(error);
            }

            logger.debug("Features object type: {}", featuresObj.getClass().getName());

            if (featuresObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> featuresList = (List<Object>) featuresObj;
                logger.info("Processing features list with {} top-level elements", featuresList.size());

                int flatCount = 0;
                int nestedCount = 0;

                for (int i = 0; i < featuresList.size(); i++) {
                    Object featureObj = featuresList.get(i);
                    // Handle both flat arrays and nested arrays (Feature[][] from workflow)
                    if (featureObj instanceof List) {
                        // This is a nested array - flatten it
                        nestedCount++;
                        @SuppressWarnings("unchecked")
                        List<Object> nestedList = (List<Object>) featureObj;
                        logger.debug("Element {} is a nested list with {} items", i, nestedList.size());

                        for (int j = 0; j < nestedList.size(); j++) {
                            Object nestedFeatureObj = nestedList.get(j);
                            if (nestedFeatureObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> featureMap = (Map<String, Object>) nestedFeatureObj;
                                try {
                                    Feature feature = objectMapper.convertValue(featureMap, Feature.class);
                                    providedFeatures.add(feature);
                                } catch (Exception e) {
                                    logger.error("Failed to convert nested feature at index [{}][{}]: {}", i, j, e.getMessage());
                                    throw e;
                                }
                            } else {
                                logger.warn("Nested element [{}][{}] is not a Map, it's a {}", i, j,
                                           nestedFeatureObj != null ? nestedFeatureObj.getClass().getName() : "null");
                            }
                        }
                    } else if (featureObj instanceof Map) {
                        flatCount++;
                        @SuppressWarnings("unchecked")
                        Map<String, Object> featureMap = (Map<String, Object>) featureObj;

                        try {
                            // Convert the map to a Feature object
                            Feature feature = objectMapper.convertValue(featureMap, Feature.class);
                            providedFeatures.add(feature);
                        } catch (Exception e) {
                            logger.error("Failed to convert flat feature at index {}: {}", i, e.getMessage());
                            throw e;
                        }
                    } else {
                        logger.warn("Element {} is neither List nor Map, it's a {}", i,
                                   featureObj != null ? featureObj.getClass().getName() : "null");
                    }
                }

                logger.info("Processed {} flat features and {} nested arrays, total features extracted: {}",
                           flatCount, nestedCount, providedFeatures.size());
            } else {
                logger.error("Features object is not a List, it's a {}", featuresObj.getClass().getName());
            }

            if (providedFeatures.isEmpty()) {
                logger.warn("No features were extracted from the request");
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

            // Transform coveredFeatures to workflow-compatible format
            List<Map<String, Object>> workflowCompatibleFeatures = new ArrayList<>();
            if (coverageResult.getCoveredFeatures() != null) {
                for (CoveredFeature coveredFeature : coverageResult.getCoveredFeatures()) {
                    Map<String, Object> featureMap = new HashMap<>();

                    // Get the matched feature details
                    if (coveredFeature.getMatchedFeature() != null) {
                        featureMap.put("feature", coveredFeature.getMatchedFeature().getFeature());
                        featureMap.put("description", coveredFeature.getMatchedFeature().getDescription());
                        featureMap.put("section_text", coveredFeature.getMatchedFeature().getSectionText());
                    }

                    // Get the reference feature (summary feature) for matchedWith
                    String matchedWith = "";
                    if (coveredFeature.getReferenceFeatureId() != null) {
                        // Find the summary feature by ID to get its checklist
                        var refFeature = summaryFeatures.stream()
                            .filter(sf -> coveredFeature.getReferenceFeatureId().equals(sf.getId()))
                            .findFirst();

                        if (refFeature.isPresent() && refFeature.get().getChecklist() != null) {
                            // Join checklist items with comma
                            matchedWith = String.join(", ", refFeature.get().getChecklist());
                        }
                    }
                    featureMap.put("matchedWith", matchedWith);

                    workflowCompatibleFeatures.add(featureMap);
                }
            }

            // Add coveredFeatures at root level for workflow compatibility
            response.put("coveredFeatures", workflowCompatibleFeatures);

            logger.info("Summary coverage analysis completed and report saved");
            logger.info("Returning {} covered features for downstream processing", workflowCompatibleFeatures.size());
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
