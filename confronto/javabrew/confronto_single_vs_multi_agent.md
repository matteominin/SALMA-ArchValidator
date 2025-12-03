# Confronto Single-Agent vs Multi-Agent: Analisi JavaBrew

## Documento di Riferimento
- **Documento Originale:** JavaBrew.pdf
- **Data Confronto:** 3 Dicembre 2025

---

## 1. Panoramica dei Due Approcci

| Aspetto | Single-Agent | Multi-Agent |
|---------|--------------|-------------|
| Requisiti estratti | 62 | 45 |
| Use Case estratti | 18 | 30 |
| Test estratti | 63 | 58 |
| Copertura requisiti | 95.2% | 95.6% |
| Copertura Use Case | 83.3% | 83.3% |
| Voto finale | N/A | 22/30 |
| Pagine report | 23 | 22 |

**Valori reali nel documento originale:**
- Requisiti formali: **0** (nessuna sezione formale)
- Use Case espliciti: **8** (UC-1 a UC-8.1)
- Test documentati: **63**

---

## 2. Punti di Forza - Single-Agent

### 2.1 Accuratezza Conteggio Test
| Aspetto | Valutazione |
|---------|-------------|
| Test totali | 63 (corretto) |
| Match con documento | 100% |

Il single-agent ha estratto correttamente il numero esatto di test presenti nel documento originale.

### 2.2 Numerazione Use Case più Vicina all'Originale
| UC Single-Agent | UC Documento | Match |
|-----------------|--------------|-------|
| UC-1: User Login | UC-1: User Login | ✓ |
| UC-2: User Registration | UC-2: User Registration | ✓ |
| UC-3: Purchase Item | UC-3: Buy Item | ✓ |

I primi use case mantengono la numerazione originale del documento.

### 2.3 Struttura Report più Snella
- Report di 23 pagine vs 22
- Meno sezioni ridondanti
- Analisi più diretta

### 2.4 Minor Inflazione degli Use Case
- 18 UC estratti vs 30 del multi-agent
- Più vicino agli 8 UC reali (errore del 125% vs 275%)

---

## 3. Punti di Forza - Multi-Agent

### 3.1 Analisi Architetturale più Dettagliata

| Elemento | Single-Agent | Multi-Agent |
|----------|--------------|-------------|
| 6-layer architecture | ✓ Identificata | ✓ Identificata + dettagli |
| Pattern Builder | ✓ Menzionato | ✓ Analizzato con componenti |
| Pattern DAO | ✓ Menzionato | ✓ Lista completa DAO |
| Pattern Mapper | ✓ Menzionato | ✓ Analizzato |
| Componenti specifici | Generici | UserController, MachineController, etc. |

Il multi-agent fornisce una mappatura più precisa dei componenti architetturali.

### 3.2 Sistema di Scoring
- Fornisce un voto finale (22/30)
- Metriche quantitative più strutturate
- Formula di calcolo esplicita (anche se basata su dati non affidabili)

### 3.3 Valutazione Qualità Requisiti
Il multi-agent classifica i requisiti estratti:

| Classificazione | Descrizione |
|-----------------|-------------|
| Well-defined | Requisiti chiari e specifici |
| Needs detail | Requisiti che richiedono maggiori dettagli |
| Vague | Requisiti troppo generici |

Esempio positivo: REQ-45 "Future remote maintenance" correttamente marcato come feature futura.

### 3.4 Raccomandazioni più Strutturate
- Suggerimenti per Aggregate Root
- Proposte per Domain Events
- Roadmap architetturale (anche se fuori scope)

### 3.5 Formato più Professionale
- Tabelle di tracciabilità dettagliate
- Sezioni ben organizzate
- Executive summary chiaro

---

## 4. Criticità - Single-Agent

### 4.1 Inflazione Requisiti Maggiore

| Aspetto | Valore |
|---------|--------|
| REQ estratti | 62 |
| REQ nel documento | 0 formali |
| Errore | 62 requisiti inventati |

Il single-agent ha generato più requisiti "fantasma" rispetto al multi-agent (62 vs 45).

### 4.2 Errori di Numerazione Use Case

| UC Report | UC Reale | Problema |
|-----------|----------|----------|
| UC-4: Recharge Wallet | UC-5: Recharge Balance | Numerazione errata |
| UC-6: View Transaction History | Non esiste | Inventato |
| UC-8: Complete Maintenance Task | UC-6: Mark task as completed | Numerazione errata |
| UC-12-15: Inventory CRUD | Non esistono | Inventati |
| UC-18: Remote Maintenance | Non esiste | Inventato |

### 4.3 Struttura TOC Errata
Riferimenti a sezioni inesistenti nel documento:
- "2.3 Offline Operation & Resilience"
- "2.6 Architectural Overview"
- "Appendix: Complete Requirements Inventory"

### 4.4 Mancanza di Sistema di Valutazione
- Nessun voto finale
- Metriche meno strutturate
- Difficile valutare la "qualità" complessiva del documento

### 4.5 Distribuzione Test Non Verificabile
Afferma 71% unit, 19% integration, 10% system ma il documento originale non classifica i test per tipo.

---

## 5. Criticità - Multi-Agent

### 5.1 Maggiore Inflazione Use Case

| Aspetto | Valore |
|---------|--------|
| UC estratti | 30 |
| UC nel documento | 8 |
| Errore | +275% (22 UC inventati/suddivisi) |

Il multi-agent ha inflazionato maggiormente il numero di use case.

### 5.2 Errori di Conteggio Test

| Aspetto | Multi-Agent | Documento |
|---------|-------------|-----------|
| Test totali | 58 | 63 |
| Discrepanza | -5 test | - |

Ha perso 5 test rispetto al documento originale.

### 5.3 Numerazione Use Case Completamente Diversa

| UC Multi-Agent | UC Documento | Problema |
|----------------|--------------|----------|
| UC-8: User Login | UC-1: User Login | Numerazione errata |
| UC-9: User Login with Details | Non esiste | Inventato |
| UC-13: Buy Item | UC-3: Buy Item | Numerazione errata |
| UC-28: Track Active Connections | Non esiste | Inventato |
| UC-29: Remote Maintenance | Non esiste | Inventato |

La numerazione parte da UC-8 invece che da UC-1.

### 5.4 Voto Basato su Dati Inaffidabili
Il voto 22/30 è calcolato su:
- 45 requisiti auto-generati
- 30 use case inflazionati
- Metriche non verificabili

### 5.5 Raccomandazioni Enterprise Fuori Contesto
- "Add IoT gateway for remote maintenance"
- "Define offline storage (SQLite)"
- "Performance requirements missing"

Suggerimenti per sistema enterprise applicati a progetto accademico.

---

## 6. Errori Comuni ad Entrambi gli Approcci

| Errore | Single-Agent | Multi-Agent | Gravità |
|--------|--------------|-------------|---------|
| Requisiti auto-generati | 62 REQ | 45 REQ | **CRITICA** |
| Use Case inventati | UC-18 Remote Maintenance | UC-29 Remote Maintenance | **CRITICA** |
| Offline come gap critico | Sì | Sì | **ALTA** |
| Architettura corretta | Sì | Sì | - |
| Metriche non verificabili | Sì | Sì | **MEDIA** |
| Raccomandazioni fuori scope | Sì | Sì | **MEDIA** |

### Problema Fondamentale Condiviso
Entrambi gli approcci:
1. **Inventano una struttura formale** di requisiti che non esiste
2. **Valutano il documento** contro questa struttura auto-generata
3. **Trattano feature future** (offline operation) come gap critici
4. **Applicano metodologia enterprise** a un progetto accademico

---

## 7. Tabella Comparativa Finale

| Criterio | Single-Agent | Multi-Agent | Migliore |
|----------|--------------|-------------|----------|
| **Accuratezza Test** | 63/63 (100%) | 58/63 (92%) | Single |
| **Inflazione Requisiti** | +62 | +45 | Multi |
| **Inflazione Use Case** | +10 (125%) | +22 (275%) | Single |
| **Numerazione UC** | Parzialmente corretta | Completamente errata | Single |
| **Analisi Architettura** | Corretta ma generica | Corretta e dettagliata | Multi |
| **Sistema di Scoring** | Assente | Presente (22/30) | Multi |
| **Classificazione Qualità** | Assente | Presente | Multi |
| **Raccomandazioni** | Generiche | Strutturate | Multi |
| **Formato Report** | Standard | Professionale | Multi |
| **Affidabilità Complessiva** | Bassa | Bassa | Pari |

---

## 8. Conclusioni

### Single-Agent: Quando Preferirlo

**Pro:**
- Conteggio test accurato
- Minor inflazione degli use case
- Numerazione UC più vicina all'originale
- Output più rapido e snello

**Contro:**
- Maggiore inflazione requisiti
- Mancanza sistema di valutazione
- Analisi architetturale meno dettagliata

**Ideale per:** Analisi rapida dove l'accuratezza del conteggio test è prioritaria.

### Multi-Agent: Quando Preferirlo

**Pro:**
- Analisi architetturale più dettagliata
- Sistema di scoring strutturato
- Classificazione qualità requisiti
- Formato report più professionale
- Minor numero di requisiti inventati

**Contro:**
- Maggiore inflazione use case (+275%)
- Errore nel conteggio test (-5)
- Numerazione UC completamente errata
- Voto finale basato su dati inaffidabili

**Ideale per:** Analisi dove l'aspetto architetturale è prioritario e si accetta un trade-off sull'accuratezza dei conteggi.

### Raccomandazione Finale

| Aspetto | Raccomandazione |
|---------|-----------------|
| **Per analisi architettura** | Multi-Agent |
| **Per conteggio accurato** | Single-Agent |
| **Per valutazione complessiva** | Nessuno dei due (dati non affidabili) |
| **Per documenti accademici** | Entrambi inadeguati |
| **Per documenti enterprise** | Multi-Agent (con verifica manuale) |

**Conclusione:** Entrambi gli approcci producono risultati utili per l'**analisi architetturale**, ma sono **entrambi inaffidabili** per:
- Conteggio e tracciabilità requisiti
- Conteggio e numerazione use case
- Valutazione gap critici
- Scoring finale

La scelta tra i due dipende dalle priorità specifiche dell'analisi, ma in entrambi i casi è necessaria una **verifica manuale** dei risultati.

---

## 9. Matrice di Affidabilità per Sezione

| Sezione Report | Single-Agent | Multi-Agent |
|----------------|--------------|-------------|
| Architettura | MEDIA | **ALTA** |
| Pattern Design | MEDIA | **ALTA** |
| Requisiti | **BASSA** | **BASSA** |
| Use Cases | MEDIA | BASSA |
| Test | **ALTA** | MEDIA |
| Gap Critici | **BASSA** | **BASSA** |
| Raccomandazioni | MEDIA | MEDIA |
| Voto Finale | N/A | **BASSA** |

---

*Documento generato il 3 Dicembre 2025*