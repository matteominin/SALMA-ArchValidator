package com.matteominin.pdf_extractor.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LatexService {

    private static final int MAX_RETRY_ATTEMPTS = 1;
    private static final String REPORTS_DIR = "/tmp/reports";

    private final ChatClient chatClient;

    @Value("${latex.pdflatex.command:pdflatex}")
    private String pdflatexCommand;

    public LatexService(@Qualifier("anthropicChatClient") ChatClient anthropicChatClient) {
        this.chatClient = anthropicChatClient;
    }

    /**
     * Converts a LaTeX report to PDF with automatic error fixing using LLM.
     *
     * @param latexContent The LaTeX source code
     * @return The path to the generated PDF file
     * @throws IOException If file operations fail
     * @throws RuntimeException If PDF compilation fails after all retry attempts
     */
    public String compileLatex(String latexContent) throws IOException {
        // Create reports directory if it doesn't exist
        Path reportsPath = Paths.get(REPORTS_DIR);
        if (!Files.exists(reportsPath)) {
            Files.createDirectories(reportsPath);
        }

        // Generate unique filename with timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String baseFileName = "swe_report_" + timestamp;
        
        latexContent = cleanLatexOutput(latexContent);

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            log.info("PDF compilation attempt {}/{}", attempt, MAX_RETRY_ATTEMPTS);

            try {
                // Save LaTeX content to file
                Path texFilePath = reportsPath.resolve(baseFileName + ".tex");
                Files.writeString(texFilePath, latexContent, StandardCharsets.UTF_8);

                // Try to compile PDF
                CompilationResult result = null;
                for (int i = 0; i < 3; i++) {
                    result = compilePdf(texFilePath);
                }

                if (result != null && result.success) {
                    // Clean up auxiliary files
                    cleanupAuxiliaryFiles(reportsPath, baseFileName);

                    Path pdfPath = reportsPath.resolve(baseFileName + ".pdf");
                    File reportFile = pdfPath.toFile();
                    
                    log.info("PDF generated successfully at: {}", pdfPath.toAbsolutePath());
                    return reportFile.getAbsolutePath();
                } else {
                    log.warn("Compilation failed on attempt {}. Error: {}", attempt, result.errorMessage);

                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        // Use LLM to fix the error with targeted approach
                        latexContent = fixLatexTargeted(latexContent, result.errorMessage, attempt);
                    } else {
                        // All attempts exhausted
                        throw new RuntimeException(
                            String.format("Failed to compile PDF after %d attempts. Last error: %s",
                                MAX_RETRY_ATTEMPTS, result.errorMessage)
                        );
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("PDF compilation was interrupted", e);
            }
        }

        throw new RuntimeException("Failed to compile PDF after " + MAX_RETRY_ATTEMPTS + " attempts");
    }

    /**
     * Compiles a LaTeX file to PDF using pdflatex.
     */
    private CompilationResult compilePdf(Path texFilePath) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
            pdflatexCommand,
            "-interaction=nonstopmode",
            "-output-directory=" + texFilePath.getParent().toAbsolutePath(),
            texFilePath.toAbsolutePath().toString()
        );

        processBuilder.directory(texFilePath.getParent().toFile());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        // Capture output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new CompilationResult(false, "Compilation timeout after 60 seconds");
        }

        int exitCode = process.exitValue();

        if (exitCode == 0) {
            return new CompilationResult(true, null);
        } else {
            // Extract error message from log file
            String errorMessage = extractErrorFromLog(texFilePath);
            if (!StringUtils.hasText(errorMessage)) {
                errorMessage = output.toString();
            }
            return new CompilationResult(false, errorMessage);
        }
    }

    /**
     * Extracts error messages from the LaTeX log file.
     */
    private String extractErrorFromLog(Path texFilePath) {
        Path logFile = texFilePath.getParent().resolve(
            texFilePath.getFileName().toString().replace(".tex", ".log")
        );

        if (!Files.exists(logFile)) {
            return "Log file not found";
        }

        try {
            List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            List<String> errorLines = new ArrayList<>();
            boolean inError = false;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);

                // Detect error markers
                if (line.startsWith("!") || line.contains("Error:") || line.contains("error")) {
                    inError = true;
                    errorLines.add(line);
                } else if (inError) {
                    errorLines.add(line);
                    // Continue for a few more lines for context
                    if (errorLines.size() > 50) {
                        break;
                    }
                    // Stop if we hit a blank line after error
                    if (line.trim().isEmpty() && errorLines.size() > 5) {
                        inError = false;
                    }
                }
            }

            if (errorLines.isEmpty()) {
                // If no explicit errors found, return last 30 lines of log
                int startIdx = Math.max(0, lines.size() - 30);
                return lines.subList(startIdx, lines.size()).stream()
                    .collect(Collectors.joining("\n"));
            }

            return errorLines.stream().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            log.error("Failed to read log file: {}", logFile, e);
            return "Failed to read log file: " + e.getMessage();
        }
    }

    /**
     * Parses LaTeX error message to extract line number.
     */
    private ErrorInfo parseLatexError(String errorMessage) {
        int lineNumber = -1;

        // Try to extract line number from error message
        // Common patterns: "l.123", "line 123", "at line 123"
        java.util.regex.Pattern linePattern = java.util.regex.Pattern.compile(
            "(?:^l\\.(\\d+)|line\\s+(\\d+)|at\\s+line\\s+(\\d+))",
            java.util.regex.Pattern.MULTILINE
        );
        java.util.regex.Matcher lineMatcher = linePattern.matcher(errorMessage);

        if (lineMatcher.find()) {
            for (int i = 1; i <= 3; i++) {
                String match = lineMatcher.group(i);
                if (match != null) {
                    lineNumber = Integer.parseInt(match);
                    break;
                }
            }
        }

        log.debug("Parsed error - Line: {}", lineNumber);
        return new ErrorInfo(lineNumber);
    }

    /**
     * Extracts a context window around a specific line from the LaTeX content.
     */
    private String extractContext(String latexContent, int centerLine, int windowSize) {
        String[] lines = latexContent.split("\n");

        if (centerLine <= 0 || centerLine > lines.length) {
            // If line number is invalid, return the whole content
            return latexContent;
        }

        // Convert 1-based line number to 0-based array index
        int centerIndex = centerLine - 1;
        int startLine = Math.max(0, centerIndex - windowSize);
        int endLine = Math.min(lines.length, centerIndex + windowSize + 1);

        StringBuilder context = new StringBuilder();
        for (int i = startLine; i < endLine; i++) {
            context.append(lines[i]).append("\n");
        }

        log.debug("Extracted context: lines {}-{} (center: {}, window: {})",
            startLine + 1, endLine, centerLine, windowSize);
        return context.toString();
    }

    /**
     * Replaces specific lines in the LaTeX content with corrected lines.
     */
    private String replaceLines(String originalContent, int startLine, int endLine, String correctedLines) {
        String[] lines = originalContent.split("\n", -1);

        if (startLine < 1 || endLine > lines.length || startLine > endLine) {
            log.warn("Invalid line range: {}-{} (total lines: {})", startLine, endLine, lines.length);
            return originalContent;
        }

        StringBuilder result = new StringBuilder();

        // Add lines before the replacement
        for (int i = 0; i < startLine - 1; i++) {
            result.append(lines[i]).append("\n");
        }

        // Add corrected lines
        result.append(correctedLines);
        if (!correctedLines.endsWith("\n")) {
            result.append("\n");
        }

        // Add lines after the replacement
        for (int i = endLine; i < lines.length; i++) {
            result.append(lines[i]);
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        log.info("Replaced lines {}-{} with corrected content ({} chars)",
            startLine, endLine, correctedLines.length());
        return result.toString();
    }

    /**
     * Fixes LaTeX compilation errors using progressive context expansion.
     * Starts with small context window and expands if needed.
     */
    private String fixLatexTargeted(String latexContent, String errorMessage, int attemptNumber) {
        log.info("Using targeted fix approach (attempt {})", attemptNumber);

        // Parse error to get line number
        ErrorInfo errorInfo = parseLatexError(errorMessage);

        // Progressive window sizes: small -> medium -> large -> full document
        int[] windowSizes = {10, 30, 100, Integer.MAX_VALUE};
        String windowLabels[] = {"small (±10 lines)", "medium (±30 lines)", "large (±100 lines)", "full document"};

        for (int i = 0; i < windowSizes.length; i++) {
            int windowSize = windowSizes[i];
            String label = windowLabels[i];

            log.info("Attempting fix with {} context window", label);

            String contextToFix;
            int contextStartLine;
            int contextEndLine;

            if (errorInfo.lineNumber > 0 && windowSize < Integer.MAX_VALUE) {
                // Targeted fix with context window
                String[] lines = latexContent.split("\n");
                contextStartLine = Math.max(1, errorInfo.lineNumber - windowSize);
                contextEndLine = Math.min(lines.length, errorInfo.lineNumber + windowSize);
                contextToFix = extractContext(latexContent, errorInfo.lineNumber, windowSize);

                log.debug("Context window: lines {}-{} around error line {}",
                    contextStartLine, contextEndLine, errorInfo.lineNumber);
            } else {
                // Fallback: use full document
                contextToFix = latexContent;
                contextStartLine = 1;
                String[] lines = latexContent.split("\n");
                contextEndLine = lines.length;

                log.warn("Unable to identify error line or using full document fallback");
            }

            try {
                // Request LLM fix for this context
                String fixedContext = requestLLMFix(
                    contextToFix,
                    errorMessage,
                    attemptNumber,
                    errorInfo.lineNumber,
                    contextStartLine,
                    contextEndLine,
                    windowSize < Integer.MAX_VALUE
                );

                // Apply the fix
                if (windowSize < Integer.MAX_VALUE && errorInfo.lineNumber > 0) {
                    // Replace only the fixed context
                    String fixedLatex = replaceLines(latexContent, contextStartLine, contextEndLine, fixedContext);
                    log.info("Applied targeted fix to lines {}-{}", contextStartLine, contextEndLine);
                    return fixedLatex;
                } else {
                    // Full document was fixed
                    log.info("Applied full document fix");
                    return fixedContext;
                }

            } catch (Exception e) {
                log.warn("Fix attempt with {} failed: {}", label, e.getMessage());

                // If this is the last window size, rethrow
                if (i == windowSizes.length - 1) {
                    throw e;
                }

                // Otherwise, continue to next window size
                log.info("Expanding context window to try again...");
            }
        }

        // Should never reach here
        throw new RuntimeException("All fix attempts exhausted");
    }

    /**
     * Requests LLM to fix LaTeX errors in the given context.
     */
    private String requestLLMFix(
            String contextToFix,
            String errorMessage,
            int attemptNumber,
            int errorLine,
            int contextStartLine,
            int contextEndLine,
            boolean isTargeted) {

        String systemPrompt = """
            You are an expert LaTeX compiler and debugger.
            Your task is to fix LaTeX compilation errors with MINIMAL changes.

            IMPORTANT RULES:
            1. Make ONLY the changes necessary to fix the error
            2. Do NOT reformat or restructure working code
            3. Do NOT add explanations or markdown formatting
            4. Return ONLY the corrected LaTeX code
            5. Preserve all line breaks and spacing from the original

            Common issues to watch for:
            - Missing or incorrect packages
            - Unescaped special characters (%, $, &, #, _, {, })
            - Unclosed environments
            - Missing braces or brackets
            - Invalid commands or syntax
            - Encoding issues

            Return the complete fixed LaTeX code that can replace the provided context.
            """;

        String userPrompt;
        if (isTargeted) {
            userPrompt = String.format("""
                Attempt: %d of %d
                Error line: %d
                Context: lines %d-%d

                Compilation error:
                ```
                %s
                ```

                LaTeX context to fix:
                ```latex
                %s
                ```

                Fix ONLY what is necessary to resolve this error. Return the corrected lines.
                """,
                attemptNumber, MAX_RETRY_ATTEMPTS,
                errorLine, contextStartLine, contextEndLine,
                errorMessage,
                contextToFix);
        } else {
            userPrompt = String.format("""
                Attempt: %d of %d

                Compilation error:
                ```
                %s
                ```

                LaTeX document:
                ```latex
                %s
                ```

                Fix the error and return the corrected document.
                """,
                attemptNumber, MAX_RETRY_ATTEMPTS,
                errorMessage,
                contextToFix);
        }

        SystemMessage systemMessage = new SystemMessage(systemPrompt);
        UserMessage userMessage = new UserMessage(userPrompt);

        String fixedLatex = chatClient.prompt()
            .messages(systemMessage, userMessage)
            .call()
            .content();

        // Clean up any markdown formatting
        fixedLatex = cleanLatexOutput(fixedLatex);

        log.info("LLM provided fixed LaTeX (length: {} chars)", fixedLatex.length());
        return fixedLatex;
    }

    protected static String cleanLatexOutput(String report) {
        // Remove markdown code block markers (```latex or ```)
        report = report.replaceAll("^```latex\\s*\n?", "");
        report = report.replaceAll("^```\\s*\n?", "");
        report = report.replaceAll("\n?```\\s*$", "");

        // Remove any title/introduction before \documentclass
        if (report.contains("\\documentclass")) {
            int docclassIndex = report.indexOf("\\documentclass");
            report = report.substring(docclassIndex);
        }

        // Trim any leading/trailing whitespace
        report = report.trim();

        return report;
    }

    /**
     * Cleans up auxiliary LaTeX files (.aux, .log, etc.).
     */
    private void cleanupAuxiliaryFiles(Path directory, String baseFileName) {
        String[] extensions = {".aux", ".log", ".out", ".toc", ".nav", ".snm"};

        for (String ext : extensions) {
            try {
                Path auxFile = directory.resolve(baseFileName + ext);
                if (Files.exists(auxFile)) {
                    Files.delete(auxFile);
                    log.debug("Deleted auxiliary file: {}", auxFile.getFileName());
                }
            } catch (IOException e) {
                log.warn("Failed to delete auxiliary file: {}{}", baseFileName, ext, e);
            }
        }
    }

    /**
     * Internal class to hold compilation results.
     */
    private static class CompilationResult {
        final boolean success;
        final String errorMessage;

        CompilationResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Internal class to hold parsed error information.
     */
    private static class ErrorInfo {
        final int lineNumber;

        ErrorInfo(int lineNumber) {
            this.lineNumber = lineNumber;
        }
    }
}
