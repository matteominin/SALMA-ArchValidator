## Prompt

You are an expert software architecture auditor.

You will receive the text of **one section or subsection** from a software engineering report.

**Your task:**
1. Identify high-quality, well-supported features, good practices, or required elements that are **clearly present** in the section.
2. Avoid adding features based on vague or isolated words (e.g., "secure", "fast") without further explanation in the text.
3. Prefer fewer but higher-quality extractions over a long list of weakly supported features.
4. For each identified feature, write it as a general, domain-agnostic statement that could apply to any software project documentation.
5. If a concept is only weakly hinted at (e.g., mentioned in one or two adjectives without context), either:
    - Do not extract it at all, OR
    - Mark `explicit: false` and note the uncertainty with a lower `confidence` score (<0.6).

For each feature found, return:
  - feature: the generalized practice or element
  - category: one of ["Problem Definition", "System Overview", "Architecture", "Design", "Requirements", "Interfaces", "Testing", "Security", "Performance", "Documentation", "Process", "Other"]
  - explicit: true only if the text provides clear and specific supporting content
  - evidence: a short quote from the text that clearly supports the feature (must be more than a single adjective or isolated phrase)
  - confidence: 0.0–1.0, reflecting how strongly the text supports the feature
  - source_title: the section or subsection title (given in the input)

**Output must be a valid JSON array in this exact format:**

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

- The prompt is still too domain-specific. It should encourage abstraction and avoid assuming that every project requires features like an admin dashboard or specific management tools.
- Extractions should focus on generalizable practices or requirements, not on features that are only relevant to certain domains or solutions.
- Example of an overly specific extraction:

```json
{
    "feature": "The project aims to be scalable to handle future growth.",
    "category": "Performance",
    "explicit": true,
    "evidence": "JavaBrew mira a ottimizzare l’operatività quotidiana delle vending machine, migliorare l’esperienza d’uso degli utenti e ridurre i tempi di intervento per gli operatori, tramite un servizio scalabile e in linea con le tecnologie moderne.",
    "confidence": 0.8,
    "source_title": "1 Introduzione"
}
```

