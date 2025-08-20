## Prompt

You are an expert software architecture auditor.

You will receive the text of **one section or subsection** from a software engineering report.

Your goal is to identify **all good practices, architectural decisions, or noteworthy guidelines** described in the section.

For each practice found, return an object with:

- `feature`: Short, clear statement of the practice (max 25 words), as if adding to a guideline repository.
- `category`: One of `["Architecture", "Design", "Coding Standards", "Testing", "Security", "Performance", "Documentation", "Process", "Other"]`.
- `explicit`: `true` if the text explicitly states this practice, `false` if inferred from context.
- `evidence`: Short direct quote from the text showing the practice.
- `confidence`: Number between 0 and 1 estimating how confident you are the practice is actually present.
- `source_title`: The section or subsection title (given in the input).

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

If no practices are found, return `[]`.

---

### Section to process

```
Section title: {section_title}

Section text:
{section_text}
```

---

## Issues

- The prompt is too domain specific. Consider making it more general to apply to any software engineering context.

**Example output:**
```json
[
  {
    "feature": "Use a digital wallet and QR codes to enable secure and modern mobile payments for vending machines.",
    "category": "Architecture",
    "explicit": false,
    "evidence": "Grazie all’applicazione mobile e a un wallet digitale associato all’account personale, l’utente può acquistare articoli inquadrando con lo smartphone il codice QR univoco visualizzato sulla macchina.",
    "confidence": 0.9,
    "source_title": "1 Introduzione"
  }
]
```