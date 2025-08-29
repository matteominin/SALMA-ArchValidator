package com.matteominin.pdf_extractor.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UncoveredFeature {
    private String referenceFeatureId;
    private double similarity;
}
