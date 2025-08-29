package com.matteominin.pdf_extractor.model.coverage;

import com.matteominin.pdf_extractor.model.feature.MatchedFeature;
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
