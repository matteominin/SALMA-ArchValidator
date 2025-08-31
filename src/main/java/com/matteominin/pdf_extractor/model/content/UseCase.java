package com.matteominin.pdf_extractor.model.content;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UseCase {
    @JsonProperty("case_id")
    private String caseId;

    private String name;

    private List<String> actors;

    @JsonProperty("main_flow")
    private List<String> mainFlow;

    @JsonProperty("alternative_flows")  
    private List<String> alternativeFlows;

    @JsonProperty("is_explicit")
    private boolean isExplicit;

    @Override
    public String toString() {
        return name + "\n" + actors + "\n" + mainFlow.toString();
    }
}
