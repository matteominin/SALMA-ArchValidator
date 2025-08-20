package com.matteominin.pdf_extractor.service;

import com.matteominin.pdf_extractor.model.Feature;
import com.matteominin.pdf_extractor.repository.FeatureRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Service
public class FeatureService {
    
    private static final Logger logger = LoggerFactory.getLogger(FeatureService.class);
    
    @Autowired
    private FeatureRepository featureRepository;
    
    @Autowired
    private EmbeddingService embeddingService;
    
    public Feature addFeature(Feature feature) {
        validateFeature(feature);
        
        // Generate embedding if not present
        if (feature.getEmbedding() == null || feature.getEmbedding().isEmpty()) {
            String text = buildEmbeddingText(feature);
            List<Double> embedding = embeddingService.generateEmbedding(text);
            feature.setEmbedding(embedding);
        }
        
        // Save to repository
        String id = featureRepository.saveFeature(feature);
        feature.setId(id);
        
        logger.info("Feature '{}' added successfully with ID: {}", feature.getFeature(), id);
        return feature;
    }
    
    public List<Feature> addBatchFeatures(List<List<Feature>> features) {
        List<Feature> allFeatures = new ArrayList<>();

        for (List<Feature> batch : features) {
            for (Feature f: batch) {
                if (!f.getFeature().trim().isEmpty()) {
                    allFeatures.add(f);
                }
            }
        }

        for (Feature feature : allFeatures) {
            if (feature.getEmbedding() == null || feature.getEmbedding().isEmpty()) {
                String text = buildEmbeddingText(feature);
                List<Double> embedding = embeddingService.generateEmbedding(text);
                feature.setEmbedding(embedding);
            }
        }

        // save to db
        featureRepository.saveFeatureList(allFeatures);
        
        logger.info("Batch embedding completed for {} features", features.size());
        return allFeatures;
    }

    public List<Feature> getAllFeatures() {
        return featureRepository.findAll();
    }
    
    private void validateFeature(Feature feature) {
        if (feature == null) {
            throw new IllegalArgumentException("Feature cannot be null");
        }
        if (feature.getFeature() == null || feature.getFeature().trim().isEmpty()) {
            throw new IllegalArgumentException("Feature name is required");
        }
    }
    
    private String buildEmbeddingText(Feature feature) {
        StringBuilder text = new StringBuilder();
        text.append(feature.getFeature());
        
        if (feature.getDescription() != null && !feature.getDescription().trim().isEmpty()) {
            text.append(". ").append(feature.getDescription());
        }
        
        if (feature.getCategory() != null && !feature.getCategory().trim().isEmpty()) {
            text.append(". Category: ").append(feature.getCategory());
        }
        
        return text.toString();
    }
}
