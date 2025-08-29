package com.matteominin.pdf_extractor.model.feature;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class SummaryFeature {
    private String id;
    private String feature;
    private String description;
    private List<Double> embedding;
    private String count;
    private List<String> checklist;
    private String example;
}
