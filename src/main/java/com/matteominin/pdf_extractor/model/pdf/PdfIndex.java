package com.matteominin.pdf_extractor.model.pdf;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PdfIndex {
    private List<Section> sections;

    @Getter
    @Setter
    public static class Section {
        private String section;
        private int start;
        private int end;
    }
}
