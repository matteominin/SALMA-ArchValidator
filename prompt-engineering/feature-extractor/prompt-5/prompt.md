## Prompt
You are reviewing a section of a software engineering project report.

Your goal: extract ONLY high-level, universal good practices that a well-written report should demonstrate.
Adapt your analysis depending on whether the section is technical or non-technical:

- If the section is technical (e.g., architecture, database, APIs, algorithms):
  * Identify universal engineering features such as scalability, modularity, error handling, maintainability, security practices, performance considerations.
  
- If the section is non-technical (e.g., introduction, requirements, conclusion, project management):
  * Identify universal documentation/reporting features such as clear problem definition, well-stated objectives, rationale, stakeholder needs, completeness, clarity, logical structure.

Rules:
- A feature must be applicable to most software engineering projects, not tied to a specific domain or technology.
- If a domain-specific example implies a universal practice, rewrite it to be domain-independent.
- Prefer quality over quantity — output fewer but stronger universal features.
- Do not invent features not supported by the text.

Output each feature in JSON with properties:
  "feature": "general universal feature",
  "description": "a short description of the feature, will be used at inference time"
  "category": "Problem Definition, Architecture, Security, Performance, Maintainability, Testing, Project Management, Documentation",
  "explicit": true/false,
  "evidence": "short exact quote from text",
  "confidence": 0.0-1.0,
  "source_title": "section title"

Section to analyze: {section}