You are reviewing a section of a software engineering project report.

Your goal: extract ONLY high-level, universal good practices that a well-written report should demonstrate.

Adapt your analysis depending on whether the section is technical or non-technical:

If the section is technical (e.g., architecture, database, APIs, algorithms):
Identify universal engineering features such as scalability, modularity, error handling, maintainability, security practices, performance considerations.
If the section is non-technical (e.g., introduction, requirements, conclusion, project management):
Identify universal documentation/reporting features such as clear problem definition, well-stated objectives, rationale, stakeholder needs, completeness, clarity, logical structure.
Additional Instructions:
Always generalize the feature so it applies to any software engineering project, not just the one described in the text.
Avoid domain-specific references (e.g., “vending machines”, “QR codes”, “wallets”) unless rephrased into a universal concept (e.g., “digital payment methods” → “secure transaction mechanisms”).
If a domain-specific example implies a universal practice, rewrite it in a domain-neutral way.
Prefer quality over quantity — output fewer but stronger universal features.
Do not invent features not supported by the text.
If no valid universal features are found in the section, output a JSON object with only the source_title and all the other fileds empty.
Output format (JSON array):
Each feature must have:

  "feature": "general universal feature",
  "description": "short description of the feature, will be used at inference time",
  "category": "Problem Definition, Architecture, Security, Performance, Maintainability, Testing, Project Management, Documentation",
  "explicit": true/false,
  "evidence": "short exact quote from text",
  "confidence": 0.0-1.0,
  "source_title": "section title"

If no feature is found for a section:

  "feature": "",
  "description": "",
  "category": "",
  "explicit": false,
  "evidence": "",
  "confidence": 0.0,
  "source_title": "section title"

Section to analyze: {section}