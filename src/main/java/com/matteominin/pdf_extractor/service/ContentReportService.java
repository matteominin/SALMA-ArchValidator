package com.matteominin.pdf_extractor.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.matteominin.pdf_extractor.model.content.Architecture;
import com.matteominin.pdf_extractor.model.content.ContentReport;
import com.matteominin.pdf_extractor.model.content.Requirement;
import com.matteominin.pdf_extractor.model.content.UseCase;

@Service
public class ContentReportService {

    public ContentReport consolidateReport(List<UseCase> useCases, List<Requirement> requirements, List<Architecture> architectures) {
        ContentReport report = ContentReport.builder()
                .useCases(useCases)
                .requirements(requirements)
                .architectures(architectures)
                .build();
        return report;
    }
}
