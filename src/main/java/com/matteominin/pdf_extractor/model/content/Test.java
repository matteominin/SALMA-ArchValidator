package com.matteominin.pdf_extractor.model.content;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Test {
    @JsonProperty("test_id")
    private String testId;

    @JsonProperty("test_type")
    private String testType;

    @JsonProperty("tested_artifact_name")
    private String testedArtifactName;

    @JsonProperty("coverage_hint")
    private String coverageHint;

    @JsonProperty("description_summary")
    private String descriptionSummary;
}
