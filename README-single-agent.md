# Single-Agent PDF Validation Report Generator

Questo script esegue il prompt single-agent per analizzare un PDF di un progetto software e generare un report di validazione completo.

## Caratteristiche

- ✅ **Input PDF Diretto**: Claude legge il PDF nativamente (non serve conversione in testo)
- ✅ **Analisi Completa**: Estrae requirements, use cases, architecture, tests
- ✅ **Traceability Matrix**: Mappa completa tra tutti gli artefatti
- ✅ **Feature Validation**: Valida contro best practices universali
- ✅ **Report LaTeX**: Genera e compila automaticamente un report PDF professionale

## Prerequisiti

### 1. API Key di Anthropic

Ottieni una API key da: https://console.anthropic.com/

### 2. Python 3.8+

```bash
python --version  # Verifica versione
```

### 3. LaTeX (per compilazione PDF finale)

**macOS:**
```bash
brew install --cask mactex-no-gui
# oppure
brew install --cask basictex
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install texlive-latex-base texlive-latex-extra
```

**Windows:**
Scarica MiKTeX da: https://miktex.org/download

## Installazione

### 1. Installa le dipendenze Python

```bash
pip install -r requirements-single-agent.txt
```

### 2. Configura l'API Key

**Opzione A: Variabile d'ambiente**
```bash
export ANTHROPIC_API_KEY="sk-ant-..."
```

**Opzione B: File .env**
```bash
echo "ANTHROPIC_API_KEY=sk-ant-..." > .env
```

**Opzione C: Parametro da linea di comando**
```bash
python single_agent_executor.py report.pdf --api-key sk-ant-...
```

## Utilizzo

### Esempio Base

```bash
python single_agent_executor.py /path/to/your/report.pdf
```

Output:
```
[SingleAgent] Starting analysis of: /path/to/your/report.pdf
[SingleAgent] Output directory: ./report/1234567890
[SingleAgent] Model: claude-3-5-sonnet-20241022

[SingleAgent] Reading PDF file...
[SingleAgent] PDF size: 256.45 KB (base64)

[SingleAgent] Sending request to Claude API...
[SingleAgent] This may take several minutes for complex PDFs...

[SingleAgent] Response received!
[SingleAgent] Input tokens: 45234
[SingleAgent] Output tokens: 12567

[SingleAgent] Extracting JSON outputs...
  ✓ Saved: phase1_toc_extraction.json
  ✓ Saved: phase2_section_classification.json
  ✓ Saved: phase3_consolidation.json
  ✓ Saved: phase4_traceability.json
  ✓ Saved: phase5_feature_validation.json
  ✓ Saved: phase6_final_output.json

[SingleAgent] Extracting LaTeX report...
  ✓ LaTeX saved to: ./report/1234567890/report.tex
[SingleAgent] Compiling LaTeX to PDF...
  ✓ PDF compiled successfully: ./report/1234567890/report.pdf

================================================================================
[SingleAgent] Execution completed successfully!
================================================================================
Output directory: ./report/1234567890
LaTeX report: ./report/1234567890/report.pdf
Result summary: ./report/1234567890/execution_result.json
```

### Esempio con Output Directory Personalizzata

```bash
python single_agent_executor.py report.pdf --output-dir ./my-analysis
```

### Esempio con API Key Inline

```bash
python single_agent_executor.py report.pdf --api-key sk-ant-api03-...
```

## Struttura Output

Dopo l'esecuzione, troverai nella directory di output:

```
report/1234567890/
├── raw_response.txt                      # Risposta completa di Claude
├── phase1_toc_extraction.json           # Indice estratto dal PDF
├── phase2_section_classification.json   # Sezioni classificate + contenuti
├── phase3_consolidation.json            # Dati consolidati
├── phase4_traceability.json             # Matrice di traceability
├── phase5_feature_validation.json       # Validazione feature
├── phase6_final_output.json             # Output finale con metriche
├── report.tex                            # Sorgente LaTeX del report
├── report.pdf                            # Report PDF finale
├── report.aux                            # File ausiliari LaTeX
├── report.log                            # Log compilazione LaTeX
└── execution_result.json                 # Riepilogo esecuzione
```

## Output Finale (phase6_final_output.json)

```json
{
  "success": true,
  "pdfPath": "/path/to/report/1234567890/report.pdf",
  "texPath": "/path/to/report/1234567890/report.tex",
  "summary": {
    "requirements_extracted": 45,
    "requirements_by_type": {
      "functional": 28,
      "non_functional": 12,
      "constraint": 3,
      "goal": 2
    },
    "use_cases_extracted": 12,
    "architecture_components": 11,
    "tests_extracted": 67,
    "traceability_coverage_percentage": 93.3,
    "feature_coverage_percentage": 90.0,
    "validation_status": "PASSED"
  }
}
```

## Dettagli Tecnici

### Modello Utilizzato

- **Claude 3.5 Sonnet** (`claude-3-5-sonnet-20241022`)
- Supporto nativo per PDF (no conversione necessaria)
- Context window: 200K tokens
- Max output: 16K tokens

### Processo di Esecuzione

1. **Lettura PDF**: Il PDF viene codificato in base64 e inviato direttamente a Claude
2. **Elaborazione Single-Agent**: Claude esegue tutte le 6 fasi sequenzialmente
3. **Estrazione Output**: JSON e LaTeX vengono estratti dalla risposta
4. **Compilazione**: Il LaTeX viene compilato in PDF (richiede 2 passaggi per ToC)

### Costi Stimati

Basato sui prezzi Anthropic (Marzo 2024):
- Input: $3 / 1M tokens
- Output: $15 / 1M tokens

**Esempio per PDF da 100 pagine:**
- Input tokens: ~40K-60K
- Output tokens: ~10K-15K
- Costo stimato: **$0.30-$0.50** per esecuzione

### Limitazioni

1. **Dimensione PDF**: Massimo ~180 pagine (limite context window 200K tokens)
2. **Tempo di Esecuzione**: 2-10 minuti per PDF complessi
3. **LaTeX Compilation**: Richiede TeX installato localmente
4. **Feature Knowledge Base**: Il prompt assume l'esistenza di 50 feature universali nel KB di Claude

## Troubleshooting

### Errore: "ANTHROPIC_API_KEY environment variable not set"

**Soluzione**: Imposta l'API key come variabile d'ambiente o usa `--api-key`

```bash
export ANTHROPIC_API_KEY="sk-ant-..."
```

### Errore: "pdflatex not found"

**Soluzione**: Installa una distribuzione LaTeX (vedi sezione Prerequisiti)

### Errore: "LaTeX compilation failed"

**Soluzione**: Controlla il file `latex_error.log` nella directory di output per i dettagli dell'errore. Errori comuni:
- Caratteri speciali non escaped (`_`, `%`, `&`)
- Environment mismatch (es. `\begin{tabularx}` con `\end{tabular}`)

### PDF troppo grande

**Soluzione**: Per PDF > 180 pagine, considera di:
1. Dividere il PDF in sezioni più piccole
2. Usare un approccio multi-agent (più efficiente per documenti molto grandi)

## Confronto Single-Agent vs Multi-Agent

| Aspetto | Single-Agent | Multi-Agent |
|---------|-------------|-------------|
| **Semplicità** | ✅ Molto semplice | ⚠️ Complesso (22 nodi) |
| **Manutenibilità** | ✅ Facile (1 prompt) | ⚠️ Difficile (13 agenti) |
| **Costo per esecuzione** | ✅ ~$0.30-0.50 | ⚠️ ~$1.50-2.50 (prompt ripetuti) |
| **Velocità** | ⚠️ 2-10 min (sequenziale) | ✅ 1-5 min (parallelo) |
| **Scalabilità** | ⚠️ Limitata (200K tokens) | ✅ Illimitata |
| **Context retention** | ✅ Pieno contesto | ⚠️ Contesto frammentato |

## Esempi d'Uso

### Test con PDF di Esempio

```bash
# Usa il PDF di esempio nel repository
python single_agent_executor.py samples/sample-report.pdf
```

### Batch Processing

```bash
# Analizza tutti i PDF in una directory
for pdf in pdfs/*.pdf; do
    python single_agent_executor.py "$pdf"
done
```

### Integrazione in Script Python

```python
from single_agent_executor import SingleAgentExecutor

executor = SingleAgentExecutor(api_key="sk-ant-...")
result = executor.execute("report.pdf", output_dir="./output")

print(f"Validation status: {result['validation_status']}")
print(f"Report PDF: {result['latex_path']}")
```

## Prossimi Passi

1. **Testa il sistema** con un PDF di esempio
2. **Valida i risultati** confrontando con analisi manuale
3. **Confronta con multi-agent** per benchmark di qualità/costo/tempo
4. **Itera sul prompt** basandoti sui risultati

## Supporto

Per problemi o domande:
- Controlla i log in `raw_response.txt` e `execution_result.json`
- Verifica che l'API key sia valida
- Assicurati che il PDF sia leggibile (non corrotto, non protetto da password)

## License

MIT
