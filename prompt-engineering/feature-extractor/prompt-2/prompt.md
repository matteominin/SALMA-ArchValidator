## Prompt

You are an expert software architecture auditor.

You will receive the text of ONE section or subsection from a software engineering report.

**Your task:**

1. Identify all good practices, architectural decisions, or guidelines described in the section.
2. Rewrite each practice as a GENERALIZED statement that could apply to many domains, avoiding:
    - specific product names
    - company names
    - brand names
    - project-specific terminology
    - domain-specific nouns (replace with generic terms like "system", "platform", "application", "device")
3. Keep each feature concise (max 25 words), phrased as a reusable best practice.

For each practice found, return:
  - feature: the generalized good practice
  - category: one of ["Architecture", "Design", "Coding Standards", "Testing", "Security", "Performance", "Documentation", "Process", "Other"]
  - explicit: true if the text explicitly states this practice, false if inferred
  - evidence: short direct quote from the text showing the practice (keep domain-specific terms here)
  - confidence: number between 0 and 1 estimating how confident you are the practice is actually present
  - source_title: the section or subsection title (given in the input)

**Output must be valid JSON in this exact format:**

```json
[
  {
    "feature": "...",
    "category": "...",
    "explicit": true,
    "evidence": "...",
    "confidence": 0.95,
    "source_title": "..."
  }
]
```

Now process the following section:

```
Section title: {section_title}

Section text:
{section_text}
```

---

## Issues

Still too domain specific, the extraction should focus on macro aspects, not implementation details bound to the specific application

{
  "feature": "Integrate mobile payment methods with secure authentication to enable cashless transactions.",
  "category": "Architecture",
  "explicit": false,
  "evidence": "Grazie all’applicazione mobile e a un wallet digitale associato...",
  "confidence": 0.9,
  "source_title": "1 Introduzione"
}