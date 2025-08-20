package com.matteominin.pdf_extractor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UncoveredFeature {
    private ReferenceFeature referenceFeature;
    private MatchedFeature matchedFeature; // Will be null for uncovered
    private double similarity; // Will be 0.0 for uncovered
}
