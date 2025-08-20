## Prompt

You are an expert software architecture auditor.

You will receive the text of **one section or subsection** from a software engineering report.

**Your task is to:**
1. Identify whether the section contains information that addresses standard *expected elements* of good software architecture documentation.
2. Abstract away all domain-specific details — focus on *what type of information* is provided or should be provided, not on the specific technology, brand, or domain.
3. If an expected element is present, summarize it as a general requirement/practice (e.g., "The introduction should clearly state the problem to be solved").
4. If an expected element is missing, still list it as a general requirement but mark `explicit: false` and set a lower `confidence` value.
5. Keep statements high-level so they apply to any software project documentation.

For each element found (or missing but relevant), return:
  - feature: the generalized expected element or practice
  - category: one of ["Problem Definition", "System Overview", "Architecture", "Design", "Requirements", "Interfaces", "Testing", "Security", "Performance", "Documentation", "Process", "Other"]
  - explicit: true if the section explicitly provides this element, false if it's absent or only implied
  - evidence: short direct quote from the text if present, or "" if absent
  - confidence: number between 0 and 1 of your certainty
  - source_title: the section or subsection title (given in the input)

**Output in strict JSON:**

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

- The extraction sometimes produces features that are too generic or based on very short, out-of-context phrases.
- Features should be meaningful, well-supported by the text, and avoid using isolated or ambiguous words as evidence.
- Example of a weak extraction:

```json
{
  "feature": "The document should mention the security aspects of the system.",
  "category": "Security",
  "explicit": true,
  "evidence": "semplice e sicura",
  "confidence": 0.9,
  "source_title": "1 Introduzione"
}
```