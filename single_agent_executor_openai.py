#!/usr/bin/env python3
"""
Single-Agent PDF Validation Report Executor (OpenAI Version)

This script executes the single-agent prompt to analyze a PDF and generate
a comprehensive validation report using OpenAI models (GPT-4/o1).
"""

import os
import sys
import json
import subprocess
import time
from pathlib import Path
from datetime import datetime
import fitz  # PyMuPDF for PDF text extraction
from openai import OpenAI

class SingleAgentExecutorOpenAI:
    """Executes the single-agent prompt for PDF validation using OpenAI"""

    def __init__(self, api_key=None, model=None):
        """
        Initialize the executor

        Args:
            api_key: OpenAI API key (defaults to OPENAI_API_KEY env var)
            model: Model to use (defaults to gpt-4-turbo)
                  Options: gpt-4-turbo, gpt-4, o1-preview, o1-mini
        """
        self.api_key = api_key or os.environ.get("OPENAI_API_KEY")
        if not self.api_key:
            raise ValueError("OPENAI_API_KEY environment variable not set")

        self.client = OpenAI(api_key=self.api_key)

        # Select model
        if model:
            self.model = model
        else:
            # Default to gpt-4-turbo (best for complex analysis)
            self.model = "gpt-4-turbo"

        print(f"[SingleAgent] Using model: {self.model}")

        # Detect if it's an o1 or GPT-5 model (they have different API requirements)
        self.is_o1_model = self.model.startswith("o1-")
        self.is_gpt5_model = self.model.startswith("gpt-5")

        # Load the single-agent prompt (V2 with strict LaTeX output requirements)
        prompt_path = Path(__file__).parent / "single-agent.md"
        with open(prompt_path, 'r', encoding='utf-8') as f:
            self.system_prompt = f.read()

        # Load the LaTeX template
        template_path = Path(__file__).parent / "report" / "sample.tex"
        with open(template_path, 'r', encoding='utf-8') as f:
            self.latex_template = f.read()
        print(f"[SingleAgent] LaTeX template loaded: {len(self.latex_template)} characters")

    def extract_text_from_pdf(self, pdf_path, output_dir):
        """
        Extract text from PDF using PyMuPDF

        Args:
            pdf_path: Path to PDF file
            output_dir: Directory to save extracted text

        Returns:
            str: Extracted text from PDF
        """
        print(f"[SingleAgent] Extracting text from PDF: {pdf_path}")

        try:
            doc = fitz.open(pdf_path)
            total_pages = len(doc)
            print(f"[SingleAgent] PDF has {total_pages} pages")

            # Extract text from all pages
            all_text = []
            page_texts = {}

            for page_num in range(total_pages):
                page = doc[page_num]
                text = page.get_text()
                all_text.append(f"--- Page {page_num + 1} ---\n{text}\n")
                page_texts[page_num + 1] = text

            doc.close()

            full_text = "\n".join(all_text)

            # Save extracted text
            text_output_path = output_dir / "extracted_text.txt"
            with open(text_output_path, 'w', encoding='utf-8') as f:
                f.write(full_text)

            print(f"[SingleAgent] Extracted text saved to: {text_output_path}")
            print(f"[SingleAgent] Total characters: {len(full_text):,}")

            # Estimate tokens (rough approximation: 1 token ≈ 4 characters)
            estimated_tokens = len(full_text) // 4
            print(f"[SingleAgent] Estimated tokens: {estimated_tokens:,}")

            if estimated_tokens > 100000:
                print("[SingleAgent] WARNING: PDF is very large. Consider splitting it.")

            # Save page-by-page text for reference
            pages_json_path = output_dir / "extracted_pages.json"
            with open(pages_json_path, 'w', encoding='utf-8') as f:
                json.dump(page_texts, f, indent=2, ensure_ascii=False)

            return full_text

        except Exception as e:
            print(f"[SingleAgent] ERROR extracting text: {e}")
            raise

    def execute(self, pdf_path, output_dir=None):
        """
        Execute the single-agent prompt on a PDF

        Args:
            pdf_path: Path to the PDF file to analyze
            output_dir: Directory to save outputs (defaults to ./report/{timestamp})

        Returns:
            dict: Final output with PDF path and summary
        """
        # Start timing
        start_time = time.time()

        # Validate inputs
        pdf_path = Path(pdf_path)
        if not pdf_path.exists():
            raise FileNotFoundError(f"PDF file not found: {pdf_path}")

        # Setup output directory
        if output_dir is None:
            timestamp = int(datetime.now().timestamp())
            output_dir = Path(__file__).parent / "report" / str(timestamp)
        else:
            output_dir = Path(output_dir)

        output_dir.mkdir(parents=True, exist_ok=True)

        print("=" * 80)
        print("[SingleAgent] PDF VALIDATION REPORT GENERATOR")
        print("=" * 80)
        print(f"[SingleAgent] PDF Input: {pdf_path}")
        print(f"[SingleAgent] Output directory: {output_dir}")
        print(f"[SingleAgent] Start time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print()

        # Extract text from PDF
        extraction_start = time.time()
        pdf_text = self.extract_text_from_pdf(pdf_path, output_dir)
        extraction_time = time.time() - extraction_start
        print(f"[SingleAgent] PDF extraction took: {extraction_time:.2f} seconds")
        print()

        # Prepare messages
        messages = [
            {
                "role": "system",
                "content": self.system_prompt
            },
            {
                "role": "user",
                "content": f"""Please analyze this PDF report and generate a complete validation report.

Input:
- filePath: {pdf_path.absolute()}

PDF Content:
{pdf_text}

Follow all phases in the system prompt to:
1. Extract table of contents and sections
2. Classify and extract content (requirements, use cases, architecture, tests)
3. Consolidate extracted data
4. Perform traceability analysis
5. Extract and validate features
6. Generate LaTeX PDF report

## LaTeX Template

You MUST use the following LaTeX template as a reference for generating the final report.
Follow the same structure, styling, colors, and formatting conventions.
Adapt the content to match the analyzed PDF while maintaining this professional format.

```latex
{self.latex_template}
```

Save all intermediate outputs as JSON files and the final LaTeX report.
Return the final JSON output with the PDF path and summary statistics."""
            }
        ]

        # Execute the prompt
        print("[SingleAgent] Sending request to OpenAI API...")
        print("[SingleAgent] This may take several minutes for complex PDFs...")
        print()

        api_start_time = time.time()

        try:
            # Different parameters for different model families
            if self.is_o1_model:
                # o1 models don't support system messages or temperature
                # Combine system prompt with user message (template already included in messages[1])
                combined_message = f"{self.system_prompt}\n\n---\n\n{messages[1]['content']}"

                response = self.client.chat.completions.create(
                    model=self.model,
                    messages=[
                        {
                            "role": "user",
                            "content": combined_message
                        }
                    ],
                    max_completion_tokens=32000  # Increased for template
                )
            elif self.is_gpt5_model:
                # GPT-5 models use max_completion_tokens and may use reasoning tokens
                # Need MORE tokens because GPT-5 uses internal reasoning (like o1)
                response = self.client.chat.completions.create(
                    model=self.model,
                    messages=messages,
                    max_completion_tokens=64000,  # Much higher for reasoning + output
                    temperature=0.0  # Deterministic output
                )
            else:
                # GPT-4 and older models use max_tokens
                response = self.client.chat.completions.create(
                    model=self.model,
                    messages=messages,
                    max_tokens=16000,
                    temperature=0.0  # Deterministic output
                )

            # Extract response text
            response_text = response.choices[0].message.content

            # Debug: check if response is None or empty
            if not response_text:
                print("[SingleAgent] WARNING: Response text is empty or None!")
                print(f"[SingleAgent] Response object: {response}")
                print(f"[SingleAgent] Message: {response.choices[0].message}")
                # Try to get content in different ways
                if hasattr(response.choices[0].message, 'content'):
                    print(f"[SingleAgent] Content attribute exists but is: {repr(response.choices[0].message.content)}")
                # Check if there's a different field
                print(f"[SingleAgent] Available attributes: {dir(response.choices[0].message)}")

            api_time = time.time() - api_start_time

            print("[SingleAgent] Response received!")
            print()
            print("=" * 80)
            print("[SingleAgent] TOKEN USAGE STATISTICS")
            print("=" * 80)
            print(f"Input tokens:      {response.usage.prompt_tokens:,}")
            print(f"Output tokens:     {response.usage.completion_tokens:,}")
            print(f"Total tokens:      {response.usage.total_tokens:,}")
            print(f"API call time:     {api_time:.2f} seconds ({api_time/60:.2f} minutes)")

            # Calculate costs (based on GPT-4 Turbo pricing - adjust for GPT-5.1 when available)
            # These are estimates - actual pricing may vary
            cost_per_1k_input = 0.01  # $0.01 per 1K input tokens (GPT-4 Turbo)
            cost_per_1k_output = 0.03  # $0.03 per 1K output tokens (GPT-4 Turbo)

            # Adjust pricing for known models
            if "gpt-4" in self.model.lower():
                cost_per_1k_input = 0.01
                cost_per_1k_output = 0.03
            elif "o1-preview" in self.model.lower():
                cost_per_1k_input = 0.015
                cost_per_1k_output = 0.06
            elif "o1-mini" in self.model.lower():
                cost_per_1k_input = 0.003
                cost_per_1k_output = 0.012
            elif "gpt-5" in self.model.lower():
                # Placeholder - update when GPT-5 pricing is known
                cost_per_1k_input = 0.015  # Estimate
                cost_per_1k_output = 0.045  # Estimate
                print(f"Note: GPT-5 pricing is estimated")

            input_cost = (response.usage.prompt_tokens / 1000) * cost_per_1k_input
            output_cost = (response.usage.completion_tokens / 1000) * cost_per_1k_output
            total_cost = input_cost + output_cost

            print(f"\nEstimated cost:")
            print(f"  Input:           ${input_cost:.4f}")
            print(f"  Output:          ${output_cost:.4f}")
            print(f"  Total:           ${total_cost:.4f}")
            print("=" * 80)
            print()

            # Save raw response
            raw_output_path = output_dir / "raw_response.txt"
            with open(raw_output_path, 'w', encoding='utf-8') as f:
                f.write(response_text)
            print(f"[SingleAgent] Raw response saved to: {raw_output_path}")

            # Try to extract JSON outputs from the response
            self._extract_and_save_outputs(response_text, output_dir)

            # Try to extract and compile LaTeX
            latex_start = time.time()
            latex_path = self._extract_and_compile_latex(response_text, output_dir)
            latex_time = time.time() - latex_start

            # Calculate total execution time
            total_time = time.time() - start_time

            # Build final summary
            result = {
                "success": True,
                "pdf_input": str(pdf_path.absolute()),
                "output_directory": str(output_dir),
                "raw_response_path": str(raw_output_path),
                "latex_path": latex_path,
                "execution_stats": {
                    "start_time": datetime.fromtimestamp(start_time).strftime('%Y-%m-%d %H:%M:%S'),
                    "end_time": datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
                    "total_time_seconds": round(total_time, 2),
                    "total_time_minutes": round(total_time / 60, 2),
                    "pdf_extraction_time_seconds": round(extraction_time, 2),
                    "api_call_time_seconds": round(api_time, 2),
                    "latex_compilation_time_seconds": round(latex_time, 2)
                },
                "token_stats": {
                    "model": self.model,
                    "input_tokens": response.usage.prompt_tokens,
                    "output_tokens": response.usage.completion_tokens,
                    "total_tokens": response.usage.total_tokens,
                    "estimated_cost": {
                        "input_cost_usd": round(input_cost, 4),
                        "output_cost_usd": round(output_cost, 4),
                        "total_cost_usd": round(total_cost, 4),
                        "cost_per_1k_input": cost_per_1k_input,
                        "cost_per_1k_output": cost_per_1k_output
                    }
                }
            }

            # Save final result
            result_path = output_dir / "execution_result.json"
            with open(result_path, 'w', encoding='utf-8') as f:
                json.dump(result, f, indent=2)

            print()
            print("=" * 80)
            print("[SingleAgent] EXECUTION COMPLETED SUCCESSFULLY!")
            print("=" * 80)
            print()
            print("📁 OUTPUT LOCATIONS:")
            print(f"   Output directory:    {output_dir}")
            print(f"   Raw response:        {raw_output_path}")
            if latex_path:
                print(f"   📄 PDF Report:       {latex_path}")
            print(f"   Result summary:      {result_path}")
            print()
            print("⏱️  EXECUTION TIME:")
            print(f"   Total time:          {total_time:.2f} seconds ({total_time/60:.2f} minutes)")
            print(f"   - PDF extraction:    {extraction_time:.2f}s")
            print(f"   - API call:          {api_time:.2f}s ({api_time/60:.2f} min)")
            print(f"   - LaTeX compilation: {latex_time:.2f}s")
            print()
            print("💰 COST SUMMARY:")
            print(f"   Model:               {self.model}")
            print(f"   Input tokens:        {response.usage.prompt_tokens:,}")
            print(f"   Output tokens:       {response.usage.completion_tokens:,}")
            print()
            print("=" * 80)
            print()

            return result

        except Exception as e:
            print(f"[SingleAgent] ERROR: {e}")
            raise

    def _extract_and_save_outputs(self, response_text, output_dir):
        """
        Extract JSON code blocks from response and save them

        Args:
            response_text: Full response text
            output_dir: Directory to save outputs
        """
        print("[SingleAgent] Extracting JSON outputs...")

        # Simple extraction of JSON code blocks
        import re
        json_blocks = re.findall(r'```json\s*\n(.*?)\n```', response_text, re.DOTALL)

        for i, json_str in enumerate(json_blocks):
            try:
                json_data = json.loads(json_str)

                # Try to identify the phase by content
                filename = f"output_{i+1}.json"
                if "tableOfContents" in json_data:
                    filename = "phase1_toc_extraction.json"
                elif "classifiedSections" in json_data:
                    filename = "phase2_section_classification.json"
                elif "consolidatedData" in json_data:
                    filename = "phase3_consolidation.json"
                elif "traceabilityMatrix" in json_data:
                    filename = "phase4_traceability.json"
                elif "featureValidation" in json_data:
                    filename = "phase5_feature_validation.json"
                elif "pdfPath" in json_data and "summary" in json_data:
                    filename = "phase6_final_output.json"

                output_path = output_dir / filename
                with open(output_path, 'w', encoding='utf-8') as f:
                    json.dump(json_data, f, indent=2)

                print(f"  ✓ Saved: {filename}")
            except json.JSONDecodeError:
                # Not valid JSON, skip
                pass

    def _extract_and_compile_latex(self, response_text, output_dir):
        """
        Extract LaTeX code from response, save it, and compile to PDF

        Args:
            response_text: Full response text
            output_dir: Directory to save outputs

        Returns:
            str: Path to compiled PDF, or None if compilation failed
        """
        print("[SingleAgent] Extracting LaTeX report...")

        import re

        # Extract LaTeX document (look for \documentclass ... \end{document})
        latex_pattern = r'```latex\s*\n(\\documentclass.*?\\end\{document\})\s*\n```'
        latex_match = re.search(latex_pattern, response_text, re.DOTALL)

        if not latex_match:
            # Try without code block markers
            latex_pattern = r'(\\documentclass.*?\\end\{document\})'
            latex_match = re.search(latex_pattern, response_text, re.DOTALL)

        if not latex_match:
            print("  ✗ No LaTeX document found in response")
            return None

        latex_content = latex_match.group(1)

        # Save LaTeX file
        tex_path = output_dir / "report.tex"
        with open(tex_path, 'w', encoding='utf-8') as f:
            f.write(latex_content)

        print(f"  ✓ LaTeX saved to: {tex_path}")

        # Try to compile LaTeX to PDF
        print("[SingleAgent] Compiling LaTeX to PDF...")
        try:
            # Run pdflatex twice (for ToC and references)
            for run in [1, 2]:
                result = subprocess.run(
                    ['pdflatex', '-interaction=nonstopmode', 'report.tex'],
                    cwd=output_dir,
                    capture_output=True,
                    text=True,
                    timeout=120
                )

                if result.returncode != 0:
                    print(f"  ✗ LaTeX compilation failed (run {run})")
                    # Save error log
                    error_log_path = output_dir / "latex_error.log"
                    with open(error_log_path, 'w') as f:
                        f.write(result.stdout)
                        f.write("\n\n")
                        f.write(result.stderr)
                    print(f"  Error log saved to: {error_log_path}")
                    return None

            pdf_path = output_dir / "report.pdf"
            if pdf_path.exists():
                print(f"  ✓ PDF compiled successfully: {pdf_path}")
                return str(pdf_path)
            else:
                print("  ✗ PDF file not created")
                return None

        except FileNotFoundError:
            print("  ✗ pdflatex not found. Install TeX Live or MiKTeX to compile LaTeX.")
            return None
        except subprocess.TimeoutExpired:
            print("  ✗ LaTeX compilation timed out")
            return None


def main():
    """Main entry point"""
    import argparse

    parser = argparse.ArgumentParser(
        description="Execute single-agent prompt for PDF validation (OpenAI)"
    )
    parser.add_argument(
        "pdf_path",
        help="Path to PDF file to analyze"
    )
    parser.add_argument(
        "--output-dir",
        help="Output directory (default: ./report/{timestamp})"
    )
    parser.add_argument(
        "--api-key",
        help="OpenAI API key (default: OPENAI_API_KEY env var)"
    )
    parser.add_argument(
        "--model",
        default="gpt-4o-mini",
        help="OpenAI model to use (default: gpt-4-turbo). Examples: gpt-4-turbo, gpt-5.1, o1-preview, o1-mini"
    )

    args = parser.parse_args()

    try:
        executor = SingleAgentExecutorOpenAI(api_key=args.api_key, model=args.model)
        result = executor.execute(args.pdf_path, args.output_dir)

        print("Execution completed successfully!")
        print(f"Results saved to: {result['output_directory']}")

        return 0

    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    sys.exit(main())
