package com.matteominin.pdf_extractor.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import lombok.extern.slf4j.Slf4j;

import com.matteominin.pdf_extractor.model.pdf.ExtractedSection;
import com.matteominin.pdf_extractor.model.pdf.PdfIndex;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PdfService {

    @Value("${app.api.base-url}")
    private String apiUrl;
    
    public String extractText(String filePath, String outputDirectory) {
        log.info("Starting text and image extraction for: {}", filePath);
        validateFilePath(filePath);

        StringBuilder result = new StringBuilder();
        PDDocument document = null;

        try {
            document = loadPDF(filePath);
            int totalPages = document.getNumberOfPages();
            log.info("Processing {} pages for extraction", totalPages);

            PDFTextStripper stripper = new PDFTextStripper();
            String extractedText = stripper.getText(document);

            result.append(extractedText);
        } catch (IOException e) {
            log.error("Error during text extraction", e);
            throw new RuntimeException("Error during text extraction", e);
        } finally {
            closeDocument(document);
        }

        String finalResult = result.toString();
        log.info("Completed extraction - total {} characters", finalResult.length());

        return finalResult;
    }

    public String extractText(String filePath) {
        return extractText(filePath, "extracted_images");
    }

    public String extractPages(String filePath, int startPage, int endPage) {
        log.info("Extracting pages {}-{} from PDF: {}", startPage, endPage, filePath);
        validateFilePath(filePath);
        validatePageRange(startPage, endPage);
        PDDocument document = null;
        try {
            document = loadPDF(filePath);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(startPage);
            if (endPage == -1) {
                stripper.setEndPage(document.getNumberOfPages());
            } else {
                stripper.setEndPage(Math.min(endPage, document.getNumberOfPages()));
            }
            String text = stripper.getText(document);
            log.debug("Successfully extracted text from pages {}-{}, length: {}", startPage, endPage, text.length());
            return text;
        } catch (IOException e) {
            log.error("Error extracting text from PDF pages {}-{}", startPage, endPage, e);
            throw new RuntimeException("Error extracting text from PDF pages", e);
        } finally {
            closeDocument(document);
        }
    }

    public List<ExtractedSection> extractSections(String filePath, PdfIndex index) {
        log.info("Extracting {} sections from PDF: {}", index.getSections().size(), filePath);

        validateExtractionRequest(filePath, index);

        List<ExtractedSection> sections = new ArrayList<>();
        PDDocument document = null;

        try {
            document = loadPDF(filePath);
            String pdfText = extractTextViaPythonApi(filePath, 1, document.getNumberOfPages(),  false);
            pdfText = removeIndexFromText(pdfText, index);
            // Extract all sections except the last one
            for (int i = 0; i < index.getSections().size() - 1; i++) {
                PdfIndex.Section currentSection = index.getSections().get(i);
                PdfIndex.Section nextSection = index.getSections().get(i + 1);

                String text = extractSingleSection(
                    document,
                    pdfText,
                    currentSection.getSection(),
                    nextSection.getSection(),
                    currentSection.getStart(),
                    nextSection.getStart(),
                    index
                );

                sections.add(ExtractedSection.builder()
                    .section(currentSection.getSection())
                    .text(text)
                    .build());
            }

            // Extract the last section
            PdfIndex.Section lastSection = index.getSections().get(index.getSections().size() - 1);
            String text = extractSingleSection(
                document,
                pdfText,
                lastSection.getSection(),
                null,
                lastSection.getStart(),
                lastSection.getEnd(),
                index
            );

            sections.add(ExtractedSection.builder()
                .section(lastSection.getSection())
                .text(text)
                .build());

            log.info("Successfully extracted {} sections from PDF", sections.size());
            return sections;

        } catch (IOException e) {
            log.error("Error extracting sections from PDF: {}", filePath, e);
            throw new RuntimeException("Error extracting sections from PDF: " + filePath, e);
        } finally {
            closeDocument(document);
        }
    }

    private PDDocument loadPDF(String filePath) throws IOException{
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("PDF file not found, filepath:" + filePath);
        }
        return Loader.loadPDF(file);
    }

    private void closeDocument(PDDocument document) {
        if (document != null) {
            try {
                document.close();
                log.debug("Successfully closed PDF document");
            } catch (IOException e) {
                log.warn("Error closing PDF document", e);
            }
        }
    }

    private String extractSingleSection(PDDocument document, String pdfText, String currentSectionTitle, String nextSectionTitle,
                                             int startPage, int endPage, PdfIndex index) {
        try {
            // Extract the specific section using regex
            String pattern = "(?s)" + Pattern.quote(currentSectionTitle) + "(.*?)" +
                           (nextSectionTitle != null ? Pattern.quote(nextSectionTitle) : "$");
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(pdfText);

            if (matcher.find()) {
                String sectionText = matcher.group(1).trim();
                log.debug("Successfully extracted section '{}', length: {}", currentSectionTitle, sectionText.length());
                return sectionText;
            } else {
                log.warn("No content found for section: {}", currentSectionTitle);
                return "";
            }
        } catch (Exception e) {
            log.error("Error extracting section '{}' from PDF", currentSectionTitle, e);
            throw new RuntimeException("Error extracting section from PDF", e);
        }
    }

    public String extractTextViaPythonApi(String filePath, int startPage, int endPage, boolean usePlaceholder) {
        try {
            final String url = apiUrl + "/extract";
            File file = new File(filePath);

            if (!file.exists()) {
                throw new IllegalArgumentException("File not found: " + filePath);
            }

            MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
            parts.add("file", new FileSystemResource(file));
            parts.add("mode", "pages");
            parts.add("start_page", String.valueOf(startPage));
            parts.add("end_page", String.valueOf(endPage));
            parts.add("use_placeholder", String.valueOf(usePlaceholder));

            RestClient restClient = RestClient.create();

            @SuppressWarnings("unchecked")
            Map<String, Object> pythonResponse = restClient.post()
                    .uri(url)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(Map.class);

            if (pythonResponse == null) {
                throw new RuntimeException("No response from PDF extraction service");
            }

            String text = pythonResponse.get("text").toString();
            log.debug("Extracted {} characters from pages {}-{} via Python API", text.length(), startPage, endPage);
            return text;

        } catch (Exception e) {
            log.error("Error calling Python API for text extraction", e);
            throw new RuntimeException("Error calling Python API for text extraction", e);
        }
    }

    private String removeIndexFromText(String fullText, PdfIndex index) {
        String firstContentTitle = index.getSections().get(0).getSection();

        // Build regex to match everything between the first occurrence and the next occurrence of the first section title
        String pattern = "(?s)" + Pattern.quote(firstContentTitle) + "(.*?)" + Pattern.quote(firstContentTitle);
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(fullText);

        if (matcher.find()) {
            StringBuilder sb = new StringBuilder();
            sb.append(fullText.substring(0, matcher.start()));
            sb.append(firstContentTitle);
            sb.append(fullText.substring(matcher.end()));
            String cleanedText = sb.toString();
            log.debug("Successfully removed index from text");
            return cleanedText;
        } else {
            log.debug("No index pattern found, returning original text");
            return fullText;
        }
    }

    private void validateFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
    }

    private void validatePageRange(int startPage, int endPage) {
        if (startPage < 1) {
            throw new IllegalArgumentException("Start page must be greater than 0");
        }
        
        if (endPage != -1 && endPage < startPage) {
            throw new IllegalArgumentException("End page must be greater than or equal to start page");
        }
    }

    private void validateExtractionRequest(String filePath, PdfIndex index) {
        validateFilePath(filePath);
        
        if (index == null || index.getSections() == null || index.getSections().isEmpty()) {
            throw new IllegalArgumentException("Index must contain at least one section");
        }
        
        // Validate section data
        for (PdfIndex.Section section : index.getSections()) {
            if (section.getSection() == null || section.getSection().trim().isEmpty()) {
                throw new IllegalArgumentException("Section name cannot be null or empty");
            }
            
            if (section.getStart() < 1) {
                throw new IllegalArgumentException("Section start page must be greater than 0");
            }
        }
    }
}
