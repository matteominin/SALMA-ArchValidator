package com.matteominin.pdf_extractor.model.coverage;

import java.util.List;
import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coverage {
    private Double coveragePercentage;
    private int coveredCount;
    private int uncoveredCount;
    
    @Builder.Default
    private List<CoveredFeature> coveredFeatures = new ArrayList<>();
    
    @Builder.Default
    private List<UncoveredFeature> uncoveredFeatures = new ArrayList<>();

    public void addCoveredFeature(CoveredFeature feature) {
        coveredFeatures.add(feature);
        coveredCount = coveredFeatures.size();
    }

    public void addUncoveredFeature(UncoveredFeature feature) {
        uncoveredFeatures.add(feature);
        uncoveredCount = uncoveredFeatures.size();
    }
    
    public void calculateCoveragePercentage() {
        int total = coveredCount + uncoveredCount;
        if (total > 0) {
            coveragePercentage = (double) coveredCount / total * 100.0;
            coveragePercentage = Math.round(coveragePercentage * 100.0) / 100.0;
        } else {
            coveragePercentage = 0.0;
        }
    }
}
