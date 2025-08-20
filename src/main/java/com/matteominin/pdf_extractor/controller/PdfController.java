package com.matteominin.pdf_extractor.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matteominin.pdf_extractor.model.ExtractedSection;
import com.matteominin.pdf_extractor.model.PdfIndex;
import com.matteominin.pdf_extractor.service.PdfService;

@RestController
@RequestMapping("/api/pdf")
@Slf4j
public class PdfController {

    private final PdfService pdfService;

    @Autowired
    public PdfController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @PostMapping("/extract")
    public ResponseEntity<?> extractTextFromPdf(@RequestBody Map<String, String> request) {
        try {
            String filePath = request.get("filepath");
            log.info("Processing PDF extraction request for: {}", filePath);

            String extractedText = pdfService.extractText(filePath);

            Map<String, String> response = new HashMap<>();
            response.put("text", extractedText);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request parameters: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Error processing PDF extraction", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error processing PDF: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/extract-index")
    public ResponseEntity<?> extractIndexPages(@RequestBody Map<String, String> request) {
        try {
            String filePath = request.get("filePath");
            int startPage = Integer.parseInt(request.get("startPage"));
            int endPage = Integer.parseInt(request.get("endPage"));
            log.info("Processing PDF index extraction request for: {} from page {} to {}", filePath, startPage, endPage);

            String extractedText = pdfService.extractPages(filePath, startPage, endPage);

            Map<String, String> response = new HashMap<>();
            response.put("text", extractedText);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing PDF index extraction", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error processing PDF: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/extract-sections")
    public ResponseEntity<?> extractSectionsFromPdf(@RequestBody Map<String, Object> request) {
        try {
            String filePath = (String) request.get("filePath");
            Object indexObj = request.get("index");
            
            log.info("Processing PDF section extraction request for: {}", filePath);
            
            ObjectMapper mapper = new ObjectMapper();
            List<PdfIndex.Section> sections;
            
            if (indexObj instanceof List) {
                sections = ((List<?>) indexObj).stream()
                        .map(item -> mapper.convertValue(item, PdfIndex.Section.class))
                        .toList();
            } else {
                throw new IllegalArgumentException("Index must be an array of sections");
            }
            
            PdfIndex index = new PdfIndex();
            index.setSections(sections);

            List<ExtractedSection> extractedSections = pdfService.extractSections(filePath, index);
            return ResponseEntity.ok(extractedSections);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request parameters: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Error processing PDF section extraction", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error processing PDF: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
