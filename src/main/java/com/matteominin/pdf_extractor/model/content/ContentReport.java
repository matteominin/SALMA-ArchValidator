package com.matteominin.pdf_extractor.model.content;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentReport {
    @JsonProperty("use_case")
    List<UseCase> useCases;
    
    List<Requirement> requirements;
    
    @JsonProperty("architecture")
    List<Architecture> architectures;
}
