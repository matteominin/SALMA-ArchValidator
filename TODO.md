## Dettagli dei Componenti

Per il requisito **Partially Satisfied (REQ-5)**, l'agente ha identificato i componenti generici "Application" e "Database". Sarebbe preferibile che l'agente specificasse i componenti rilevanti descritti nel documento, come **"Componente Logica di Business"** e **"Componente Database"**, per rendere il report più preciso.

## Chiarezza del Linguaggio

Assicurati che le descrizioni nel report (`description`) corrispondano esattamente al documento di origine. Ad esempio, la descrizione di **REQ-1** fornita non corrisponde esattamente al documento `wrong_arch.pdf`, anche se il significato è simile. Una tracciabilità perfetta richiede un'aderenza rigorosa al testo originale.

## Fix title in the requirements extraction
title is not passed correctly

## UC to mockups 
{
    "systemPromptTemplate": "",
    "inputPorts": [
        {
            "role": "SYSTEM_PROMPT_VARIABLE",
            "key": "use_cases",
            "schema": {
                "type": "ARRAY",
                "items": {
                    "type": "OBJECT",
                    "properties": {
                        "case_id": {
                            "type": "STRING"
                        },
                        "name": {
                            "type": "STRING"
                        },
                        "actors": {
                            "type": "ARRAY",
                            "items": {
                                "type": "STRING"
                            }
                        },
                        "main_flow": {
                            "type": "ARRAY",
                            "items": {
                                "type": "STRING"
                            }
                        },
                        "alternative_flows": {
                            "type": "ARRAY",
                            "items": {
                                "type": "STRING"
                            }
                        },
                        "is_explicit": {
                            "type": "BOOLEAN"
                        }
                    }
                }
            },
            "portType": "LLM"
        },
        {
            "role": "SYSTEM_PROMPT_VARIABLE",
            "key": "mockups",
            "schema": {
                "type": "STRING"
            },
            "portType": "LLM"
        }
    ],
    "outputPorts": [
        {
            "role": "RESPONSE",
            "key": "uc_to_mock",
            "schema": {
                "type": "ARRAY",
                "items": {
                    "type": "OBJECT",
                    "properties": {
                        "uc_id": {
                            "type": "STRING"
                        },
                        "mockup_coverage": {
                            "type": "ARRAY",
                            "items": {"type": "STRING"}
                        },
                        "status": {
                            "type": "STRING"
                        },
                        "rationale": {
                            "type": "STRING"
                        }
                    }
                }
            },
            "portType": "LLM"
        }
    ],
    "modelType": "LLM",
    "provider": "openai",
    "modelName": "gpt-5-mini",
    "enabled": true,
    "version": {
        "major": 0,
        "minor": 0,
        "patch": 0
    },
    "name": "UcToMockups",
    "type": "AI",
    "description": "Verifies which mockups satisfy an use case",
    "author": "matteo"
}

## prompt:
You are a UX engineer.
You are given a set of use cases and UI mockups.
Determine which mockups illustrate each use case’s screens or interactions.
For each use case:
Identify mockups corresponding to key steps (login screen, catalog, confirmation, etc.).
If none found, mark "mockup_coverage": [] and "status": "Missing".
Output JSON:
[
  {
    "uc_id": "UC-1",
    "mockup_coverage": ["MOCK-1", "MOCK-2"],
    "status": "Covered",
    "rationale": "MOCK-1 shows login; MOCK-2 shows error state."
  }
]
Inputs:
use_cases: [...JSON...]
mockups: [...JSON...]