package com.matteominin.pdf_extractor.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.matteominin.pdf_extractor.service.LatexService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/pdf-report")
public class PdfReport {

    private final ChatClient chatClient;

    @Autowired
    private LatexService latexService;

    public PdfReport(@Qualifier("anthropicChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/generate")
    public String generateReport(@RequestBody Map<String, Object> request) throws IOException {
        String extractedData = request.get("data") != null ? request.get("data").toString() : "";
        String req_to_uc = request.get("req_to_uc") != null ? request.get("req_to_uc").toString() : "";
        String uc_to_arc = request.get("uc_to_arc") != null ? request.get("uc_to_arc").toString() : "";
        String uc_to_test = request.get("uc_to_test") != null ? request.get("uc_to_test").toString() : "";
        String treaceabilityMap = request.get("map") != null ? request.get("map").toString() : "";

        // Read the sample LaTeX template
        String sampleReport = Files.readString(Paths.get("report/architectural_validation_report.tex"));

        String prompt = String.format("You are an expert software architect generating comprehensive architectural validation reports. You analyze traceability data from software projects and produce professional LaTeX validation reports following a specific template structure.\n" + //
                        "\n" + //
                        "## INPUT DATA STRUCTURE\n" + //
                        "\n" + //
                        "You will receive the following inputs:\n" + //
                        "\n" + //
                        "### 1. EXTRACTED DATA (Raw project artifacts)\n" + //
                        "This contains the original extracted information from the project documentation, including:\n" + //
                        "- Requirements specifications\n" + //
                        "- Use case descriptions\n" + //
                        "- Architecture components and patterns\n" + //
                        "- Test specifications\n" + //
                        "\n" + //
                        "EXTRACTED DATA:\n" + //
                        "%s\n" + //
                        "\n" + //
                        "### 2. TRACEABILITY ANALYSIS\n" + //
                        "\n" + //
                        "#### 2.1 Requirements-to-Use Cases Mapping (req_to_uc)\n" + //
                        "Array of objects with: req_id, covered_by_use_cases[], status (Covered/UNSUPPORTED), rationale\n" + //
                        "\n" + //
                        "REQ_TO_UC:\n" + //
                        "%s\n" + //
                        "\n" + //
                        "#### 2.2 Use Cases-to-Architecture Mapping (uc_to_arc)\n" + //
                        "Array of objects with: uc_id, implemented_by_components[], status (Covered/Not Covered), rationale\n" + //
                        "\n" + //
                        "UC_TO_ARC:\n" + //
                        "%s\n" + //
                        "\n" + //
                        "#### 2.3 Use Cases-to-Tests Mapping (uc_to_test)\n" + //
                        "Array of objects with: uc_id, main_flow_tested (true/false), alternative_flow_tested[], status (Complete/Partial/Missing), missing_flows[], rationale\n" + //
                        "\n" + //
                        "UC_TO_TEST:\n" + //
                        "%s\n" + //
                        "\n" + //
                        "#### 2.4 Complete Traceability Matrix (map)\n" + //
                        "JSON object with:\n" + //
                        "- traceability_matrix[]: {req_id, use_cases[], components[], tests[], mockups[], status}\n" + //
                        "- orphans: {requirements[], use_cases[], tests[], mockups[]}\n" + //
                        "\n" + //
                        "TRACEABILITY MAP:\n" + //
                        "%s\n" + //
                        "\n" + //
                        "## YOUR TASK\n" + //
                        "\n" + //
                        "Generate a complete, production-ready LaTeX architectural validation report following the structure and style of this example:\n" + //
                        "\n" + //
                        "EXAMPLE LATEX REPORT:\n" + //
                        "%s\n" + //
                        "\n" + //
                        "## REPORT REQUIREMENTS\n" + //
                        "\n" + //
                        "1. **Structure**: Follow the example LaTeX template structure exactly\n" + //
                        "2. **Analysis Focus**:\n" + //
                        "   - Identify and analyze UNSUPPORTED requirements (from req_to_uc)\n" + //
                        "   - Highlight use cases not covered by architecture (from uc_to_arc)\n" + //
                        "   - Detail test coverage gaps (Complete/Partial/Missing from uc_to_test)\n" + //
                        "   - List and analyze orphaned artifacts (from map.orphans)\n" + //
                        "3. **Strengths**: Mention well-traced requirements and comprehensive coverage areas\n" + //
                        "4. **Recommendations**: Provide actionable insights to address gaps\n" + //
                        "5. **Length**: Keep the report compact (7-12 pages)\n" + //
                        "6. **Format**: Output ONLY valid LaTeX code with NO comments, markdown formatting, or code blocks\n" + //
                        "\n" + //
                        "Generate the complete LaTeX document now:\n",
                        extractedData, req_to_uc, uc_to_arc, uc_to_test, treaceabilityMap, sampleReport);

        ChatResponse chatResponse = chatClient.prompt()
                .system("You are an expert LaTeX document generator. Generate ONLY valid LaTeX code with NO markdown formatting or code blocks. Start directly with \\documentclass and end with \\end{document}.")
                .user(prompt)
                .call()
                .chatResponse();

        log.info("Generate report {} input tokens, {} output tokens", chatResponse.getMetadata().getUsage().getPromptTokens(),
                chatResponse.getMetadata().getUsage().getCompletionTokens());
        
        String response = chatResponse.getResult().getOutput().getText();

        // Clean markdown code blocks if present
        response = cleanLatexResponse(response);

        // save response to a .tex file for debugging
        Files.writeString(Paths.get("report/generated_report.tex"), response);

        String pdfPath;
        try {
            pdfPath = latexService.compileLatex(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compile LaTeX report", e);
        }

        return pdfPath;
    }

    /**
     * Clean LaTeX response by removing markdown code blocks if present
     */
    private String cleanLatexResponse(String latex) {
        if (latex == null) {
            return latex;
        }

        // Remove markdown code blocks (```latex and ```)
        latex = latex.replaceAll("```latex\\s*", "");
        latex = latex.replaceAll("```\\s*", "");

        // Trim whitespace
        latex = latex.trim();

        return latex;
    }
}
