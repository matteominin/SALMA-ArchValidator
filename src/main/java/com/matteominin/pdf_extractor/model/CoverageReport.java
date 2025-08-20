package com.matteominin.pdf_extractor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageReport {
    private String id;
    private Coverage coverage;
    private int providedFeatures;
    private boolean success;
    private double threshold;
    private int totalSummaryFeatures;
    private Date createdAt;
    private Date updatedAt;
    private String reportName;
    private String description;
}
