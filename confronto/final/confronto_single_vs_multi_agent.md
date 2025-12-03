# Analisi Comparativa: Approccio Single-Agent vs Multi-Agent per la Validazione Automatizzata di Elaborati Software

## Abstract

Questo documento presenta un'analisi comparativa empirica tra due approcci di validazione automatizzata basati su Large Language Models (LLM): un sistema **Single-Agent** e un sistema **Multi-Agent**. L'analisi è stata condotta su due elaborati software di natura diversa: **JavaBrew** (sistema di gestione distributori automatici) e **Library Management** (applicazione gestionale per biblioteca). I risultati evidenziano differenze significative in termini di accuratezza, affidabilità e completezza dell'analisi prodotta.

---

## 1. Metodologia di Confronto

### 1.1 Elaborati Analizzati

| Elaborato | Dominio | Complessità | Use Case Reali | Test Reali |
|-----------|---------|-------------|----------------|------------|
| JavaBrew | Distributori automatici | Media-Alta | 8 | 63 |
| Library Management | Gestione biblioteca | Media | 13 | 61 |

### 1.2 Metriche di Valutazione

L'analisi comparativa si basa sulle seguenti metriche quantitative:
- **Accuratezza estrazione requisiti**: corrispondenza con requisiti documentati
- **Accuratezza estrazione Use Case**: match con UC esplicitamente definiti
- **Accuratezza conteggio test**: corrispondenza con risultati di esecuzione
- **Precisione tracciabilità**: validità delle mappature tra artefatti
- **Tasso di false positive**: criticità riportate ma non reali

---

## 2. Risultati dell'Estrazione dei Requisiti

### 2.1 Dati Quantitativi

| Elaborato | Single-Agent | Multi-Agent | Valore Reale |
|-----------|--------------|-------------|--------------|
| JavaBrew | 62 requisiti | 45 requisiti | 0 (nessuna sezione formale) |
| Library | 18 requisiti | 3 requisiti | ~18 (impliciti) |

### 2.2 Analisi Qualitativa

#### Caso JavaBrew: Inflazione dei Requisiti

Entrambi gli approcci hanno generato requisiti non presenti nel documento originale. Tuttavia, il comportamento differisce significativamente:

**Single-Agent** - Ha prodotto 62 requisiti, nessuno dei quali formalmente presente nel documento:

> *"REQ estratti: 62, REQ nel documento: 0 formali, Errore: 62 requisiti inventati"*
> — Confronto JavaBrew, Sezione 4.1

**Multi-Agent** - Ha prodotto 45 requisiti con un tentativo di classificazione qualitativa:

> *"Il multi-agent classifica i requisiti estratti: Well-defined (Requisiti chiari e specifici), Needs detail (Requisiti che richiedono maggiori dettagli), Vague (Requisiti troppo generici)"*
> — Confronto JavaBrew, Sezione 3.3

> *"Esempio positivo: REQ-45 'Future remote maintenance' correttamente marcato come feature futura"*
> — Confronto JavaBrew, Sezione 3.3

#### Caso Library: Comportamento Divergente

**Single-Agent** - Estrazione dettagliata e categorizzata:

> *"Requisiti estratti: 18 (12 funzionali, 4 non funzionali, 2 di sistema)"*
> *"Accuratezza: 90%"*
> — Confronto Library, Sezione 1

**Multi-Agent** - Estrazione minimalista e superficiale:

> *"Requisiti estratti: 3 (generici ad alto livello)"*
> *"Accuratezza: 70% (ma superficiale)"*
> — Confronto Library, Sezione 1

### 2.3 Evidenze Comparative

| Metrica | Single-Agent | Multi-Agent |
|---------|--------------|-------------|
| Granularità | Alta (categorizzazione presente) | Variabile (da 3 a 45) |
| Consistenza tra elaborati | Media | Bassa |
| Categorizzazione | Presente | Solo in JavaBrew |

**Conclusione parziale**: Il Single-Agent mostra maggiore consistenza nella granularità dell'estrazione, mentre il Multi-Agent presenta comportamento erratico (minimalista in Library, inflazionato in JavaBrew).

---

## 3. Risultati dell'Estrazione degli Use Case

### 3.1 Dati Quantitativi

| Elaborato | Single-Agent | Multi-Agent | Valore Reale | Errore SA | Errore MA |
|-----------|--------------|-------------|--------------|-----------|-----------|
| JavaBrew | 18 UC | 30 UC | 8 UC | +125% | +275% |
| Library | 13 UC | 17 UC | 13 UC | 0% | +31% |

### 3.2 Analisi degli Errori

#### Caso JavaBrew: Inflazione Sistematica

**Single-Agent** - Errori di numerazione e UC inventati:

> *"UC-4: Recharge Wallet → UC-5: Recharge Balance (Numerazione errata)"*
> *"UC-6: View Transaction History → Non esiste (Inventato)"*
> *"UC-12-15: Inventory CRUD → Non esistono (Inventati)"*
> *"UC-18: Remote Maintenance → Non esiste (Inventato)"*
> — Confronto JavaBrew, Sezione 4.2

**Multi-Agent** - Numerazione completamente divergente e UC fantasma:

> *"UC-8: User Login → UC-1: User Login (Numerazione errata)"*
> *"UC-9: User Login with Details → Non esiste (Inventato)"*
> *"UC-28: Track Active Connections → Non esiste (Inventato)"*
> *"UC-29: Remote Maintenance → Non esiste (Inventato)"*
> *"La numerazione parte da UC-8 invece che da UC-1"*
> — Confronto JavaBrew, Sezione 5.3

#### Caso Library: Divergenza Significativa

**Single-Agent** - Estrazione perfetta:

> *"Use Case estratti: 13"*
> *"Use Case corretti: 13/13 (100%)"*
> *"Struttura: Corretta e coerente con l'elaborato"*
> — Confronto Library, Sezione 2

**Multi-Agent** - Frammentazione e UC fantasma:

> *"Use Case estratti: 17"*
> *"Use Case corretti: ~6-7/17 (40%)"*
> *"Problemi: UC frammentati e UC 'fantasma' inventati"*
> — Confronto Library, Sezione 2

> *"UC fantasma: 'Domain Model Definition', 'Testing per Item Type' non esistono"*
> *"Frammentazione: UC singoli divisi in multipli"*
> *"Mappatura errata: UC-7 nel report ≠ UC-7 nell'elaborato"*
> — Confronto Library, Sezione 2

### 3.3 Tabella Comparativa Accuratezza UC

| Metrica | Single-Agent | Multi-Agent |
|---------|--------------|-------------|
| Accuratezza JavaBrew | ~44% (8 corretti su 18) | ~27% (8 corretti su 30) |
| Accuratezza Library | **100%** (13 su 13) | **40%** (6-7 su 17) |
| UC inventati JavaBrew | ~10 | ~22 |
| UC inventati Library | 0 | 4+ |
| Media accuratezza | **72%** | **33.5%** |

**Conclusione parziale**: Il Single-Agent dimostra accuratezza significativamente superiore nell'estrazione degli Use Case (+38.5 punti percentuali). Il Multi-Agent tende a frammentare UC esistenti e inventarne di nuovi.

---

## 4. Risultati dell'Analisi Architetturale

### 4.1 Dati Quantitativi

| Elaborato | Architettura Reale | Single-Agent | Multi-Agent |
|-----------|-------------------|--------------|-------------|
| JavaBrew | 6-layer + Builder + DAO + Mapper | Identificata | Identificata + dettagli |
| Library | MVC + Singleton + DAO | Identificata | Identificata |

### 4.2 Profondità dell'Analisi

**Multi-Agent** - Maggiore dettaglio sui componenti (JavaBrew):

> *"6-layer architecture: ✓ Identificata + dettagli"*
> *"Pattern Builder: ✓ Analizzato con componenti"*
> *"Pattern DAO: ✓ Lista completa DAO"*
> *"Componenti specifici: UserController, MachineController, etc."*
> — Confronto JavaBrew, Sezione 3.1

**Single-Agent** - Identificazione corretta ma generica (JavaBrew):

> *"6-layer architecture: ✓ Identificata"*
> *"Pattern Builder: ✓ Menzionato"*
> *"Pattern DAO: ✓ Menzionato"*
> *"Componenti specifici: Generici"*
> — Confronto JavaBrew, Sezione 3.1

**Parità su Library**:

> *"Accuratezza architettura Single-Agent: 85%"*
> *"Accuratezza architettura Multi-Agent: 85%"*
> — Confronto Library, Sezione 3

### 4.3 Pattern Non Rilevati

**Single-Agent** (Library):

> *"Pattern rilevati: MVC, Singleton, DAO"*
> *"Pattern mancanti: Observer (per notifiche)"*
> — Confronto Library, Sezione 3

**Multi-Agent** (Library):

> *"Pattern rilevati: MVC, DAO, Service Layer"*
> *"Pattern mancanti: Singleton esplicito"*
> — Confronto Library, Sezione 3

### 4.4 Tabella Comparativa Architettura

| Metrica | Single-Agent | Multi-Agent |
|---------|--------------|-------------|
| Accuratezza media | 85% | 85% |
| Profondità analisi | Media | **Alta** |
| Componenti identificati (Library) | 16 | ~8 principali |
| Dettaglio pattern | Menzionati | **Analizzati** |

**Conclusione parziale**: Parità sull'accuratezza (85%), ma il Multi-Agent fornisce analisi architetturale più dettagliata e componenti specifici.

---

## 5. Risultati dell'Analisi dei Test

### 5.1 Dati Quantitativi

| Elaborato | Single-Agent | Multi-Agent | Valore Reale | Errore SA | Errore MA |
|-----------|--------------|-------------|--------------|-----------|-----------|
| JavaBrew | 63 test | 58 test | 63 test | **0%** | -8% |
| Library | 12 classi | 47 test | 61 test | N/A | -23% |

### 5.2 Evidenze di Accuratezza

**Single-Agent** (JavaBrew) - Conteggio perfetto:

> *"Test totali: 63 (corretto)"*
> *"Match con documento: 100%"*
> — Confronto JavaBrew, Sezione 2.1

**Multi-Agent** (JavaBrew) - Errore di conteggio:

> *"Test totali: 58"*
> *"Discrepanza: -5 test"*
> *"Ha perso 5 test rispetto al documento originale"*
> — Confronto JavaBrew, Sezione 5.2

**Multi-Agent** (Library) - Errore significativo:

> *"Test identificati: 47 (errato - dovrebbero essere 61)"*
> — Confronto Library, Sezione 4

L'elaborato Library riporta esplicitamente:

> *"Tests run: 61, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS"*
> — Confronto Library, Sezione 4 (citazione da elaborato originale)

### 5.3 Classificazione dei Test

**Single-Agent** (Library) - Classificazione strutturata:

> *"Classi di test identificate: 12"*
> *"Tipologie: Unit, Integration, E2E"*
> *"Unit: 4 classi, Integration: 4 classi, E2E: 4 classi"*
> — Confronto Library, Sezione 4

**Single-Agent** (JavaBrew) - Distribuzione non verificabile:

> *"Afferma 71% unit, 19% integration, 10% system ma il documento originale non classifica i test per tipo"*
> — Confronto JavaBrew, Sezione 4.5

### 5.4 Tabella Comparativa Test

| Metrica | Single-Agent | Multi-Agent |
|---------|--------------|-------------|
| Accuratezza conteggio JavaBrew | **100%** | 92% |
| Accuratezza conteggio Library | N/A | 77% |
| Identificazione classi | Presente | Assente |
| Framework rilevati | JUnit 5, Mockito, Maven | JUnit 5, Mockito |

**Conclusione parziale**: Il Single-Agent è più affidabile nel conteggio dei test, con accuratezza del 100% su JavaBrew contro il 77-92% del Multi-Agent.

---

## 6. Risultati della Tracciabilità

### 6.1 Dati Quantitativi

| Elaborato | Single-Agent | Multi-Agent |
|-----------|--------------|-------------|
| JavaBrew | Non quantificata | Non quantificata |
| Library | 90% | 50% |

### 6.2 Qualità delle Mappature

**Single-Agent** (Library) - Tracciabilità completa:

> *"Req → UC: ✅ Mappature complete e corrette"*
> *"UC → Architettura: ✅ Corretta"*
> *"UC → Test: ✅ Corretta"*
> *"Accuratezza tracciabilità: 90%"*
> — Confronto Library, Sezione 5

**Multi-Agent** (Library) - Tracciabilità compromessa:

> *"Req → UC: ⚠️ Mappatura troppo generica (3 req → 17 UC tutti)"*
> *"UC → Architettura: ⚠️ Basata su UC errati"*
> *"UC → Test: ⚠️ Basata su UC errati"*
> *"Accuratezza tracciabilità: 50%"*
> — Confronto Library, Sezione 5

### 6.3 Impatto degli Errori a Cascata

Il Multi-Agent presenta un problema strutturale: gli errori nell'estrazione degli UC si propagano alla tracciabilità:

> *"UC → Architettura: Errata (base UC errata)"*
> *"UC → Test: Errata"*
> — Confronto Library, Sezione 5

### 6.4 Tabella Comparativa Tracciabilità

| Metrica | Single-Agent | Multi-Agent |
|---------|--------------|-------------|
| Accuratezza Library | **90%** | 50% |
| Granularità | Alta | Bassa |
| Errori a cascata | No | Sì |

**Conclusione parziale**: Il Single-Agent produce tracciabilità significativamente più accurate (+40 punti percentuali), senza propagazione di errori.

---

## 7. Analisi delle Criticità Rilevate

### 7.1 Dati Quantitativi

| Elaborato | Single-Agent | Multi-Agent |
|-----------|--------------|-------------|
| JavaBrew - criticità totali | N/A | N/A |
| Library - criticità totali | 4 | 6 |
| Library - criticità valide | 4 (100%) | 4 (67%) |
| Library - false positive | 0 | 2 |

### 7.2 False Positive del Multi-Agent (Library)

> *"Vague Component Responsibilities: ❌ Falsa - elaborato documenta chiaramente"*
> *"Undefined Architectural Pattern: ❌ Falsa - MVC/DAO/Singleton documentati"*
> — Confronto Library, Sezione 6

### 7.3 Criticità Valide Comuni

**Single-Agent** (Library):

> *"Pattern Observer non rilevato: ✅ Valida"*
> *"JaCoCo non menzionato: ✅ Valida"*
> *"Requisiti NFR incompleti: ✅ Valida"*
> *"Diagrammi di sequenza non analizzati: ✅ Valida"*
> — Confronto Library, Sezione 6

**Multi-Agent** (Library) - criticità valide:

> *"Missing Offline Resilience: ✅ Valida"*
> *"No Domain Events Infrastructure: ✅ Valida"*
> — Confronto Library, Sezione 6

### 7.4 Errore Comune: Feature Future come Gap

Entrambi gli approcci commettono l'errore di trattare feature future pianificate come gap critici:

> *"Errore comune - Offline come gap critico: Sì (entrambi)"*
> *"Gravità: ALTA"*
> — Confronto JavaBrew, Sezione 6

### 7.5 Tabella Comparativa Criticità

| Metrica | Single-Agent | Multi-Agent |
|---------|--------------|-------------|
| Tasso criticità valide | **100%** | 67% |
| False positive | 0 | 2 |
| Utilità feedback | Alta | Media |

**Conclusione parziale**: Il Single-Agent non produce false positive, mentre il Multi-Agent ha un tasso del 33% di criticità errate.

---

## 8. Sistema di Valutazione e Scoring

### 8.1 Presenza del Sistema di Scoring

| Elaborato | Single-Agent | Multi-Agent |
|-----------|--------------|-------------|
| JavaBrew | Assente | 22/30 |
| Library | 82% copertura | 18/30 |

### 8.2 Affidabilità dello Scoring

**Multi-Agent** (JavaBrew) - Score basato su dati inaffidabili:

> *"Il voto 22/30 è calcolato su: 45 requisiti auto-generati, 30 use case inflazionati, Metriche non verificabili"*
> — Confronto JavaBrew, Sezione 5.4

**Multi-Agent** (Library) - Score sottostimato:

> *"Punteggio assegnato: 18/30"*
> *"Punteggio suggerito (post-verifica): ~23/30"*
> *"Problema: Sottostima significativa (-5 punti)"*
> — Confronto Library, Sezione 7

### 8.3 Approccio Single-Agent

> *"Copertura feature dichiarata: 82%"*
> *"Approccio: Feature-based validation"*
> *"Giudizio: Realistico e giustificato"*
> — Confronto Library, Sezione 7

### 8.4 Tabella Comparativa Scoring

| Metrica | Single-Agent | Multi-Agent |
|---------|--------------|-------------|
| Sistema presente | No (metriche %) | Sì (voto numerico) |
| Affidabilità | Media | **Bassa** |
| Base di calcolo | Metriche verificabili | Dati auto-generati |

**Conclusione parziale**: Il Multi-Agent fornisce un voto numerico, ma questo è calcolato su dati inaffidabili (requisiti inventati, UC errati), risultando in valutazioni non attendibili.

---

## 9. Formato e Presentazione del Report

### 9.1 Caratteristiche Strutturali

**Multi-Agent** - Formato professionale:

> *"Tabelle di tracciabilità dettagliate"*
> *"Sezioni ben organizzate"*
> *"Executive summary chiaro"*
> *"Raccomandazioni più strutturate: Suggerimenti per Aggregate Root, Proposte per Domain Events, Roadmap architetturale"*
> — Confronto JavaBrew, Sezioni 3.4-3.5

**Single-Agent** - Formato standard:

> *"Report di 23 pagine vs 22"*
> *"Meno sezioni ridondanti"*
> *"Analisi più diretta"*
> — Confronto JavaBrew, Sezione 2.3

### 9.2 Lunghezza Report

| Elaborato | Single-Agent | Multi-Agent |
|-----------|--------------|-------------|
| JavaBrew | 23 pagine | 22 pagine |
| Library | 26 pagine | 17 pagine |

### 9.3 Raccomandazioni Fuori Contesto

**Multi-Agent** (JavaBrew) - Suggerimenti enterprise per progetto accademico:

> *"Raccomandazioni Enterprise Fuori Contesto:"*
> *"- 'Add IoT gateway for remote maintenance'"*
> *"- 'Define offline storage (SQLite)'"*
> *"- 'Performance requirements missing'"*
> *"Suggerimenti per sistema enterprise applicati a progetto accademico"*
> — Confronto JavaBrew, Sezione 5.5

**Single-Agent** (Library) - Raccomandazione inappropriata:

> *"Raccomandazione API REST non applicabile (app desktop)"*
> — Confronto Library, Sezione 9

---

## 10. Sintesi dei Punti di Forza e Debolezza

### 10.1 Single-Agent

#### Punti di Forza

| # | Punto di Forza | Evidenza |
|---|----------------|----------|
| 1 | Accuratezza Use Case | *"Use Case corretti: 13/13 (100%)"* — Library |
| 2 | Conteggio test preciso | *"Test totali: 63 (corretto), Match con documento: 100%"* — JavaBrew |
| 3 | Zero false positive | *"False criticità: 0"* — Library |
| 4 | Tracciabilità affidabile | *"Accuratezza tracciabilità: 90%"* — Library |
| 5 | Requisiti categorizzati | *"18 (12 funzionali, 4 non funzionali, 2 di sistema)"* — Library |

#### Punti di Debolezza

| # | Punto di Debolezza | Evidenza |
|---|-------------------|----------|
| 1 | Inflazione requisiti | *"62 requisiti inventati"* — JavaBrew |
| 2 | Analisi architetturale generica | *"Componenti specifici: Generici"* — JavaBrew |
| 3 | Assenza sistema di scoring | *"Voto finale: N/A"* — JavaBrew |
| 4 | Pattern secondari non rilevati | *"Pattern mancanti: Observer"* — Library |
| 5 | Raccomandazioni fuori contesto | *"API REST non applicabile (app desktop)"* — Library |

### 10.2 Multi-Agent

#### Punti di Forza

| # | Punto di Forza | Evidenza |
|---|----------------|----------|
| 1 | Analisi architetturale dettagliata | *"Componenti specifici: UserController, MachineController, etc."* — JavaBrew |
| 2 | Sistema di scoring presente | *"Fornisce un voto finale (22/30)"* — JavaBrew |
| 3 | Formato report professionale | *"Tabelle di tracciabilità dettagliate, Executive summary chiaro"* — JavaBrew |
| 4 | Classificazione qualità requisiti | *"Well-defined, Needs detail, Vague"* — JavaBrew |
| 5 | Minor inflazione requisiti | *"45 REQ vs 62 REQ del Single-Agent"* — JavaBrew |

#### Punti di Debolezza

| # | Punto di Debolezza | Evidenza |
|---|-------------------|----------|
| 1 | Bassa accuratezza UC | *"Use Case corretti: ~6-7/17 (40%)"* — Library |
| 2 | UC fantasma inventati | *"'Domain Model Definition', 'Testing per Item Type' non esistono"* — Library |
| 3 | Errore conteggio test | *"47 (errato - dovrebbero essere 61)"* — Library |
| 4 | False positive nelle criticità | *"False criticità: 2 su 6 (33%)"* — Library |
| 5 | Score inaffidabile | *"Voto basato su: 45 requisiti auto-generati, 30 use case inflazionati"* — JavaBrew |
| 6 | Comportamento inconsistente | *"3 requisiti in Library vs 45 in JavaBrew"* |

---

## 11. Tabella Riassuntiva Finale

### 11.1 Accuratezza per Categoria

| Categoria | Single-Agent | Multi-Agent | Differenza | Vincitore |
|-----------|--------------|-------------|------------|-----------|
| Requisiti | 90% | 70% | +20% | **Single** |
| Use Case | 72% | 33.5% | +38.5% | **Single** |
| Architettura | 85% | 85% | 0% | Pari |
| Test | 100% | 84.5% | +15.5% | **Single** |
| Tracciabilità | 90% | 50% | +40% | **Single** |
| Criticità valide | 100% | 67% | +33% | **Single** |
| **MEDIA** | **89.5%** | **65%** | **+24.5%** | **Single** |

### 11.2 Caratteristiche Qualitative

| Caratteristica | Single-Agent | Multi-Agent | Vincitore |
|----------------|--------------|-------------|-----------|
| Consistenza tra elaborati | Alta | Bassa | **Single** |
| Profondità architettura | Media | Alta | **Multi** |
| Formato presentazione | Standard | Professionale | **Multi** |
| Sistema di scoring | Assente | Presente (inaffidabile) | Pari |
| Raccomandazioni | Generiche | Strutturate | **Multi** |

---

## 12. Matrice di Affidabilità per Sezione

| Sezione Report | Single-Agent | Multi-Agent | Note |
|----------------|--------------|-------------|------|
| Architettura | MEDIA | **ALTA** | Multi-Agent più dettagliato |
| Pattern Design | MEDIA | **ALTA** | Multi-Agent analizza componenti |
| Requisiti | MEDIA | **BASSA** | Entrambi inflazionano, SA più consistente |
| Use Cases | **ALTA** | BASSA | SA 100% su Library |
| Test | **ALTA** | MEDIA | SA 100% accuratezza JavaBrew |
| Tracciabilità | **ALTA** | BASSA | SA 90% vs MA 50% |
| Gap/Criticità | MEDIA | **BASSA** | MA 33% false positive |
| Voto Finale | N/A | **BASSA** | Basato su dati inaffidabili |

---

## 13. Limitazioni Comuni

### 13.1 Problema dell'Inflazione degli Artefatti

Entrambi gli approcci tendono a generare artefatti non presenti nel documento originale:

> *"Problema Fondamentale Condiviso: Entrambi gli approcci: 1) Inventano una struttura formale di requisiti che non esiste, 2) Valutano il documento contro questa struttura auto-generata"*
> — Confronto JavaBrew, Sezione 6

### 13.2 Trattamento Errato delle Feature Future

> *"Trattano feature future (offline operation) come gap critici"*
> *"Applicano metodologia enterprise a un progetto accademico"*
> — Confronto JavaBrew, Sezione 6

### 13.3 Metriche Non Verificabili

> *"Distribuzione Test Non Verificabile: Afferma 71% unit, 19% integration, 10% system ma il documento originale non classifica i test per tipo"*
> — Confronto JavaBrew, Sezione 4.5

---

## 14. Raccomandazioni per l'Utilizzo

### 14.1 Quando Preferire Single-Agent

| Scenario | Motivazione | Evidenza |
|----------|-------------|----------|
| Conteggio artefatti accurato | 100% accuratezza test | *"Match con documento: 100%"* |
| Estrazione Use Case | 72% vs 33.5% | *"UC corretti: 13/13"* |
| Tracciabilità affidabile | +40% accuratezza | *"Accuratezza: 90%"* |
| Documenti accademici | Meno false positive | *"False criticità: 0"* |
| Validazione rapida | Output più snello | *"Analisi più diretta"* |

### 14.2 Quando Preferire Multi-Agent

| Scenario | Motivazione | Evidenza |
|----------|-------------|----------|
| Analisi architetturale profonda | Dettaglio componenti | *"UserController, MachineController, etc."* |
| Report per stakeholder | Formato professionale | *"Executive summary chiaro"* |
| Classificazione qualità | Categorizzazione presente | *"Well-defined, Needs detail, Vague"* |
| Raccomandazioni strutturate | Suggerimenti dettagliati | *"Aggregate Root, Domain Events"* |

**Nota**: Il Multi-Agent richiede sempre verifica manuale dei dati numerici.

---

## 15. Conclusioni

### 15.1 Risultati Principali

1. **Accuratezza complessiva**: Il Single-Agent supera il Multi-Agent di 24.5 punti percentuali (89.5% vs 65%)

2. **Use Case**: Differenza più significativa (+38.5%), con il Single-Agent che raggiunge il 100% di accuratezza su Library

3. **Architettura**: Unica categoria con parità (85%), dove il Multi-Agent offre maggiore profondità di analisi

4. **Affidabilità**: Il Single-Agent non produce false positive nelle criticità, mentre il Multi-Agent ha un tasso del 33%

5. **Consistenza**: Il Single-Agent mostra comportamento più prevedibile tra elaborati diversi

### 15.2 Trade-off Fondamentale

> *"Single-Agent: Ideale per analisi rapida dove l'accuratezza del conteggio test è prioritaria"*
> *"Multi-Agent: Ideale per analisi dove l'aspetto architetturale è prioritario e si accetta un trade-off sull'accuratezza dei conteggi"*
> — Confronto JavaBrew, Sezione 8

### 15.3 Verdetto Finale

| Criterio | Vincitore |
|----------|-----------|
| Accuratezza numerica | **Single-Agent** |
| Profondità architetturale | **Multi-Agent** |
| Affidabilità complessiva | **Single-Agent** |
| Presentazione report | **Multi-Agent** |
| Uso senza verifica manuale | **Single-Agent** |

**Conclusione**: Il **Single-Agent** è l'approccio più affidabile per validazione automatizzata dove l'accuratezza dei dati è prioritaria. Il **Multi-Agent** eccelle nella presentazione e nell'analisi architetturale, ma richiede verifica manuale sistematica dei risultati numerici.

---

## Appendice A: Riferimenti ai Documenti Originali

| Documento | Percorso |
|-----------|----------|
| Confronto JavaBrew | `confronto/javabrew/confronto_single_vs_multi_agent.md` |
| Confronto Library | `confronto/library/confronto_single_vs_multi_agent.md` |

## Appendice B: Metodologia di Calcolo

### Accuratezza Media Single-Agent
```
(90 + 72 + 85 + 100 + 90 + 100) / 6 = 89.5%
```

### Accuratezza Media Multi-Agent
```
(70 + 33.5 + 85 + 84.5 + 50 + 67) / 6 = 65%
```

### Accuratezza UC Media
- Single-Agent: (44% + 100%) / 2 = 72%
- Multi-Agent: (27% + 40%) / 2 = 33.5%

### Accuratezza Test Media Multi-Agent
- (92% + 77%) / 2 = 84.5%

---

*Documento generato per tesi accademica*
*Data: 3 Dicembre 2025*
*Basato su analisi comparativa di due elaborati software*