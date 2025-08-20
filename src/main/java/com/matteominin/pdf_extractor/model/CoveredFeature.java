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
    private ReferenceFeature referenceFeature;
    private MatchedFeature matchedFeature;
    private double similarity;
}
