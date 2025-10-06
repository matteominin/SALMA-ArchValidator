package com.matteominin.pdf_extractor.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import com.matteominin.pdf_extractor.model.pdf.ExtractedSection;
import com.matteominin.pdf_extractor.model.pdf.PdfIndex;
import com.matteominin.pdf_extractor.util.ImageAwareTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

@Service
@Slf4j
public class PdfService {
    public String extractText(String filePath, String outputDirectory) {
        log.info("Starting text and image extraction for: {}", filePath);
        validateFilePath(filePath);

        StringBuilder result = new StringBuilder();
        PDDocument document = null;

        try {
            document = loadPDF(filePath);
            int totalPages = document.getNumberOfPages();
            log.info("Processing {} pages for extraction", totalPages);

            ImageAwareTextStripper stripper = new ImageAwareTextStripper();
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
            
            // Extract all sections except the last one
            for (int i = 0; i < index.getSections().size() - 1; i++) {
                PdfIndex.Section currentSection = index.getSections().get(i);
                PdfIndex.Section nextSection = index.getSections().get(i + 1);
                
                String text = extractSingleSection(
                    document, 
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

    private String extractSingleSection(PDDocument document, String currentSectionTitle, String nextSectionTitle, 
                                     int startPage, int endPage, PdfIndex index) {
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            
            int offset = calculateOffset(document, index);
            stripper.setStartPage(startPage);

            if (endPage == -1) {
                endPage = document.getNumberOfPages();
                stripper.setEndPage(endPage);
            } else if (endPage + offset <= document.getNumberOfPages()) {
                stripper.setEndPage(endPage + offset);
            } else {
                stripper.setEndPage(document.getNumberOfPages());
            }
            
            // Remove index from text before extracting section
            int indexEnd = index.getSections().get(0).getStart();
            
            String extractedText;
            if (startPage <= indexEnd) {
                extractedText = removeIndex(stripper, document, index);
            } else {
                extractedText = stripper.getText(document);
            }

            String pattern = "(?s)" + Pattern.quote(currentSectionTitle) + "(.*?)" + 
                           (nextSectionTitle != null ? Pattern.quote(nextSectionTitle) : "$");
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(extractedText);
            
            if (matcher.find()) {
                String sectionText = matcher.group(1).trim();
                log.debug("Successfully extracted section '{}', length: {}", currentSectionTitle, sectionText.length());
                return sectionText;
            } else {
                log.warn("No content found for section: {}", currentSectionTitle);
                return "";
            }
        } catch (IOException e) {
            log.error("Error extracting section '{}' from PDF", currentSectionTitle, e);
            throw new RuntimeException("Error extracting section from PDF", e);
        }
    }

    private int calculateOffset(PDDocument document, PdfIndex index) {
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            String firstSectionTitle = index.getSections().get(0).getSection();
            int occurrence = 0;

            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                
                if (text.contains(firstSectionTitle) && occurrence == 0) {
                    occurrence++;
                } else if (text.contains(firstSectionTitle) && occurrence > 0) {
                    int offset = page - 1;
                    log.debug("Calculated page offset: {}", offset);
                    return offset;
                }
            }
            
            log.debug("No offset calculated, using default: 0");
            return 0;
        } catch (IOException e) {
            log.error("Error calculating page offset", e);
            throw new RuntimeException("Error calculating page offset", e);
        }
    }

    private String removeIndex(PDFTextStripper stripper, PDDocument document, PdfIndex index) {
        try {
            stripper.setStartPage(1);
            String firstContentTitle = index.getSections().get(0).getSection();
            String fullText = stripper.getText(document);

            // Build regex to match everything between the first occurrence and the next occurrence of the first section title
            String pattern = "(?s)" + Pattern.quote(firstContentTitle) + "(.*?)" + Pattern.quote(firstContentTitle);
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(fullText);

            String cleanedText;
            if (matcher.find()) {
                StringBuilder sb = new StringBuilder();
                sb.append(fullText.substring(0, matcher.start()));
                sb.append(firstContentTitle);
                sb.append(fullText.substring(matcher.end()));
                cleanedText = sb.toString();
                log.debug("Successfully removed index from text");
            } else {
                cleanedText = fullText;
                log.debug("No index pattern found, returning original text");
            }
            return cleanedText;
        } catch (IOException e) {
            log.error("Error removing index from PDF text", e);
            throw new RuntimeException("Error removing index from PDF text", e);
        }
    }

    public void extractImages(String filePath) {
        String outputFolder = "extracted_images";
        PDDocument document = null;
        try {
            document = loadPDF(filePath);
        } catch (IOException e) {
            log.error("Error loading PDF for image extraction", e);
            throw new RuntimeException("Error loading PDF for image extraction", e);
        }

        // Create the output folder if it doesn't exist
        File outputDir = new File(outputFolder);
        if (!outputDir.exists()) {
            boolean created = outputDir.mkdirs();
            if (created) {
                log.info("Created output directory: {}", outputFolder);
            } else {
                log.warn("Failed to create output directory: {}", outputFolder);
            }
        }

        PDPage page = document.getPage(5);
        PDResources resources = page.getResources();
        Iterable<COSName> xObjectNames = resources.getXObjectNames();
        for (COSName xObjectName : xObjectNames) {
            try {
                PDXObject xObject = resources.getXObject(xObjectName);
                if (xObject instanceof PDImageXObject) {
                    PDImageXObject PDImage = (PDImageXObject) xObject;
                    BufferedImage image = PDImage.getImage();

                    String imagePath = outputFolder + File.separator + "image_" + xObjectName.getName() + ".png";
                    File imageFile = new File(imagePath);

                    ImageIO.write(image, "png", imageFile);
                    log.info("Saved image to: {}", imagePath);
                }
            } catch (IOException e) {
                log.error("Error retrieving XObject from resources", e);
                throw new RuntimeException("Error retrieving XObject from resources", e);
            }
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
