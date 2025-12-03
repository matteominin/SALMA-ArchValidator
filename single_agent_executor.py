#!/usr/bin/env python3
"""
Single-Agent PDF Validation Report Executor

This script executes the single-agent prompt to analyze a PDF and generate
a comprehensive validation report.
"""

import os
import sys
import json
import base64
import subprocess
from pathlib import Path
from datetime import datetime
from anthropic import Anthropic

class SingleAgentExecutor:
    """Executes the single-agent prompt for PDF validation"""

    def __init__(self, api_key=None):
        """
        Initialize the executor

        Args:
            api_key: Anthropic API key (defaults to ANTHROPIC_API_KEY env var)
        """
        self.api_key = api_key or os.environ.get("ANTHROPIC_API_KEY")
        if not self.api_key:
            raise ValueError("ANTHROPIC_API_KEY environment variable not set")

        self.client = Anthropic(api_key=self.api_key)
        self.model = "claude-3-5-sonnet-20241022"  # Latest Claude model with PDF support

        # Load the single-agent prompt
        prompt_path = Path(__file__).parent / "single-agent.md"
        with open(prompt_path, 'r', encoding='utf-8') as f:
            self.system_prompt = f.read()

        # Load the LaTeX template
        template_path = Path(__file__).parent / "report" / "sample.tex"
        with open(template_path, 'r', encoding='utf-8') as f:
            self.latex_template = f.read()

    def read_pdf_as_base64(self, pdf_path):
        """
        Read PDF file and encode as base64

        Args:
            pdf_path: Path to PDF file

        Returns:
            Base64 encoded PDF content
        """
        with open(pdf_path, 'rb') as f:
            pdf_bytes = f.read()
        return base64.standard_b64encode(pdf_bytes).decode('utf-8')

    def execute(self, pdf_path, output_dir=None):
        """
        Execute the single-agent prompt on a PDF

        Args:
            pdf_path: Path to the PDF file to analyze
            output_dir: Directory to save outputs (defaults to ./report/{timestamp})

        Returns:
            dict: Final output with PDF path and summary
        """
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

        print(f"[SingleAgent] Starting analysis of: {pdf_path}")
        print(f"[SingleAgent] Output directory: {output_dir}")
        print(f"[SingleAgent] Model: {self.model}")
        print(f"[SingleAgent] LaTeX template loaded: {len(self.latex_template)} characters")
        print()

        # Read PDF as base64
        print("[SingleAgent] Reading PDF file...")
        pdf_base64 = self.read_pdf_as_base64(pdf_path)
        print(f"[SingleAgent] PDF size: {len(pdf_base64) / 1024:.2f} KB (base64)")
        print()

        # Prepare the user message with PDF document
        user_message = {
            "role": "user",
            "content": [
                {
                    "type": "document",
                    "source": {
                        "type": "base64",
                        "media_type": "application/pdf",
                        "data": pdf_base64
                    }
                },
                {
                    "type": "text",
                    "text": f"""Please analyze this PDF report and generate a complete validation report.

Input:
- filePath: {pdf_path.absolute()}

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
        }

        # Execute the prompt
        print("[SingleAgent] Sending request to Claude API...")
        print("[SingleAgent] This may take several minutes for complex PDFs...")
        print()

        try:
            response = self.client.messages.create(
                model=self.model,
                max_tokens=16000,  # Large output for comprehensive report
                system=self.system_prompt,
                messages=[user_message],
                temperature=0.0  # Deterministic output for analysis
            )

            # Extract response text
            response_text = response.content[0].text

            print("[SingleAgent] Response received!")
            print(f"[SingleAgent] Input tokens: {response.usage.input_tokens}")
            print(f"[SingleAgent] Output tokens: {response.usage.output_tokens}")
            print()

            # Save raw response
            raw_output_path = output_dir / "raw_response.txt"
            with open(raw_output_path, 'w', encoding='utf-8') as f:
                f.write(response_text)
            print(f"[SingleAgent] Raw response saved to: {raw_output_path}")

            # Try to extract JSON outputs from the response
            self._extract_and_save_outputs(response_text, output_dir)

            # Try to extract and compile LaTeX
            latex_path = self._extract_and_compile_latex(response_text, output_dir)

            # Build final summary
            result = {
                "success": True,
                "pdf_input": str(pdf_path.absolute()),
                "output_directory": str(output_dir),
                "raw_response_path": str(raw_output_path),
                "latex_path": latex_path,
                "response_stats": {
                    "input_tokens": response.usage.input_tokens,
                    "output_tokens": response.usage.output_tokens,
                    "model": self.model
                }
            }

            # Save final result
            result_path = output_dir / "execution_result.json"
            with open(result_path, 'w', encoding='utf-8') as f:
                json.dump(result, f, indent=2)

            print()
            print("=" * 80)
            print("[SingleAgent] Execution completed successfully!")
            print("=" * 80)
            print(f"Output directory: {output_dir}")
            if latex_path:
                print(f"LaTeX report: {latex_path}")
            print(f"Result summary: {result_path}")
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
        description="Execute single-agent prompt for PDF validation"
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
        help="Anthropic API key (default: ANTHROPIC_API_KEY env var)"
    )

    args = parser.parse_args()

    try:
        executor = SingleAgentExecutor(api_key=args.api_key)
        result = executor.execute(args.pdf_path, args.output_dir)

        print("Execution completed successfully!")
        print(f"Results saved to: {result['output_directory']}")

        return 0

    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
