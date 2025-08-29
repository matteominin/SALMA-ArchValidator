package com.matteominin.pdf_extractor.service;

import com.matteominin.pdf_extractor.model.feature.SummaryFeature;
import com.matteominin.pdf_extractor.repository.SummaryFeatureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SummaryFeatureService {
    
    private static final Logger logger = LoggerFactory.getLogger(SummaryFeatureService.class);
    
    @Autowired
    private SummaryFeatureRepository summaryFeatureRepository;

   @Autowired
   private EmbeddingService embeddingService; 

    public List<SummaryFeature> embedSummaryFeatures(List<SummaryFeature> summaryFeatures) {
        for(SummaryFeature feature : summaryFeatures) {
            if(feature.getFeature() == null && feature.getFeature().trim().isEmpty()) {
                throw new IllegalArgumentException("Summary feature text cannot be null or empty");
            }
            if(feature.getDescription() == null && feature.getDescription().trim().isEmpty()) {
                throw new IllegalArgumentException("Summary feature description cannot be null or empty");
            }
            String text = feature.getFeature() + " " + feature.getDescription();
            List<Double> embedding = embeddingService.generateEmbedding(text);
            feature.setEmbedding(embedding);
        }

        return summaryFeatures;
    }
    
    /**
     * Save a list of summary features to the database.
     * 
     * @param summaryFeatures List of summary features to save
     * @return List of IDs of saved summary features
     */
    public List<String> saveSummaryFeatures(List<SummaryFeature> summaryFeatures) {
        if (summaryFeatures == null || summaryFeatures.isEmpty()) {
            throw new IllegalArgumentException("Summary features list cannot be null or empty");
        }
        
        // Validate each summary feature
        for (SummaryFeature summaryFeature : summaryFeatures) {
            if (summaryFeature.getFeature() == null || summaryFeature.getFeature().trim().isEmpty()) {
                throw new IllegalArgumentException("Summary feature text cannot be null or empty");
            }
        }
        
        try {
            summaryFeatures = embedSummaryFeatures(summaryFeatures);
            List<String> ids = summaryFeatureRepository.saveAll(summaryFeatures);
            logger.info("Successfully saved {} summary features", summaryFeatures.size());
            return ids;
        } catch (Exception e) {
            logger.error("Failed to save summary features: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save summary features", e);
        }
    }
    
    /**
     * Get all summary features from the database.
     * 
     * @return List of all summary features
     */
    public List<SummaryFeature> getAllSummaryFeatures() {
        try {
            List<SummaryFeature> summaryFeatures = summaryFeatureRepository.findAll();
            logger.debug("Retrieved {} summary features", summaryFeatures.size());
            return summaryFeatures;
        } catch (Exception e) {
            logger.error("Failed to retrieve summary features: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve summary features", e);
        }
    }

    /**
     * Get summary feature by ID.
     * 
     * @param id The ID of the summary feature to retrieve
     * @return The summary feature or null if not found
     */
    public SummaryFeature getSummaryFeatureById(String id) {
        try {
            SummaryFeature summaryFeature = summaryFeatureRepository.findById(id);
            logger.debug("Retrieved summary feature with ID: {}", id);
            return summaryFeature;
        } catch (Exception e) {
            logger.error("Failed to retrieve summary feature by ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve summary feature", e);
        }
    }
}
