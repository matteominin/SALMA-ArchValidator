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
        String validationReport = request.get("validation_report") != null ? request.get("validation_report").toString() : "";

        // Create report folder
        Path reportDir = Files.createDirectories(Paths.get("report", String.valueOf(System.currentTimeMillis())));
        // Save inputs
        Files.writeString(reportDir.resolve("extracted_data.json"), extractedData);
        Files.writeString(reportDir.resolve("req_to_uc.json"), req_to_uc);
        Files.writeString(reportDir.resolve("uc_to_arc.json"), uc_to_arc);
        Files.writeString(reportDir.resolve("uc_to_test.json"), uc_to_test);
        Files.writeString(reportDir.resolve("traceability_map.json"), treaceabilityMap);
        Files.writeString(reportDir.resolve("validation_report.json"), validationReport);

        // Read the sample LaTeX template (using excellent example with high grade)
        String sampleReport = Files.readString(Paths.get("report/sample.tex"));

        String prompt = String.format("You are an expert software architect generating comprehensive architectural validation reports. You analyze traceability data from software projects and produce professional LaTeX validation reports with OBJECTIVE, EVIDENCE-BASED assessments.\n" + //
                        "\n" + //
                        "## EVALUATION PHILOSOPHY\n" + //
                        "\n" + //
                        "**CRITICAL**: Your assessment must be based SOLELY on the actual quality of the architecture as evidenced in the data. DO NOT artificially inflate or deflate grades.\n" + //
                        "\n" + //
                        "### Grading Scale (Out of 30):\n" + //
                        "- **27-30 (Excellent)**: Near-perfect architecture with comprehensive coverage (>95%%), clear boundaries, resilience patterns, domain events, minimal gaps\n" + //
                        "- **24-26 (Very Good)**: Strong architecture with high coverage (85-95%%), good separation of concerns, minor improvements needed\n" + //
                        "- **21-23 (Good)**: Solid architecture with acceptable coverage (75-85%%), some architectural gaps but no critical issues\n" + //
                        "- **18-20 (Sufficient)**: Basic architecture with moderate coverage (65-75%%), several gaps requiring attention\n" + //
                        "- **15-17 (Mediocre)**: Weak architecture with low coverage (<65%%), multiple critical gaps\n" + //
                        "- **<15 (Insufficient)**: Fundamentally flawed architecture, major gaps across all areas\n" + //
                        "\n" + //
                        "### Key Evaluation Criteria:\n" + //
                        "1. **Coverage** (40%% of grade): Requirements, use cases, test coverage percentages\n" + //
                        "2. **Architecture Quality** (30%% of grade): Clear boundaries, SOLID principles, design patterns, aggregate roots\n" + //
                        "3. **Resilience** (15%% of grade): Offline capability, error handling, edge cases\n" + //
                        "4. **Testing** (15%% of grade): Test pyramid, error path coverage, integration tests\n" + //
                        "\n" + //
                        "### Excellence Indicators (High Grades 27-30):\n" + //
                        "- Requirements coverage >95%%\n" + //
                        "- Use case coverage >90%%\n" + //
                        "- Precise component boundaries documented (not vague \"manages data\" but specific CRUD operations listed)\n" + //
                        "- Offline operation fully supported (local storage, sync protocol, conflict resolution)\n" + //
                        "- Domain events infrastructure implemented\n" + //
                        "- Aggregate root enforcement (package-private modifications)\n" + //
                        "- Standardized error responses across API\n" + //
                        "- Comprehensive test pyramid (>70%% unit tests)\n" + //
                        "- Security best practices (OWASP Top 10, PCI-DSS if applicable)\n" + //
                        "\n" + //
                        "### Critical Issues (Low Grades <20):\n" + //
                        "- Requirements coverage <70%%\n" + //
                        "- No offline support when explicitly required\n" + //
                        "- Vague component responsibilities (\"manages business logic\")\n" + //
                        "- No error handling strategy\n" + //
                        "- Missing critical architectural layers\n" + //
                        "- No tests or minimal testing (<50 tests for medium project)\n" + //
                        "\n" + //
                        "**IMPORTANT**: If the architecture demonstrates high coverage and good design, GIVE IT A HIGH GRADE (27-30). If it has critical gaps, give it a LOW GRADE (<20). Be OBJECTIVE, not pessimistic or optimistic.\n" + //
                        "\n" + //
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
                        "### 3. PDF FEATURE VALIDATION REPORT\n" + //
                        "This contains the validation results from analyzing the PDF documentation against expected features:\n" + //
                        "- success: whether validation passed the threshold\n" + //
                        "- threshold: minimum coverage percentage required\n" + //
                        "- totalSummaryFeatures: total expected features\n" + //
                        "- providedFeatures: features found in the PDF\n" + //
                        "- coverage.coveragePercentage: percentage of features covered\n" + //
                        "- coverage.coveredFeatures[]: features found with similarity scores and evidence\n" + //
                        "- coverage.uncoveredFeatures[]: features not found in the PDF\n" + //
                        "\n" + //
                        "VALIDATION REPORT:\n" + //
                        "%s\n" + //
                        "\n" + //
                        "## YOUR TASK\n" + //
                        "\n" + //
                        "Generate a complete, production-ready LaTeX architectural validation report following the EXACT structure and style of this example:\n" + //
                        "\n" + //
                        "EXAMPLE LATEX REPORT:\n" + //
                        "%s\n" + //
                        "\n" + //
                        "## CRITICAL STYLING REQUIREMENTS\n" + //
                        "\n" + //
                        "### Color Scheme (MUST match exactly):\n" + //
                        "- tableheader: RGB(0,82,147) - UniFi blue for ALL table headers\n" + //
                        "- tablerow1: RGB(240,245,250) - Light blue for alternating rows\n" + //
                        "- tablerow2/white: RGB(255,255,255) - White for alternating rows\n" + //
                        "- successgreen: RGB(39,174,96) - For coverage ≥70%%\n" + //
                        "- dangerred: RGB(192,57,43) - For coverage <70%%, warnings, critical issues\n" + //
                        "- warningyellow: RGB(243,156,18) - For warning boxes\n" + //
                        "- darkgray: RGB(52,73,94) - For Critical Gap Analysis table headers\n" + //
                        "\n" + //
                        "### Section-Specific Formatting:\n" + //
                        "\n" + //
                        "#### Sections 2.1-2.5 (Functional Domains like Authentication, Payment, Inventory):\n" + //
                        "- Header table with tabularx: {Xr} columns (Section Information | Coverage Status)\n" + //
                        "- Left side: Scope, Use Cases, Requirements in minipage\n" + //
                        "- Right side: TikZ donut chart (green ≥70%%, red <70%%, orange for partial coverage)\n" + //
                        "  * Use arc calculations: angle = 360 × percentage / 100\n" + //
                        "  * Percentage text inside donut using \\small\\textbf{XX%%}\n" + //
                        "  * Below chart: requirement count and test count\n" + //
                        "- \"Requirements Detail\" table: {lXc} columns with CENTERED Status column\n" + //
                        "- \"Architecture Components\" section with descriptive text + table\n" + //
                        "- \"Test Coverage\" section with descriptive text\n" + //
                        "- \"Issues & Recommendations\" with warningyellow tcolorbox\n" + //
                        "- Use \\vspace{1.5cm} between sections (NO \\rule separator lines)\n" + //
                        "\n" + //
                        "#### Section 2.6 (Architectural Quality Assessment):\n" + //
                        "- NARRATIVE STYLE with clear subsections (NOT the header table format)\n" + //
                        "- Subsection 1: \"Architectural Overview\" - Table showing 6 layers\n" + //
                        "  * Use \\begin{center} \\begin{tabular}{@{}p{0.20\\textwidth}p{0.35\\textwidth}p{0.35\\textwidth}@{}}\n" + //
                        "  * Header: \\rowcolor{tableheader} with \\textcolor{white}{\\rule{0pt}{3ex}\\textbf{...}}\n" + //
                        "  * Rows: alternate \\rowcolor{tablerow1} and \\rowcolor{white}\n" + //
                        "  * Brief text describing layering benefits, design patterns, and DDD principles\n" + //
                        "- Subsection 2: \"Architectural Strengths\" - Narrative style with 5 numbered points\n" + //
                        "  * NO colored boxes - just clean paragraph format with bold numbered items\n" + //
                        "  * Format: \\textbf{N. Title} — Description\n" + //
                        "- Subsection 3: \"Critical Weaknesses \\& How to Improve\" - 3 weaknesses as subsubsections\n" + //
                        "  * Each weakness: problem description paragraph + \"Recommended Actions:\" table\n" + //
                        "  * Tables use \\begin{center} \\begin{tabular} with alternating row colors\n" + //
                        "  * Header: \\rowcolor{tableheader} with \\textcolor{white}{\\rule{0pt}{3ex}\\textbf{...}}\n" + //
                        "  * Rows: alternate \\rowcolor{tablerow1} and \\rowcolor{white}\n" + //
                        "  * Table columns vary: {p{0.24\\textwidth}p{0.68\\textwidth}} or {p{0.32\\textwidth}p{0.60\\textwidth}} or {p{0.27\\textwidth}p{0.65\\textwidth}}\n" + //
                        "  * NO effort/priority/timeline information\n" + //
                        "- End with \"Overall Assessment\" paragraph\n" + //
                        "- NO colored tcolorboxes in this section - use clean tables only\n" + //
                        "\n" + //
                        "#### Section 2.7 (Testing Quality):\n" + //
                        "- NARRATIVE STYLE - Single subsection named \"Testing Quality\" (NOT the header table format)\n" + //
                        "- Start with introductory paragraph about test suite (63 tests, test pyramid pattern)\n" + //
                        "- Test Distribution Pie Chart with leader lines:\n" + //
                        "  * Use \\begin{figure}[h] with TikZ pie chart\n" + //
                        "  * Define colors: unittestblue RGB(52,152,219), integrationgreen RGB(46,204,113), systemyellow RGB(241,196,15)\n" + //
                        "  * Radius: 2.2cm\n" + //
                        "  * Three slices: Unit 71%%, Integration 19%%, System 10%%\n" + //
                        "  * Labels connected with \\draw[thick] lines to external positions\n" + //
                        "  * Unit Tests label on right: \\node[right, align=left] at (30:3.2cm) {\\textbf{Unit Tests (71\\%%)}\\\\45 tests}\n" + //
                        "  * Integration label on left: \\node[left, align=right] at (-200:3.5cm) {\\textbf{Integration (19\\%%)}\\\\12 tests}\n" + //
                        "  * System label below: \\node[below left, align=right] at (-252:3.2cm) {\\textbf{System (10\\%%)}\\\\6 tests}\n" + //
                        "  * Caption: \"Test Distribution Following Test Pyramid (Total: 63 tests)\"\n" + //
                        "- Paragraph explaining test pyramid distribution\n" + //
                        "- Paragraphs on DAO-level testing and service isolation (NO tables for error coverage)\n" + //
                        "- Subsection \"Testing Gaps\" with table:\n" + //
                        "  * Use \\begin{center} \\begin{tabular} with alternating row colors\n" + //
                        "  * Header: \\rowcolor{tableheader} with white text\n" + //
                        "  * Rows: alternate \\rowcolor{tablerow1} and \\rowcolor{white}\n" + //
                        "  * Columns: Gap Type | Missing Coverage | Risk/Impact\n" + //
                        "  * Include ONLY: End-to-End Workflows, Navigation Tests, Hardware Integration\n" + //
                        "  * DO NOT include: Performance Tests, Security Tests\n" + //
                        "- NO colored tcolorboxes - use clean tables with rowcolor style\n" + //
                        "\n" + //
                        "#### Critical Gap Analysis Format:\n" + //
                        "- Use tcolorbox with criticalbox style (dangerred frame and background)\n" + //
                        "- Title: {\\Large CRITICAL: [Gap Description]}\n" + //
                        "- Two side-by-side minipage tables (each 0.48\\textwidth)\n" + //
                        "- LEFT table: Business Impact with columns {@{}p{0.15\\textwidth}p{0.80\\textwidth}@{}}\n" + //
                        "  * Header: darkgray background with white text\n" + //
                        "  * Columns: Severity | Impact\n" + //
                        "  * Rows: alternating tablerow1 and white\n" + //
                        "  * NO colored bullets in Severity column - just text (Critical, High)\n" + //
                        "- RIGHT table: Missing Components with columns {@{}p{0.10\\textwidth}p{0.85\\textwidth}@{}}\n" + //
                        "  * Header: darkgray background with white text\n" + //
                        "  * Columns: # | Component\n" + //
                        "  * Rows: alternating tablerow1 and white\n" + //
                        "- Root Cause statement at bottom with \\textbf\n" + //
                        "\n" + //
                        "### General Formatting Rules:\n" + //
                        "- CRITICAL: Use ONLY LaTeX syntax - NO markdown (**, *, #, etc.)\n" + //
                        "  * For bold: use \\textbf{text} NOT **text**\n" + //
                        "  * For italics: use \\textit{text} NOT *text*\n" + //
                        "  * For headers: use \\section, \\subsection NOT # or ##\n" + //
                        "  * ALWAYS close all \\textbf{ } braces properly\n" + //
                        "- Use titlesec package to make subsection titles LARGER: \\titleformat{\\subsection}{\\normalfont\\Large\\bfseries}{\\thesubsection}{1em}{}\n" + //
                        "- ALL tables use \\rowcolor for styling (NO booktabs \\toprule/\\midrule/\\bottomrule)\n" + //
                        "  * Header: \\rowcolor{tableheader} with \\textcolor{white}{\\rule{0pt}{3ex}\\textbf{...}}\n" + //
                        "  * Rows: alternate \\rowcolor{tablerow1} and \\rowcolor{white}\n" + //
                        "  * Use \\begin{center} \\begin{tabular} for inline tables (without captions)\n" + //
                        "  * Use \\begin{table}[h] \\begin{tabularx} ONLY for tables with captions in sections 2.1-2.5\n" + //
                        "- Use \\vspace{1.5cm} between major sections\n" + //
                        "- Use \\vspace{0.5cm} to \\vspace{0.8cm} between subsections\n" + //
                        "- NO horizontal separator lines (\\rule) anywhere\n" + //
                        "- Status column MUST be centered using @{}lXc@{} in tabularx\n" + //
                        "- Use \\textcolor{successgreen}{\\Large\\textbf{$\\bullet$}} for covered items\n" + //
                        "- Use \\textcolor{dangerred}{\\Large\\textbf{$\\bullet$}} for uncovered/critical items\n" + //
                        "\n" + //
                        "## REPORT STRUCTURE (Follow this EXACT order):\n" + //
                        "\n" + //
                        "1. **Executive Summary** (Section 1)\n" + //
                        "   - Abstract with key findings percentage summary (e.g., \"98.4%% requirements coverage, 94.4%% use case coverage, 0 critical risks\")\n" + //
                        "   - Assessment overview with metrics summary table showing 4 donut charts (Requirements, Use Cases, Tests, Architecture)\n" + //
                        "   - Quality assessment badges: If 0 critical = green \"0 Critical Issues\", if 1-2 = orange \"X High Priority\", if 3+ = red \"X Critical\"\n" + //
                        "   - Key Findings section:\n" + //
                        "     * If architecture is excellent (>95%% coverage): Title \"Architectural Excellence Highlights\" with positive tone\n" + //
                        "     * If architecture has gaps (<80%% coverage): Title \"Critical Issues Requiring Attention\" with critical tone\n" + //
                        "     * List 3-5 main findings (strengths if high quality, issues if low quality)\n" + //
                        "   - Enhancement Opportunities section (if grade >24) OR Critical Findings section (if grade <24)\n" + //
                        "\n" + //
                        "2. **Functional Domain Analysis** (Section 2) - Group by functional areas:\n" + //
                        "   2.1-2.5: Functional domains (Authentication, Payment, Inventory, etc.)\n" + //
                        "   - Use header table with donut chart format\n" + //
                        "   - Include Requirements Detail, Architecture Components, Test Coverage, Issues\n" + //
                        "   \n" + //
                        "   2.6: Architectural Quality Assessment (NARRATIVE STYLE - NO COLORED BOXES)\n" + //
                        "   - Subsections: Architectural Overview, Architectural Strengths, Critical Weaknesses & How to Improve\n" + //
                        "   - ALL tables use \\rowcolor styling (NO booktabs)\n" + //
                        "   - Strengths: narrative with numbered items (NO boxes)\n" + //
                        "   - Weaknesses: subsubsections with Recommended Actions tables (NO effort/priority)\n" + //
                        "   \n" + //
                        "   2.7: Testing Quality (NARRATIVE STYLE - NO COLORED BOXES)\n" + //
                        "   - Single subsection with pie chart (leader lines), Testing Gaps table\n" + //
                        "   - Pie chart shows test distribution (71%% Unit, 19%% Integration, 10%% System)\n" + //
                        "   - Testing Gaps: ONLY End-to-End Workflows, Navigation Tests, Hardware Integration\n" + //
                        "   - NO Error Path Test Coverage table, NO Performance/Security tests in gaps\n" + //
                        "   - ALL tables use \\rowcolor style (NO booktabs)\n" + //
                        "\n" + //
                        "3. **Cross-Cutting Concerns** (Section 3)\n" + //
                        "   - Error handling, requirements quality issues\n" + //
                        "   - Critical Gap Analysis using two-table boxed format\n" + //
                        "\n" + //
                        "4. **Risk Summary & Recommendations** (Section 4)\n" + //
                        "   - Risk table, detailed analyses, action items\n" + //
                        "\n" + //
                        "5. **Conclusions** (Section 5)\n" + //
                        "   - Subsections: Overall Assessment, Critical Gaps (if any), Final Verdict\n" + //
                        "   - NO Deployment Recommendation subsection\n" + //
                        "   - Use tables with \\rowcolor styling for strengths and gaps\n" + //
                        "   \n" + //
                        "   **GRADING INSTRUCTIONS (CRITICAL)**:\n" + //
                        "   - Calculate grade based on evaluation criteria weights:\n" + //
                        "     * Coverage (40%%): If req coverage >95%% = 12/12 points, 85-95%% = 10/12, 75-85%% = 8/12, 65-75%% = 6/12, <65%% = 4/12\n" + //
                        "     * Architecture (30%%): Clear boundaries + patterns + DDD = 9/9, Good separation = 7/9, Basic = 5/9, Weak = 3/9\n" + //
                        "     * Resilience (15%%): Full offline support = 4.5/4.5, Partial = 3/4.5, None but not required = 4/4.5, None and required = 1/4.5\n" + //
                        "     * Testing (15%%): >70%% unit + pyramid = 4.5/4.5, Good = 3.5/4.5, Basic = 2.5/4.5, Minimal = 1/4.5\n" + //
                        "   \n" + //
                        "   - Final Verdict must include:\n" + //
                        "     * Boxed grade (e.g., \"Overall Grade: 29/30\")\n" + //
                        "     * One-sentence quality summary\n" + //
                        "     * Grading Rationale paragraph explaining the score based on coverage, architecture, resilience, testing\n" + //
                        "     * If grade is 27-30: emphasize \"exceptional quality\", \"production-ready\", \"comprehensive\"\n" + //
                        "     * If grade is 21-26: emphasize \"strong fundamentals\", \"minor improvements needed\"\n" + //
                        "     * If grade is 15-20: emphasize \"significant gaps\", \"requires attention before production\"\n" + //
                        "     * If grade is <15: emphasize \"critical issues\", \"not ready for production\"\n" + //
                        "   \n" + //
                        "   **IMPORTANT**: The grade MUST reflect actual architecture quality. DO NOT default to medium grades. If coverage is >95%% and architecture is sound, grade should be 27-30.\n" + //
                        "\n" + //
                        "### LaTeX Requirements:\n" + //
                        "- Use \\usepackage[table]{xcolor}, \\usepackage{tabularx}, \\usepackage{tikz}, \\usepackage{titlesec}\n" + //
                        "- Define ALL colors exactly as specified above\n" + //
                        "- Use \\hyperref[label]{text} for ALL cross-references\n" + //
                        "- Label ALL items before referencing: \\label{req:N}, \\label{uc:N}, \\label{test:N}\n" + //
                        "\n" + //
                        "### Output Format:\n" + //
                        "- Output ONLY valid LaTeX code\n" + //
                        "- NO comments, markdown formatting, or code blocks\n" + //
                        "- Start with \\documentclass and end with \\end{document}\n" + //
                        "- Match the sample report's style EXACTLY - especially the donut charts, color scheme, and table formatting\n" + //
                        "\n" + //
                        "Generate the complete LaTeX document now:\n",
                        extractedData, req_to_uc, uc_to_arc, uc_to_test, treaceabilityMap, validationReport, sampleReport);

        ChatResponse chatResponse = chatClient.prompt()
                .system("You are an expert LaTeX document generator. Generate ONLY valid LaTeX code with NO markdown formatting or code blocks. CRITICAL: Use \\textbf{} for bold (NOT **), \\textit{} for italics (NOT *), and ALWAYS close all braces properly. Start directly with \\documentclass and end with \\end{document}.")
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
