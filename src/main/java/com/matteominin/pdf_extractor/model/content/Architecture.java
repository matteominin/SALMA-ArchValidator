package com.matteominin.pdf_extractor.model.content;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Architecture {
    private String pattern;
    private List<Component> components;

    @JsonProperty("analysis_summary")
    private String analysisSummary;
}
