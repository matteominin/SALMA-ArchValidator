package com.matteominin.pdf_extractor.model.content;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Component {
    private String name;
    private String responsibility;

    @JsonProperty("design_notes")
    private String designNotes;

    @JsonProperty("communicates_with")
    private List<String> communicatesWith;
}
