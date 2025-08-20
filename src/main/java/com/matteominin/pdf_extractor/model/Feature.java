package com.matteominin.pdf_extractor.model;

import java.util.Date;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feature {
    private String id;
    private String feature;
    private String description;
    private String category;
    private String evidence;
    private double confidence;
    private String source_title;
    private String section_text;
    private String filePath;
    private List<Double> embedding;
    private Date createdAt;
    private Date updatedAt;
}
