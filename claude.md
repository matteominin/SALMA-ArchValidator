# PDF Extractor API - Documentation per LLM

## Informazioni Generali

**Tipo di applicazione:** Spring Boot 3.2.1
**Porta:** 8080
**Base URL:** `http://localhost:8080`
**Database:** MongoDB (localhost:27017)
**AI Service:** OpenAI (text-embedding-3-small per embeddings)

---

## 1. REST API Endpoints

### 1.1 PDF Controller

**Base Path:** `/api/pdf`

#### POST `/api/pdf/extract`
Estrae il testo completo da un file PDF, opzionalmente estraendo anche le immagini.

**Request Body:**
```json
{
  "filepath": "/path/to/document.pdf",
  "outputDirectory": "/path/to/output" // opzionale
}
```

**Response:**
```json
{
  "text": "testo estratto...",
  "outputDirectory": "/path/to/output"
}
```

**Dettagli:**
- Estrae tutto il testo dal PDF
- Se `outputDirectory` è specificato, estrae anche le immagini come file separati
- Inserisce placeholder `[IMAGE GOES HERE]` nel testo dove appaiono le immagini

---

#### POST `/api/pdf/extract-index`
Estrae solo le pagine specificate da un PDF.

**Request Body:**
```json
{
  "filePath": "/path/to/document.pdf",
  "startPage": 1,
  "endPage": 10
}
```

**Response:**
```json
{
  "text": "testo estratto dalle pagine 1-10..."
}
```

**Dettagli:**
- Le pagine sono numerate partendo da 1
- Include sia startPage che endPage

---

#### POST `/api/pdf/extract-sections`
Estrae multiple sezioni basate su un indice del PDF.

**Request Body:**
```json
{
  "filePath": "/path/to/document.pdf",
  "index": [
    {
      "section": "Introduzione",
      "start": 1,
      "end": 5
    },
    {
      "section": "Capitolo 1",
      "start": 6,
      "end": 20
    }
  ]
}
```

**Response:**
```json
[
  {
    "section": "Introduzione",
    "text": "testo della sezione..."
  },
  {
    "section": "Capitolo 1",
    "text": "testo della sezione..."
  }
]
```

---

#### POST `/api/pdf/save-report`
Salva i report di summary e verification in formato markdown.

**Request Body:**
```json
{
  "summary_report": "# Summary Report\n...",
  "verification_report": "# Verification Report\n..."
}
```

**Response:**
```json
{
  "message": "Reports saved successfully"
}
```

**Dettagli:**
- Salva i report come file markdown
- Path di output configurabile nell'applicazione

---

### 1.2 Feature Controller

**Base Path:** `/api/features`

#### POST `/api/features/add`
Aggiunge una singola feature al database, generandone automaticamente l'embedding.

**Request Body:**
```json
{
  "feature": "User Authentication",
  "description": "Sistema di autenticazione utente con login e logout",
  "category": "Security",
  "evidence": "Documentato nella sezione 3.2",
  "confidence": 0.95,
  "source_title": "Requirements Document",
  "section_text": "Testo completo della sezione..."
}
```

**Response:**
```json
{
  "success": true,
  "id": "507f1f77bcf86cd799439011",
  "feature": {...},
  "message": "Feature added successfully"
}
```

**Dettagli:**
- L'embedding viene generato automaticamente combinando: `feature + ". " + description + ". Category: " + category`
- L'embedding è un vettore di 1536 dimensioni (OpenAI text-embedding-3-small)
- Campi automatici: `id`, `createdAt`, `updatedAt`, `embedding`

---

#### POST `/api/features/add-batch`
Aggiunge un batch di features, generando gli embeddings per tutte.

**Request Body:**
```json
{
  "features": [
    [
      {
        "feature": "User Authentication",
        "description": "...",
        "category": "Security"
      },
      {
        "feature": "Data Encryption",
        "description": "...",
        "category": "Security"
      }
    ]
  ]
}
```

**Response:**
```json
{
  "success": true,
  "processed": 2,
  "message": "Batch features added successfully"
}
```

**Dettagli:**
- Processa tutte le features in batch
- Genera embeddings per ciascuna feature
- Salva tutte le features nel database
- Più efficiente di chiamate singole per grandi quantità di dati

---

#### POST `/api/features/embed-batch`
Genera embeddings per un batch di features senza salvarle nel database.

**Request Body:**
```json
{
  "features": [
    [
      {
        "feature": "User Authentication",
        "description": "..."
      }
    ]
  ]
}
```

**Response:**
```json
{
  "features": [
    [
      {
        "feature": "User Authentication",
        "description": "...",
        "embedding": [0.123, -0.456, ...]
      }
    ]
  ]
}
```

**Dettagli:**
- Restituisce le features con gli embeddings aggiunti
- Utile per preview o pre-processing
- Non persiste i dati

---

#### GET `/api/features/all`
Recupera tutte le features dal database.

**Response:**
```json
[
  {
    "id": "507f1f77bcf86cd799439011",
    "feature": "User Authentication",
    "description": "...",
    "category": "Security",
    "embedding": [0.123, -0.456, ...],
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  }
]
```

---

#### POST `/api/features/cluster`
Raggruppa le features per similarità usando cosine similarity.

**Query Parameter:**
- `threshold` (opzionale, default: 0.85): soglia di similarità (0.0 - 1.0)

**Request:** Nessun body richiesto

**Response:**
```json
[
  [
    {
      "feature": "User Login",
      "description": "..."
    },
    {
      "feature": "User Authentication",
      "description": "..."
    }
  ],
  [
    {
      "feature": "Data Encryption",
      "description": "..."
    }
  ]
]
```

**Dettagli:**
- Usa cosine similarity tra embeddings
- Default threshold: 0.85 (85% similarità)
- Features con similarità >= threshold vengono raggruppate
- Gli embeddings vengono rimossi dalla risposta per ridurre la dimensione
- Usa algoritmo di clustering basato su grafi con DFS

---

#### POST `/api/features/validate-features`
Valida un set di features contro le summary features di riferimento.

**Query Parameter:**
- `threshold` (opzionale, default: 0.85): soglia di matching

**Request Body:**
```json
{
  "features": [
    {
      "feature": "User Authentication",
      "description": "..."
    }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "coverage": {
    "coveragePercentage": 75.5,
    "coveredCount": 15,
    "uncoveredCount": 5,
    "coveredFeatures": [...],
    "uncoveredFeatures": [...]
  },
  "reportSaved": true
}
```

**Dettagli:**
- Confronta le features fornite con le summary features nel database
- Calcola la percentuale di copertura
- Identifica quali features sono coperte e quali no
- Salva automaticamente un coverage report

---

### 1.3 Coverage Report Controller

**Base Path:** `/api/coverage-reports`

#### POST `/api/coverage-reports/`
Crea e salva un nuovo coverage report.

**Request Body:**
```json
{
  "reportName": "Sprint 1 Coverage Analysis",
  "description": "Analysis of feature coverage for sprint 1",
  "threshold": 0.85,
  "features": [
    {
      "feature": "User Authentication",
      "description": "..."
    }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "id": "507f1f77bcf86cd799439011",
  "coveragePercentage": 78.5,
  "coveredCount": 22,
  "uncoveredCount": 6
}
```

**Dettagli:**
- Analizza le features fornite contro le summary features
- Calcola metriche di copertura
- Persiste il report completo nel database
- Include dettagli su tutte le features coperte e non coperte

---

#### GET `/api/coverage-reports/{id}`
Recupera un coverage report specifico per ID.

**Path Parameter:**
- `id`: MongoDB ObjectId del report

**Response:**
```json
{
  "success": true,
  "report": {
    "id": "507f1f77bcf86cd799439011",
    "reportName": "Sprint 1 Coverage Analysis",
    "description": "...",
    "coverage": {
      "coveragePercentage": 78.5,
      "coveredCount": 22,
      "uncoveredCount": 6,
      "coveredFeatures": [...],
      "uncoveredFeatures": [...]
    },
    "threshold": 0.85,
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

---

#### GET `/api/coverage-reports/{id}/covered-features`
Recupera solo le features coperte di un report, arricchite con i dettagli della reference feature.

**Path Parameter:**
- `id`: MongoDB ObjectId del report

**Response:**
```json
{
  "success": true,
  "reportId": "507f1f77bcf86cd799439011",
  "count": 22,
  "coveredFeatures": [
    {
      "matchedFeature": {
        "feature": "User Authentication",
        "description": "...",
        "sectionText": "..."
      },
      "referenceFeature": {
        "feature": "Authentication System",
        "description": "...",
        "checklist": ["item1", "item2"],
        "example": "..."
      },
      "similarity": 0.92
    }
  ]
}
```

**Dettagli:**
- Include sia la feature fornita che quella di riferimento
- Mostra il punteggio di similarità
- Arricchito con checklist ed esempi dalla summary feature
- Utile per analisi dettagliata della copertura

---

### 1.4 Summary Feature Controller

**Base Path:** `/api/summary-features`

#### POST `/api/summary-features/add-batch`
Aggiunge un batch di summary features (features di riferimento).

**Request Body:**
```json
{
  "summarized_features": [
    {
      "feature": "Authentication System",
      "description": "Complete user authentication system",
      "count": "3",
      "checklist": [
        "Login functionality",
        "Logout functionality",
        "Password reset"
      ],
      "example": "User enters credentials and gets authenticated"
    }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "processed": 1,
  "ids": ["507f1f77bcf86cd799439011"],
  "message": "Summary features added successfully"
}
```

**Dettagli:**
- Summary features servono come riferimento per l'analisi di copertura
- Gli embeddings vengono generati automaticamente
- Includono checklist ed esempi per validazione dettagliata
- Campo `count` indica quante features simili sono state consolidate

---

#### GET `/api/summary-features/all`
Recupera tutte le summary features.

**Response:**
```json
[
  {
    "id": "507f1f77bcf86cd799439011",
    "feature": "Authentication System",
    "description": "...",
    "embedding": [0.123, -0.456, ...],
    "count": "3",
    "checklist": [...],
    "example": "..."
  }
]
```

---

### 1.5 Content Report Controller

**Base Path:** `/api/content`

#### POST `/api/content/consolidate`
Consolida un content report rimuovendo duplicati tramite clustering.

**Request Body:**
```json
{
  "raw_report": [
    {
      "use_case": [
        {
          "case_id": "UC001",
          "name": "User Login",
          "actors": ["User", "System"],
          "main_flow": ["User enters credentials", "System validates"],
          "alternative_flows": ["Invalid credentials"],
          "is_explicit": true
        }
      ],
      "requirements": [
        {
          "req_id": "REQ001",
          "description": "System shall authenticate users",
          "type": "functional",
          "source_text": "...",
          "quality_notes": "Clear and testable"
        }
      ],
      "tests": [
        {
          "test_id": "T001",
          "test_type": "Integration",
          "tested_artifact_name": "Login Module",
          "coverage_hint": "Tests UC001",
          "description_summary": "Verifies login functionality"
        }
      ],
      "architecture": [
        {
          "pattern": "MVC",
          "components": [
            {
              "name": "UserController",
              "responsibility": "Handle user requests",
              "design_notes": "RESTful endpoints",
              "communicates_with": [
                {"component": "UserService", "via": "HTTP"}
              ]
            }
          ],
          "analysis_summary": "..."
        }
      ]
    }
  ]
}
```

**Response:**
```json
{
  "use_case": [...],
  "requirements": [...],
  "tests": [...],
  "architecture": [...]
}
```

**Dettagli:**
- Identifica e rimuove duplicati usando clustering con threshold 0.92
- Riassegna ID sequenziali a requirements, use cases e tests
- Mantiene il primo elemento di ogni cluster
- Usa cosine similarity sugli embeddings delle descrizioni
- Processo:
  1. Estrae descrizioni da ogni item
  2. Genera embeddings per tutte le descrizioni
  3. Usa clustering pairwise con soglia 0.92
  4. Mantiene primo item di ogni cluster
  5. Riassegna ID nel formato: REQ001, UC001, T001, etc.

---

## 2. Modelli Dati

### 2.1 Feature (PDF Feature)

```json
{
  "id": "string (MongoDB ObjectId)",
  "feature": "string (nome feature)",
  "description": "string (descrizione dettagliata)",
  "category": "string (categoria)",
  "evidence": "string (evidenza nel documento)",
  "confidence": "number (0.0-1.0)",
  "source_title": "string (titolo sorgente)",
  "section_text": "string (testo completo sezione)",
  "filePath": "string (path file originale)",
  "embedding": "[number] (vettore 1536 dim)",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

---

### 2.2 SummaryFeature

```json
{
  "id": "string",
  "feature": "string",
  "description": "string",
  "embedding": "[number] (1536 dim)",
  "count": "string (numero features consolidate)",
  "checklist": ["string"],
  "example": "string"
}
```

---

### 2.3 Coverage

```json
{
  "coveragePercentage": "number (0-100)",
  "coveredCount": "integer",
  "uncoveredCount": "integer",
  "coveredFeatures": [
    {
      "referenceFeatureId": "string",
      "matchedFeature": {
        "feature": "string",
        "description": "string",
        "sectionText": "string"
      },
      "similarity": "number (0.0-1.0)"
    }
  ],
  "uncoveredFeatures": [
    {
      "referenceFeatureId": "string",
      "similarity": "number (migliore match trovato)"
    }
  ]
}
```

---

### 2.4 CoverageReport

```json
{
  "id": "string",
  "reportName": "string",
  "description": "string",
  "threshold": "number (0.0-1.0)",
  "coverage": "Coverage object",
  "providedFeatures": "integer",
  "totalSummaryFeatures": "integer",
  "success": "boolean",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

---

### 2.5 ContentReport

```json
{
  "use_case": [
    {
      "case_id": "string (es: UC001)",
      "name": "string",
      "actors": ["string"],
      "main_flow": ["string"],
      "alternative_flows": ["string"],
      "is_explicit": "boolean"
    }
  ],
  "requirements": [
    {
      "req_id": "string (es: REQ001)",
      "description": "string",
      "type": "string (functional/non-functional)",
      "source_text": "string",
      "quality_notes": "string"
    }
  ],
  "tests": [
    {
      "test_id": "string (es: T001)",
      "test_type": "string",
      "tested_artifact_name": "string",
      "coverage_hint": "string",
      "description_summary": "string"
    }
  ],
  "architecture": [
    {
      "pattern": "string (es: MVC, Microservices)",
      "components": [
        {
          "name": "string",
          "responsibility": "string",
          "design_notes": "string",
          "communicates_with": [
            {
              "component": "string",
              "via": "string"
            }
          ]
        }
      ],
      "analysis_summary": "string"
    }
  ]
}
```

---

## 3. Workflows Comuni

### 3.1 Workflow: Estrarre Features da PDF

```
1. POST /api/pdf/extract-sections
   - Input: PDF path + indice sezioni
   - Output: Testo estratto per sezione

2. [Processa il testo esternamente per identificare features]

3. POST /api/features/add-batch
   - Input: Array di features identificate
   - Output: Features salvate con embeddings generati
```

---

### 3.2 Workflow: Analisi di Copertura

```
1. POST /api/summary-features/add-batch
   - Input: Features di riferimento (una tantum)
   - Output: Summary features salvate

2. POST /api/coverage-reports/
   - Input: Features da validare + nome report
   - Output: Report di copertura completo

3. GET /api/coverage-reports/{id}/covered-features
   - Input: ID del report
   - Output: Dettagli features coperte con riferimenti
```

---

### 3.3 Workflow: Clustering Features Simili

```
1. POST /api/features/add-batch
   - Input: Features da analizzare
   - Output: Features salvate nel DB

2. POST /api/features/cluster?threshold=0.85
   - Input: Soglia di similarità
   - Output: Clusters di features simili
```

---

### 3.4 Workflow: Consolidamento Contenuti

```
1. [Estrai contenuti da PDF usando strumenti esterni]

2. POST /api/content/consolidate
   - Input: ContentReport con possibili duplicati
   - Output: ContentReport consolidato senza duplicati
```

---

## 4. Concetti Chiave per LLM

### 4.1 Embeddings
- **Modello:** OpenAI text-embedding-3-small
- **Dimensioni:** 1536
- **Generazione automatica:** Tutti gli endpoint che salvano features/summary features generano embeddings automaticamente
- **Formato testo per embedding feature:**
  ```
  {feature} + ". " + {description} + ". Category: " + {category}
  ```

### 4.2 Similarity Matching
- **Metrica:** Cosine Similarity
- **Formula:** `cosine_similarity = (A · B) / (||A|| * ||B||)`
- **Range:** 0.0 (totalmente diversi) a 1.0 (identici)
- **Threshold comuni:**
  - 0.85: clustering features, coverage analysis
  - 0.92: consolidamento contenuti (più restrittivo)

### 4.3 Clustering
- **Algoritmi disponibili:**
  1. **Pairwise Cosine Clustering:** Costruisce grafo di similarità + DFS per componenti connesse
  2. **DBSCAN:** Con cosine distance come metrica
- **Output:** Lista di liste, ogni sublista è un cluster
- **Note:** Gli embeddings vengono rimossi dalla risposta per efficienza

### 4.4 Coverage Analysis
- **Concetto:** Misura quanto un set di features copre le summary features di riferimento
- **Processo:**
  1. Genera embeddings per features fornite
  2. Calcola similarità con tutte le summary features
  3. Match se similarità >= threshold
  4. Calcola percentuale: `(covered / total_summary) * 100`
- **Output:** Covered features, uncovered features, percentuale

### 4.5 Content Consolidation
- **Scopo:** Rimuovere duplicati da contenuti estratti
- **Threshold:** 0.92 (più alto = più conservativo)
- **Items consolidati:**
  - Requirements (REQ001, REQ002, ...)
  - Use Cases (UC001, UC002, ...)
  - Tests (T001, T002, ...)
  - Architecture components
- **Strategia:** Mantiene il primo elemento di ogni cluster

---

## 5. Configurazione MongoDB

### Database: `features_repo`

**Collections:**
- `features`: Features estratte da PDF
- `summary_features`: Features di riferimento per coverage
- `coverage_reports`: Report di analisi copertura

**Connection String:** `mongodb://localhost:27017`

### Ottimizzazioni per Query
- `findAllWithEmbeddings()`: Solo features con embeddings non-null
- `streamAllWithEmbeddings()`: Streaming per grandi dataset
- `findEmbeddingsOnly()`: Minimal data fetch (id, feature, embedding)

---

## 6. Variabili d'Ambiente Richieste

```bash
OPENAI_API_KEY=sk-...
```

**Nota:** L'applicazione non parte senza questa variabile.

---

## 7. Limiti e Vincoli

### File Upload
- **Max file size:** 10MB
- **Max request size:** 10MB

### Embeddings
- **Costo:** Ogni chiamata a OpenAI ha un costo
- **Rate limiting:** Rispetta i limiti OpenAI API
- **Retry:** Non implementato automatic retry

### Database
- **No authentication:** MongoDB deve essere accessibile senza autenticazione
- **No connection pooling config:** Usa defaults

### Security
- **No authentication:** Tutti gli endpoint sono pubblici
- **No authorization:** Nessun controllo accessi
- **No rate limiting:** Applicativo non implementa rate limiting
- **No input validation:** Limitata validazione input

---

## 8. Errori Comuni

### Error: "OpenAI API key not configured"
**Causa:** Variabile `OPENAI_API_KEY` non impostata
**Soluzione:** Esporta la variabile d'ambiente

### Error: "MongoDB connection failed"
**Causa:** MongoDB non è in esecuzione o non raggiungibile
**Soluzione:** Avvia MongoDB su localhost:27017

### Error: "File not found"
**Causa:** Path del PDF non corretto
**Soluzione:** Verifica che il path sia assoluto e il file esista

### Error: "Threshold must be between 0 and 1"
**Causa:** Valore threshold non valido
**Soluzione:** Usa valori tra 0.0 e 1.0

---

## 9. Best Practices per l'Uso via LLM

### 9.1 Quando usare add vs add-batch
- **`add`**: Singola feature, feedback immediato richiesto
- **`add-batch`**: Multiple features, più efficiente per > 5 features

### 9.2 Scelta del Threshold
- **0.80-0.85**: Clustering ampio, più permissivo
- **0.85-0.90**: Balance tra precisione e recall
- **0.90-0.95**: Molto restrittivo, solo match quasi identici

### 9.3 Gestione Coverage Reports
- **Salva sempre reports importanti** con nomi descrittivi
- **Usa descriptions dettagliate** per contesto futuro
- **Recupera covered features** per analisi dettagliata

### 9.4 Consolidamento Contenuti
- **Usa sempre consolidate** prima di processare ulteriormente
- **Threshold 0.92** è ottimale per la maggior parte dei casi
- **Controlla ID riassegnati** per tracciabilità

### 9.5 Estrazione PDF
- **`extract-sections`** è preferibile per documenti strutturati
- **`extract`** solo se serve tutto il documento
- **`extract-index`** per range specifici

---

## 10. Esempi d'Uso Completi

### Esempio 1: Setup Iniziale Features di Riferimento

```bash
# 1. Estrai sezioni da PDF
POST /api/pdf/extract-sections
{
  "filePath": "/docs/requirements.pdf",
  "index": [
    {"section": "Features", "start": 10, "end": 25}
  ]
}

# 2. [Processa il testo con LLM per identificare features]

# 3. Salva summary features
POST /api/summary-features/add-batch
{
  "summarized_features": [
    {
      "feature": "User Authentication",
      "description": "Complete authentication system",
      "count": "1",
      "checklist": ["Login", "Logout", "Password reset"],
      "example": "User logs in with email/password"
    }
  ]
}
```

---

### Esempio 2: Validare Features Implementate

```bash
# 1. Prepara features implementate
POST /api/coverage-reports/
{
  "reportName": "Sprint 3 Implementation Coverage",
  "description": "Features implemented in sprint 3",
  "threshold": 0.85,
  "features": [
    {
      "feature": "Login System",
      "description": "Users can log in with credentials"
    },
    {
      "feature": "Logout Functionality",
      "description": "Users can log out from the system"
    }
  ]
}

# Response
{
  "success": true,
  "id": "abc123",
  "coveragePercentage": 66.67,
  "coveredCount": 2,
  "uncoveredCount": 1
}

# 2. Analizza dettagli copertura
GET /api/coverage-reports/abc123/covered-features

# Response mostra quali checklist items sono coperti
```

---

### Esempio 3: Clustering per Trovare Duplicati

```bash
# 1. Carica tutte le features
POST /api/features/add-batch
{
  "features": [[
    {"feature": "User Login", "description": "Login system"},
    {"feature": "Authentication", "description": "User authentication"},
    {"feature": "Data Export", "description": "Export data to CSV"}
  ]]
}

# 2. Esegui clustering
POST /api/features/cluster?threshold=0.85

# Response: Features simili raggruppate
[
  [
    {"feature": "User Login", ...},
    {"feature": "Authentication", ...}
  ],
  [
    {"feature": "Data Export", ...}
  ]
]
```

---

### Esempio 4: Consolidare Content Report

```bash
POST /api/content/consolidate
{
  "raw_report": [
    {
      "requirements": [
        {
          "req_id": "R1",
          "description": "System shall authenticate users",
          "type": "functional"
        },
        {
          "req_id": "R2",
          "description": "The system must authenticate users",
          "type": "functional"
        },
        {
          "req_id": "R3",
          "description": "Export data to CSV",
          "type": "functional"
        }
      ]
    }
  ]
}

# Response: R1 e R2 consolidati (simili > 0.92)
{
  "requirements": [
    {
      "req_id": "REQ001",
      "description": "System shall authenticate users",
      "type": "functional"
    },
    {
      "req_id": "REQ002",
      "description": "Export data to CSV",
      "type": "functional"
    }
  ]
}
```

---

## 11. Riferimenti Rapidi

### Threshold Recommendations
| Use Case | Threshold | Rationale |
|----------|-----------|-----------|
| Feature clustering | 0.85 | Balance precision/recall |
| Coverage analysis | 0.85 | Catch similar implementations |
| Content consolidation | 0.92 | Avoid false positives |
| Strict matching | 0.95+ | Only near-identical items |

### HTTP Status Codes
- **200 OK**: Successo
- **400 Bad Request**: Input non valido
- **404 Not Found**: Risorsa non trovata
- **500 Internal Server Error**: Errore server (spesso OpenAI o MongoDB)

### Collection Names (MongoDB)
- `features`: Features estratte
- `summary_features`: Features di riferimento
- `coverage_reports`: Report di copertura

---

## 12. Note Finali

Questa API è progettata per:
1. **Estrarre contenuti strutturati** da PDF
2. **Analizzare similarità** tra features usando embeddings
3. **Validare copertura** di implementazioni contro requisiti
4. **Consolidare duplicati** in report di contenuti

**Flusso tipico completo:**
```
PDF → Estrazione → Identificazione Features → Embedding →
Clustering/Coverage → Report Consolidato
```

L'uso di embeddings OpenAI permette matching semantico (non solo keyword matching), rendendo l'analisi più robusta e intelligente.
