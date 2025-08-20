package com.matteominin.pdf_extractor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoveredFeature {
    private String referenceFeatureId;
    private MatchedFeature matchedFeature;
    private double similarity;
}
