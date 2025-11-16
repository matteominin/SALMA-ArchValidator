package com.matteominin.pdf_extractor.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

        // Create report folder
        Path reportDir = Files.createDirectories(Paths.get("report", String.valueOf(System.currentTimeMillis())));
        // Save inputs
        Files.writeString(reportDir.resolve("extracted_data.json"), extractedData);
        Files.writeString(reportDir.resolve("req_to_uc.json"), req_to_uc);
        Files.writeString(reportDir.resolve("uc_to_arc.json"), uc_to_arc);
        Files.writeString(reportDir.resolve("uc_to_test.json"), uc_to_test);
        Files.writeString(reportDir.resolve("traceability_map.json"), treaceabilityMap);

        // Read the sample LaTeX template
        String sampleReport = Files.readString(Paths.get("report/architectural_validation_report copy.tex"));

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
                        "### Document Structure (Follow this EXACT order):\n" + //
                        "1. **Executive Summary** (Section 1)\n" + //
                        "   - Assessment overview with metrics summary table\n" + //
                        "   - Visual graphs using pgfplots/tikz: Coverage bar chart, Test distribution chart, Risk distribution chart\n" + //
                        "   - Critical findings (NO specific REQ/UC/TEST references - describe issues generically)\n" + //
                        "   - Architectural strengths\n" + //
                        "\n" + //
                        "2. **Project Inventory** (Section 2) - MUST COME BEFORE ANY ANALYSIS\n" + //
                        "   - 2.1 Requirements Inventory: Complete longtable with ALL requirements (REQ-1 to REQ-N)\n" + //
                        "   - 2.2 Use Cases Inventory: Complete longtable with ALL use cases (UC-1 to UC-N)\n" + //
                        "   - 2.3 Test Suite Inventory: Complete longtable with ALL tests (T-1 to T-N)\n" + //
                        "   - ALL tables MUST use alternating row colors: \\rowcolors{2}{white}{tablerowgray}\n" + //
                        "   - ALL table headers MUST use: \\rowcolor{white}\n" + //
                        "   - Each row MUST have a \\label{req:N}, \\label{uc:N}, or \\label{test:N}\n" + //
                        "\n" + //
                        "3. **Requirements Coverage Analysis** (Section 3)\n" + //
                        "   - Offline operation gaps, vague requirements, quality issues\n" + //
                        "   - Reference requirements using \\hyperref[req:N]{REQ-N}\n" + //
                        "\n" + //
                        "4. **Use Case Analysis** (Section 4)\n" + //
                        "   - Test coverage gaps, well-covered use cases\n" + //
                        "   - Reference use cases using \\hyperref[uc:N]{UC-N}\n" + //
                        "\n" + //
                        "5. **Architectural Quality Assessment** (Section 5)\n" + //
                        "   - Layered architecture, design patterns, domain model\n" + //
                        "\n" + //
                        "6. **Test Strategy Assessment** (Section 6)\n" + //
                        "   - Infrastructure quality, strengths, gaps\n" + //
                        "   - Reference tests using \\hyperref[test:N]{T-N}\n" + //
                        "\n" + //
                        "7. **Critical Risks and Recommendations** (Section 7)\n" + //
                        "   - Risk summary table with \\hyperref links to requirements/use cases\n" + //
                        "   - Detailed risk descriptions with recommendations\n" + //
                        "   - Action items table with priorities and timelines\n" + //
                        "\n" + //
                        "8. **Conclusion** (Section 8)\n" + //
                        "   - Overall assessment, recommended actions, final assessment\n" + //
                        "\n" + //
                        "### Critical LaTeX Requirements:\n" + //
                        "- Use \\usepackage[table]{xcolor} and define \\definecolor{tablerowgray}{RGB}{245,245,245}\n" + //
                        "- Use \\usepackage{pgfplots} and \\usepackage{tikz} for graphs\n" + //
                        "- ALL inventory tables MUST use longtable with alternating colors\n" + //
                        "- ALL items MUST be labeled before being referenced: \\label{req:1}REQ-1, \\label{uc:1}UC-1, \\label{test:1}T-1\n" + //
                        "- Use \\hyperref[label]{text} for ALL cross-references to requirements, use cases, and tests\n" + //
                        "- NEVER reference a REQ/UC/TEST ID before its table is defined\n" + //
                        "- Include graphs in Executive Summary: bar charts for coverage, test distribution\n" + //
                        "\n" + //
                        "### Analysis Requirements:\n" + //
                        "- Identify and analyze UNSUPPORTED requirements (from req_to_uc)\n" + //
                        "- Highlight use cases not covered by architecture (from uc_to_arc)\n" + //
                        "- Detail test coverage gaps (Complete/Partial/Missing from uc_to_test)\n" + //
                        "- List and analyze orphaned artifacts (from map.orphans)\n" + //
                        "- Identify vague or ambiguous requirements needing clarification\n" + //
                        "- Highlight architectural strengths and well-covered areas\n" + //
                        "- Provide actionable, prioritized recommendations\n" + //
                        "\n" + //
                        "### Output Format:\n" + //
                        "- Output ONLY valid LaTeX code\n" + //
                        "- NO comments, markdown formatting, or code blocks\n" + //
                        "- Start with \\documentclass and end with \\end{document}\n" + //
                        "- Keep report length 10-15 pages when compiled\n" + //
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
        Path texPath = Files.writeString(reportDir.resolve("report.tex"), response);

        String pdfPath;
        try {
            pdfPath = latexService.compileLatex(texPath);
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
