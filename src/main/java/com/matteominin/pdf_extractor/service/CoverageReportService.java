package com.matteominin.pdf_extractor.service;

import com.matteominin.pdf_extractor.model.*;
import com.matteominin.pdf_extractor.repository.CoverageReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CoverageReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(CoverageReportService.class);
    
    @Autowired
    private CoverageReportRepository coverageReportRepository;
    
    /**
     * Save a coverage report to the database
     */
    public CoverageReport saveCoverageReport(CoverageReport coverageReport) {
        try {
            validateCoverageReport(coverageReport);
            
            String reportId = coverageReportRepository.save(coverageReport);
            coverageReport.setId(reportId);
            
            logger.info("Coverage report saved successfully with ID: {}", reportId);
            return coverageReport;
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid coverage report data: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error saving coverage report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save coverage report", e);
        }
    }

    /**
     * Create a coverage report from validation results
     */
    public CoverageReport createCoverageReport(Coverage coverage, 
                                             int providedFeatures, 
                                             double threshold, 
                                             int totalSummaryFeatures,
                                             String reportName,
                                             String description) {
        
        return CoverageReport.builder()
                .coverage(coverage)
                .providedFeatures(providedFeatures)
                .success(true)
                .threshold(threshold)
                .totalSummaryFeatures(totalSummaryFeatures)
                .reportName(reportName)
                .description(description)
                .build();
    }

    /**
     * Get coverage report by ID
     */
    public CoverageReport getCoverageReportById(String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Coverage report ID cannot be null or empty");
            }
            
            CoverageReport report = coverageReportRepository.findById(id);
            if (report == null) {
                throw new RuntimeException("Coverage report not found with ID: " + id);
            }
            
            return report;
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid coverage report ID: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error retrieving coverage report by ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve coverage report", e);
        }
    }
    
    /**
     * Convert the current analyzeCoverage result format to the new Coverage model
     */
    public Coverage convertToCoverageModel(List<SummaryFeature> summaryFeatures,
                                                 List<Feature> providedFeatures,
                                         double threshold) {
        
        Coverage coverage = Coverage.builder().build();
        
        for (var summaryFeature : summaryFeatures) {
            boolean isCovered = false;
            Feature bestMatch = null;
            double bestSimilarity = 0.0;
            
            // Check if this summary feature is covered by any provided feature
            for (Feature providedFeature : providedFeatures) {
                if (providedFeature.getEmbedding() != null && !providedFeature.getEmbedding().isEmpty() &&
                    summaryFeature.getEmbedding() != null && !summaryFeature.getEmbedding().isEmpty()) {
                    
                    double similarity = EmbeddingService.calculateCosineSimilarity(
                        summaryFeature.getEmbedding(), 
                        providedFeature.getEmbedding()
                    );
                    
                    if (similarity >= threshold && similarity > bestSimilarity) {
                        isCovered = true;
                        bestMatch = providedFeature;
                        bestSimilarity = similarity;
                    }
                }
            }

            if (isCovered) {
                // Create matched feature
                MatchedFeature matchedFeature = MatchedFeature.builder()
                        .feature(bestMatch.getFeature())
                        .description(bestMatch.getDescription())
                        .sectionText(bestMatch.getSection_text())
                        .build();
                
                CoveredFeature coveredFeature = CoveredFeature.builder()
                        .referenceFeatureId(summaryFeature.getId())
                        .matchedFeature(matchedFeature)
                        .similarity(bestSimilarity)
                        .build();
                
                coverage.addCoveredFeature(coveredFeature);
            } else {
                UncoveredFeature uncoveredFeature = UncoveredFeature.builder()
                        .referenceFeatureId(summaryFeature.getId())
                        .similarity(0.0)
                        .build();
                
                coverage.addUncoveredFeature(uncoveredFeature);
            }
        }
        
        coverage.calculateCoveragePercentage();
        return coverage;
    }
    
    private void validateCoverageReport(CoverageReport report) {
        if (report == null) {
            throw new IllegalArgumentException("Coverage report cannot be null");
        }
        
        if (report.getCoverage() == null) {
            throw new IllegalArgumentException("Coverage data cannot be null");
        }
        
        if (report.getReportName() == null || report.getReportName().trim().isEmpty()) {
            throw new IllegalArgumentException("Report name cannot be null or empty");
        }
    }
}
