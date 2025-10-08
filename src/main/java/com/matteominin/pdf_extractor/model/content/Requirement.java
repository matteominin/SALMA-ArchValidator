package com.matteominin.pdf_extractor.model.content;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Requirement {
    @JsonProperty("req_id")
    private String reqId;
    private String description;
    private String type;    // type of requirement (functional, non-functional)

    @JsonProperty("source_text")
    private String sourceText;

    @JsonProperty("quality_notes")
    private String qualityNotes;

    @Override
    public String toString() {
        return "Description: " + description + "\nSource Text: " + sourceText;
    }
}
