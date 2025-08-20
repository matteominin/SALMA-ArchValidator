package com.matteominin.pdf_extractor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceFeature {
    private String feature;
    private String description;
    private String sectionText;
}
