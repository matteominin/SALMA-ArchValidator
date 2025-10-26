package com.matteominin.pdf_extractor.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.matteominin.pdf_extractor.service.LatexService;

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
        String req_to_uc = request.get("req_to_uc") != null ? request.get("req_to_uc").toString() : "";
        String uc_to_arc = request.get("uc_to_arc") != null ? request.get("uc_to_arc").toString() : "";
        String uc_to_test = request.get("uc_to_test") != null ? request.get("uc_to_test").toString() : "";
        String treaceabilityMap = request.get("map") != null ? request.get("map").toString() : "";

        // Read the sample LaTeX template
        String sampleReport = Files.readString(Paths.get("report/architectural_validation_report.tex"));

        String prompt = String.format("You are an expert software architect generating comprehensive architectural validation reports. You analyze traceability data from software projects and produce professional LaTeX validation reports following a specific template structure.\n" + //
                        "\n" + //
                        "  ## INPUT DATA STRUCTURE\n" + //
                        "\n" + //
                        "  You will receive four inputs:\n" + //
                        "\n" + //
                        "  1. **req_to_uc**: Requirements-to-Use Cases mapping (array of objects with req_id, covered_by_use_cases, status, rationale)\n" + //
                        "\t%s\n" + //
                        "  2. **uc_to_arc**: Use Cases-to-Architecture mapping (array of objects with uc_id, implemented_by_components, status, rationale)\n" + //
                        "\t%s\n" + //
                        "  3. **uc_to_test**: Use Cases-to-Tests mapping (array of objects with uc_id, main_flow_tested, alternative_flow_tested, status,\n" + //
                        "  missing_flows, rationale)\n" + //
                        "\t%s\n" + //
                        "  4. **map**: Complete traceability map (JSON object with treaceability_matrix array containing req_id, use_cases, components, tests,\n" + //
                        "  mockups, staus)\n" + //
                        "\t%s\n" + //
                        "\n" + //
                        "  ## YOUR TASK\n" + //
                        "\n" + //
                        "  Generate a complete, production-ready LaTeX architectural validation report taking the following latex as example:\n" + //
                        "%s\n" + //
                        " Make sure to not add any comment at the beginning or the end of the LaTeX document. Put special attention on the incorrect mappings and generate insightful analysis and recommendations to address the identified gaps in the architecture validation. Mention some of the strength points analyzed, keep the report compact, not too many pages, keep around 7-12 pages\n" + //,
                        "", req_to_uc, uc_to_arc, uc_to_test, treaceabilityMap, sampleReport);

        String response = chatClient.prompt()
                .system("You are a helpful assistant that generates LaTeX reports based on traceability data.")
                .user(prompt)  
                .call()
                .content();
        
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
}
