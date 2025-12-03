# 🚀 Quick Start con GPT-5.1

## Esecuzione

```bash
python single_agent_executor_openai.py report.pdf --model gpt-5.1
```

## Output a Console

Vedrai qualcosa come:

```
================================================================================
[SingleAgent] PDF VALIDATION REPORT GENERATOR
================================================================================
[SingleAgent] PDF Input: /path/to/report.pdf
[SingleAgent] Output directory: ./report/1733156789
[SingleAgent] Start time: 2024-12-02 18:39:49

[SingleAgent] Extracting text from PDF: report.pdf
[SingleAgent] PDF has 85 pages
[SingleAgent] Extracted text saved to: ./report/1733156789/extracted_text.txt
[SingleAgent] Total characters: 245,678
[SingleAgent] Estimated tokens: 61,419
[SingleAgent] PDF extraction took: 2.34 seconds

[SingleAgent] Sending request to OpenAI API...
[SingleAgent] This may take several minutes for complex PDFs...

[SingleAgent] Response received!

================================================================================
[SingleAgent] TOKEN USAGE STATISTICS
================================================================================
Input tokens:      65,234
Output tokens:     14,567
Total tokens:      79,801
API call time:     180.45 seconds (3.01 minutes)
Note: GPT-5 pricing is estimated

Estimated cost:
  Input:           $0.9785
  Output:          $0.6555
  Total:           $1.6340
================================================================================

[SingleAgent] Raw response saved to: ./report/1733156789/raw_response.txt
[SingleAgent] Extracting JSON outputs...
  ✓ Saved: phase1_toc_extraction.json
  ✓ Saved: phase2_section_classification.json
  ✓ Saved: phase3_consolidation.json
  ✓ Saved: phase4_traceability.json
  ✓ Saved: phase5_feature_validation.json
  ✓ Saved: phase6_final_output.json

[SingleAgent] Extracting LaTeX report...
  ✓ LaTeX saved to: report.tex
[SingleAgent] Compiling LaTeX to PDF...
  ✓ PDF compiled successfully: report.pdf

================================================================================
[SingleAgent] EXECUTION COMPLETED SUCCESSFULLY!
================================================================================

📁 OUTPUT LOCATIONS:
   Output directory:    ./report/1733156789
   Raw response:        ./report/1733156789/raw_response.txt
   📄 PDF Report:       ./report/1733156789/report.pdf
   Result summary:      ./report/1733156789/execution_result.json

⏱️  EXECUTION TIME:
   Total time:          195.32 seconds (3.26 minutes)
   - PDF extraction:    2.34s
   - API call:          180.45s (3.01 min)
   - LaTeX compilation: 12.53s

💰 COST SUMMARY:
   Model:               gpt-5.1
   Input tokens:        65,234
   Output tokens:       14,567
   Total tokens:        79,801
   Estimated cost:      $1.6340

================================================================================
```

## 📂 Dove Trovare i Risultati

Dopo l'esecuzione, tutti i file sono nella directory `./report/{timestamp}/`:

```
report/1733156789/
├── 📄 report.pdf                         ⬅️ REPORT FINALE (QUESTO È IL PIÙ IMPORTANTE!)
├── 📊 execution_result.json              ⬅️ Statistiche complete (token, tempo, costo)
├── phase6_final_output.json              ⬅️ Metriche del report (requirements, coverage, ecc.)
│
├── extracted_text.txt                    # Testo estratto dal PDF
├── extracted_pages.json                  # Testo per pagina
├── raw_response.txt                      # Risposta completa di GPT-5.1
│
├── phase1_toc_extraction.json           # Indice estratto
├── phase2_section_classification.json   # Sezioni classificate
├── phase3_consolidation.json            # Dati consolidati
├── phase4_traceability.json             # Matrice di traceability
├── phase5_feature_validation.json       # Validazione feature
│
├── report.tex                            # Sorgente LaTeX
├── report.aux                            # File ausiliari LaTeX
└── report.log                            # Log compilazione LaTeX
```

## 📊 Statistiche Dettagliate

### File: `execution_result.json`

```json
{
  "success": true,
  "pdf_input": "/path/to/report.pdf",
  "output_directory": "./report/1733156789",
  "raw_response_path": "./report/1733156789/raw_response.txt",
  "latex_path": "./report/1733156789/report.pdf",

  "execution_stats": {
    "start_time": "2024-12-02 18:39:49",
    "end_time": "2024-12-02 18:43:04",
    "total_time_seconds": 195.32,
    "total_time_minutes": 3.26,
    "pdf_extraction_time_seconds": 2.34,
    "api_call_time_seconds": 180.45,
    "latex_compilation_time_seconds": 12.53
  },

  "token_stats": {
    "model": "gpt-5.1",
    "input_tokens": 65234,
    "output_tokens": 14567,
    "total_tokens": 79801,
    "estimated_cost": {
      "input_cost_usd": 0.9785,
      "output_cost_usd": 0.6555,
      "total_cost_usd": 1.634,
      "cost_per_1k_input": 0.015,
      "cost_per_1k_output": 0.045
    }
  }
}
```

### File: `phase6_final_output.json`

```json
{
  "success": true,
  "pdfPath": "./report/1733156789/report.pdf",
  "texPath": "./report/1733156789/report.tex",
  "summary": {
    "requirements_extracted": 45,
    "requirements_by_type": {
      "functional": 28,
      "non_functional": 12,
      "constraint": 3,
      "goal": 2
    },
    "requirements_quality": {
      "well_defined": 32,
      "needs_detail": 10,
      "vague": 3
    },
    "use_cases_extracted": 12,
    "use_cases_explicit": 10,
    "use_cases_implicit": 2,
    "architecture_components": 11,
    "architecture_pattern": "Layered Architecture",
    "tests_extracted": 67,
    "tests_by_type": {
      "unit": 45,
      "integration": 15,
      "system": 5,
      "performance": 2
    },
    "traceability_coverage_percentage": 93.3,
    "traceability_details": {
      "requirements_covered": 42,
      "requirements_uncovered": 3,
      "use_cases_fully_tested": 10,
      "use_cases_partially_tested": 2,
      "orphan_requirements": 3,
      "orphan_tests": 2
    },
    "feature_coverage_percentage": 90.0,
    "feature_details": {
      "total_kb_features": 50,
      "covered_features": 45,
      "uncovered_features": 5
    },
    "validation_status": "PASSED",
    "critical_issues": 3,
    "warnings": 6
  }
}
```

## 🎯 I 3 File Più Importanti

### 1. 📄 `report.pdf`
**IL REPORT FINALE COMPLETO**
- 6 sezioni con analisi dettagliata
- Tabelle con requirements, use cases, architecture, tests
- Matrice di traceability completa
- Validazione feature con checklist
- Raccomandazioni prioritizzate

### 2. 📊 `execution_result.json`
**STATISTICHE ESECUZIONE**
- Tempo totale e per fase
- Token utilizzati (input/output)
- Costo stimato in USD
- Timestamp inizio/fine

### 3. 📈 `phase6_final_output.json`
**METRICHE DEL REPORT**
- Numero di requirements/use cases/tests estratti
- Percentuali di coverage
- Status di validazione
- Issue critici

## 📖 Come Leggere i Risultati

### Console Output
Durante l'esecuzione vedi:
1. ✅ Estrazione PDF (tempo: ~2-5s)
2. ✅ Chiamata API (tempo: ~2-5 min)
3. ✅ Statistiche token e costo
4. ✅ Estrazione JSON (6 file)
5. ✅ Compilazione LaTeX
6. ✅ Summary finale con locations

### File JSON
Tutti i JSON sono formattati e leggibili:
```bash
# Visualizza metriche finali
cat report/1733156789/phase6_final_output.json | jq '.summary'

# Visualizza statistiche esecuzione
cat report/1733156789/execution_result.json | jq '.execution_stats'

# Visualizza costi
cat report/1733156789/execution_result.json | jq '.token_stats.estimated_cost'
```

### Report PDF
Apri `report.pdf` per il report completo professionale con:
- Executive Summary
- Content Extraction Results
- Traceability Matrix
- Feature-Based Validation
- Detailed Analysis
- Recommendations

## ⚡ Comandi Rapidi

```bash
# Esegui con GPT-5.1
python single_agent_executor_openai.py report.pdf --model gpt-5.1

# Apri il PDF generato (macOS)
open report/*/report.pdf

# Visualizza statistiche
cat report/*/execution_result.json | jq '.'

# Visualizza metriche report
cat report/*/phase6_final_output.json | jq '.summary'

# Calcola costo totale
cat report/*/execution_result.json | jq '.token_stats.estimated_cost.total_cost_usd'
```

## 💡 Tip

Per trovare rapidamente l'ultimo report generato:

```bash
# Trova directory più recente
LATEST=$(ls -t report/ | head -1)

# Apri il PDF
open "report/$LATEST/report.pdf"

# Vedi statistiche
cat "report/$LATEST/execution_result.json" | jq '.'
```

## 📊 Esempio Statistiche Reali

Per un PDF di **85 pagine** (~60K tokens):

| Metrica | Valore |
|---------|--------|
| Tempo totale | 3.26 minuti |
| Token input | 65,234 |
| Token output | 14,567 |
| Costo | $1.63 |
| Requirements | 45 |
| Use Cases | 12 |
| Tests | 67 |
| Coverage | 93.3% |

## 🎓 Next Steps

1. ✅ Esegui il comando
2. 📄 Apri `report/*/report.pdf`
3. 📊 Controlla `execution_result.json` per statistiche
4. 📈 Analizza `phase6_final_output.json` per metriche

Tutto pronto! 🚀
