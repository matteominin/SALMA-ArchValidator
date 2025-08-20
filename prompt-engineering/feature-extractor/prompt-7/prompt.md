## Prompt

You are reviewing a section of a software engineering project report.  
Your task: **extract only high-level, universal good practices that any well-written software engineering report should demonstrate.**

## Rules for Analysis
- **Technical sections** (architecture, database, APIs, algorithms): identify universal engineering qualities.  
- **Non-technical sections** (introduction, requirements, conclusion, project management): identify universal reporting qualities.  
- Always generalize to universal practices, avoiding domain-specific references.  
- If a domain-specific example implies a universal principle, rewrite it in a domain-neutral way.  
- Prefer **fewer but stronger features**.  
- Do not invent features not supported by the text.  

## Output
Each extracted feature must contain the following fields:

  "feature": "general universal feature",
  "description": "short description of the feature, will be used at inference time",
  "category": "Problem Definition | Architecture | Security | Performance | Maintainability | Testing | Project Management | Documentation",
  "explicit": true/false,
  "evidence": "short exact quote from text",
  "confidence": 0.0-1.0,
  "source_title": "section title",
  "section_text": "long excerpt that includes the essential part plus additional surrounding details, kept in natural multi-line form, to provide enough context for thorough validation of the feature.",
  "file": "filepath"

## Input
Section to analyze: {section}
filePath: {filePath}