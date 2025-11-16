# Correzioni per il Workflow "PDF Complete Validation Report"

**Workflow ID**: `f1f6881d-2398-4c47-88b1-7f65a590ca26`

## Nota sui Binding Automatici

I binding non sono necessari quando l'output port e l'input port hanno **lo stesso nome** e schema compatibile. Il sistema li mappa automaticamente.

**Verificato - Questi funzionano automaticamente** (stesso nome di porta):
- ✅ ReqToUc → Report Generator: `req_to_uc` → `req_to_uc`
- ✅ UcToArc → Report Generator: `uc_to_arc` → `uc_to_arc`
- ✅ UcToTest → Report Generator: `uc_to_test` → `uc_to_test`
- ✅ Data Transformer → Report Generator: `report` → `data` (binding esplicito già presente)
- ✅ Traceability report → Report Generator: `report` → `map` (binding esplicito già presente)
- ✅ Feature Validator → Report Generator: `report` → `validation_report` (binding esplicito già presente)

## Errori Trovati e Correzioni (Solo 2 Errori Critici)

### ❌ ERRORE 1: Connessione Feature Validator → Compliance Agent (Nodo 11 → Nodo 12)

**Edge ID**: `1b1668f0-b783-4f91-9627-18e7d45f8fb3`

**Stato Attuale**:
```json
{
  "_id": "1b1668f0-b783-4f91-9627-18e7d45f8fb3",
  "sourceNodeId": "11",
  "targetNodeId": "12",
  "bindings": {}
}
```

**❌ Problema**: Il Compliance Agent (sub-workflow) richiede in input `coveredFeatures`, ma non c'è nessun binding!

**✅ Correzione**:
```json
{
  "_id": "1b1668f0-b783-4f91-9627-18e7d45f8fb3",
  "sourceNodeId": "11",
  "targetNodeId": "12",
  "bindings": {
    "report$coverage$coveredFeatures": "coveredFeatures"
  }
}
```

**Comando MongoDB**:
```javascript
db.meta_workflows.updateOne(
  {
    "_id": "f1f6881d-2398-4c47-88b1-7f65a590ca26",
    "edges._id": "1b1668f0-b783-4f91-9627-18e7d45f8fb3"
  },
  {
    $set: {
      "edges.$.bindings": {
        "report$coverage$coveredFeatures": "coveredFeatures"
      }
    }
  }
)
```

---

### ❌ ERRORE 2: Connessione Compliance Agent → Report Generator (Nodo 12 → Nodo 13)

**Edge ID**: `366b3b81-6182-4389-9212-d796956d8d96`

**Stato Attuale**:
```json
{
  "_id": "366b3b81-6182-4389-9212-d796956d8d96",
  "sourceNodeId": "12",
  "targetNodeId": "13",
  "bindings": {}
}
```

**❌ Problema**: Il Compliance Agent produce un output (array di compliance checks), ma non viene passato al Report Generator!

**Analisi dell'Output del Compliance Agent**:
Il Compliance Agent è un sub-workflow che ha un loop (FOR_EACH) con `outputKey: "out"`. L'output finale è un array di oggetti `compliance` dal nodo "Checklist verifier".

**✅ Correzione**:
```json
{
  "_id": "366b3b81-6182-4389-9212-d796956d8d96",
  "sourceNodeId": "12",
  "targetNodeId": "13",
  "bindings": {
    "out": "compliance_report"
  }
}
```

**Comando MongoDB**:
```javascript
db.meta_workflows.updateOne(
  {
    "_id": "f1f6881d-2398-4c47-88b1-7f65a590ca26",
    "edges._id": "366b3b81-6182-4389-9212-d796956d8d96"
  },
  {
    $set: {
      "edges.$.bindings": {
        "out": "compliance_report"
      }
    }
  }
)
```

**NOTA IMPORTANTE**: Devi anche **aggiungere il parametro `compliance_report` agli input del Report Generator** nel nodo MongoDB!

---

## ⚠️ MODIFICA NECESSARIA AL NODO REPORT GENERATOR

Devi aggiungere un nuovo input port al Report Generator per ricevere il `compliance_report`:

```json
{
  "role": "REQ_BODY_FIELD",
  "key": "compliance_report",
  "schema": {
    "type": "ARRAY",
    "items": {
      "type": "OBJECT",
      "properties": {
        "feature": {
          "type": "STRING"
        },
        "description": {
          "type": "STRING"
        },
        "checklist": {
          "type": "ARRAY",
          "items": {
            "type": "OBJECT",
            "properties": {
              "check": {
                "type": "STRING"
              },
              "satisfied": {
                "type": "STRING"
              },
              "explaination": {
                "type": "STRING"
              }
            }
          }
        }
      }
    }
  },
  "portType": "REST"
}
```

**Comando MongoDB per aggiungere l'input port**:
```javascript
db.meta_nodes.updateOne(
  { "_id": "cb3895fc-9f6d-435c-b781-cdec4cab2ad9" },
  {
    $push: {
      "inputPorts": {
        "role": "REQ_BODY_FIELD",
        "key": "compliance_report",
        "schema": {
          "type": "ARRAY",
          "items": {
            "type": "OBJECT",
            "properties": {
              "feature": { "type": "STRING" },
              "description": { "type": "STRING" },
              "checklist": {
                "type": "ARRAY",
                "items": {
                  "type": "OBJECT",
                  "properties": {
                    "check": { "type": "STRING" },
                    "satisfied": { "type": "STRING" },
                    "explaination": { "type": "STRING" }
                  }
                }
              }
            }
          }
        },
        "portType": "REST"
      }
    }
  }
)
```

---

## Script Completo per Applicare Tutte le Correzioni

```javascript
// 1. Correggi connessione Feature Validator → Compliance Agent
db.meta_workflows.updateOne(
  {
    "_id": "f1f6881d-2398-4c47-88b1-7f65a590ca26",
    "edges._id": "1b1668f0-b783-4f91-9627-18e7d45f8fb3"
  },
  {
    $set: {
      "edges.$.bindings": {
        "report$coverage$coveredFeatures": "coveredFeatures"
      }
    }
  }
);

// 2. Correggi connessione Compliance Agent → Report Generator
db.meta_workflows.updateOne(
  {
    "_id": "f1f6881d-2398-4c47-88b1-7f65a590ca26",
    "edges._id": "366b3b81-6182-4389-9212-d796956d8d96"
  },
  {
    $set: {
      "edges.$.bindings": {
        "out": "compliance_report"
      }
    }
  }
);

// 3. Aggiungi input port compliance_report al Report Generator
db.meta_nodes.updateOne(
  { "_id": "cb3895fc-9f6d-435c-b781-cdec4cab2ad9" },
  {
    $push: {
      "inputPorts": {
        "role": "REQ_BODY_FIELD",
        "key": "compliance_report",
        "schema": {
          "type": "ARRAY",
          "items": {
            "type": "OBJECT",
            "properties": {
              "feature": { "type": "STRING" },
              "description": { "type": "STRING" },
              "checklist": {
                "type": "ARRAY",
                "items": {
                  "type": "OBJECT",
                  "properties": {
                    "check": { "type": "STRING" },
                    "satisfied": { "type": "STRING" },
                    "explaination": { "type": "STRING" }
                  }
                }
              }
            }
          }
        },
        "portType": "REST"
      }
    }
  }
);
```

---

## Riepilogo Input del Report Generator Dopo le Correzioni

Dopo aver applicato tutte le correzioni, il Report Generator riceverà:

1. ✅ `data` (dal Data Transformer - nodo 3) - binding esplicito già presente
2. ✅ `req_to_uc` (da ReqToUc - nodo 4) - **mapping automatico** (stesso nome porta)
3. ✅ `uc_to_arc` (da UcToArc - nodo 6) - **mapping automatico** (stesso nome porta)
4. ✅ `uc_to_test` (da UcToTest - nodo 7) - **mapping automatico** (stesso nome porta)
5. ✅ `map` (da Traceability report - nodo 8) - binding esplicito già presente
6. ✅ `validation_report` (da Feature validator - nodo 11) - binding esplicito già presente
7. ✅ `compliance_report` (da Compliance Agent - nodo 12) - **NUOVO** (da aggiungere)

---

## Prossimi Passi

1. Esegui lo script MongoDB completo sopra
2. Verifica che tutte le modifiche siano applicate correttamente
3. Aggiorna anche il codice Java del PdfReport.java per includere `compliance_report`
4. Testa il workflow completo
