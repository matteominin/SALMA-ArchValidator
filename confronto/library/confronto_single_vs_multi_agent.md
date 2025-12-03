# Confronto Single Agent vs Multi Agent
## Analisi del Sistema Library Management

---

## Sommario Esecutivo

Questo report confronta le prestazioni di due approcci di analisi automatizzata basati su LLM per la validazione di elaborati software:

| Sistema | Accuratezza Complessiva | Verdetto |
|---------|------------------------|----------|
| **Single Agent** | **92%** | ✅ Affidabile |
| **Multi Agent** | **64%** | ⚠️ Parzialmente Affidabile |

**Vincitore**: Single Agent (+28 punti percentuali)

---

## 1. Confronto dell'Estrazione dei Requisiti

### Single Agent
- **Requisiti estratti**: 18 (12 funzionali, 4 non funzionali, 2 di sistema)
- **Accuratezza**: 90%
- **Approccio**: Estrazione dettagliata e categorizzata

### Multi Agent
- **Requisiti estratti**: 3 (generici ad alto livello)
- **Accuratezza**: 70% (ma superficiale)
- **Approccio**: Estrazione minimalista

### Analisi Comparativa

| Aspetto | Single Agent | Multi Agent | Migliore |
|---------|--------------|-------------|----------|
| Quantità requisiti | 18 | 3 | Single Agent |
| Granularità | Alta | Bassa | Single Agent |
| Categorizzazione | ✅ Presente | ❌ Assente | Single Agent |
| Copertura | Completa | Incompleta | Single Agent |

**Vincitore Sezione**: Single Agent

---

## 2. Confronto dell'Estrazione Use Case

### Single Agent
- **Use Case estratti**: 13
- **Use Case corretti**: 13/13 (100%)
- **Struttura**: Corretta e coerente con l'elaborato

| UC | Descrizione | Verifica |
|----|-------------|----------|
| UC1-UC3 | Autenticazione | ✅ |
| UC4-UC5 | Gestione libri | ✅ |
| UC6-UC8 | Prestiti e prenotazioni | ✅ |
| UC9 | Recensioni | ✅ |
| UC10-UC13 | Amministrazione | ✅ |

### Multi Agent
- **Use Case estratti**: 17
- **Use Case corretti**: ~6-7/17 (40%)
- **Problemi**: UC frammentati e UC "fantasma" inventati

| Problema | Descrizione | Impatto |
|----------|-------------|---------|
| UC fantasma | "Domain Model Definition", "Testing per Item Type" non esistono | Alto |
| Frammentazione | UC singoli divisi in multipli | Alto |
| Mappatura errata | UC-7 nel report ≠ UC-7 nell'elaborato | Alto |

### Analisi Comparativa

| Aspetto | Single Agent | Multi Agent | Migliore |
|---------|--------------|-------------|----------|
| Numero UC | 13 (corretto) | 17 (errato) | Single Agent |
| Accuratezza UC | 100% | 40% | Single Agent |
| UC inventati | 0 | 4+ | Single Agent |
| Mappatura attori | ✅ Corretta | ⚠️ Parziale | Single Agent |

**Vincitore Sezione**: Single Agent (+60%)

---

## 3. Confronto dell'Analisi Architetturale

### Single Agent
- **Componenti identificati**: 16
- **Accuratezza architettura**: 85%
- **Pattern rilevati**: MVC, Singleton, DAO
- **Pattern mancanti**: Observer (per notifiche)

### Multi Agent
- **Componenti identificati**: ~8 principali
- **Accuratezza architettura**: 85% (dichiarata 63%)
- **Pattern rilevati**: MVC, DAO, Service Layer
- **Pattern mancanti**: Singleton esplicito

### Analisi Comparativa

| Aspetto | Single Agent | Multi Agent | Migliore |
|---------|--------------|-------------|----------|
| Componenti trovati | 16 | ~8 | Single Agent |
| Pattern MVC | ✅ | ✅ | Pari |
| Pattern DAO | ✅ | ✅ | Pari |
| Pattern Singleton | ✅ | ⚠️ Parziale | Single Agent |
| Service Layer | ✅ | ✅ | Pari |
| MainService | ❌ Non esplicito | ❌ Non esplicito | Pari |

**Vincitore Sezione**: Single Agent (marginale)

---

## 4. Confronto dell'Analisi dei Test

### Single Agent
- **Classi di test identificate**: 12
- **Accuratezza**: 95%
- **Tipologie**: Unit, Integration, E2E

| Tipo Test | Classi | Copertura |
|-----------|--------|-----------|
| Unit | 4 | Alta |
| Integration | 4 | Alta |
| E2E | 4 | Media |

### Multi Agent
- **Test identificati**: 47 (errato - dovrebbero essere 61)
- **Accuratezza**: 77%
- **Distribuzione dichiarata**: 85% Unit, 8% Integration, 7% System

### Verifica con Elaborato Originale
L'elaborato riporta **61 test** totali (risultati Maven):
```
Tests run: 61, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Analisi Comparativa

| Aspetto | Single Agent | Multi Agent | Migliore |
|---------|--------------|-------------|----------|
| Classi test | 12 (corretto) | Non specificato | Single Agent |
| Conteggio test | Non contati | 47 (errato, reali: 61) | N/A |
| Framework JUnit 5 | ✅ | ✅ | Pari |
| Framework Mockito | ✅ | ✅ | Pari |
| Maven | ✅ | ⚠️ Non menzionato | Single Agent |
| JaCoCo | ❌ Non rilevato | ❌ Non rilevato | Pari |

**Vincitore Sezione**: Single Agent

---

## 5. Confronto delle Tracciabilità

### Single Agent
- **Req → UC**: ✅ Mappature complete e corrette
- **UC → Architettura**: ✅ Corretta
- **UC → Test**: ✅ Corretta
- **Accuratezza tracciabilità**: 90%

### Multi Agent
- **Req → UC**: ⚠️ Mappatura troppo generica (3 req → 17 UC tutti)
- **UC → Architettura**: ⚠️ Basata su UC errati
- **UC → Test**: ⚠️ Basata su UC errati
- **Accuratezza tracciabilità**: 50%

### Analisi Comparativa

| Aspetto | Single Agent | Multi Agent | Migliore |
|---------|--------------|-------------|----------|
| Req → UC | Specifica | Generica | Single Agent |
| UC → Arch | Corretta | Errata (base UC errata) | Single Agent |
| UC → Test | Corretta | Errata | Single Agent |
| Granularità | Alta | Bassa | Single Agent |

**Vincitore Sezione**: Single Agent (+40%)

---

## 6. Confronto delle Criticità Rilevate

### Single Agent - Criticità Riportate
| Criticità | Validità |
|-----------|----------|
| Pattern Observer non rilevato | ✅ Valida |
| JaCoCo non menzionato | ✅ Valida |
| Requisiti NFR incompleti | ✅ Valida |
| Diagrammi di sequenza non analizzati | ✅ Valida |

**False criticità**: 0

### Multi Agent - Criticità Riportate
| Criticità | Validità |
|-----------|----------|
| Use Case Implementation Gap | ⚠️ Basata su UC errati |
| Vague Component Responsibilities | ❌ Falsa - elaborato documenta chiaramente |
| Incomplete Test Coverage | ⚠️ Discutibile - 61 test passati |
| Undefined Architectural Pattern | ❌ Falsa - MVC/DAO/Singleton documentati |
| Missing Offline Resilience | ✅ Valida |
| No Domain Events Infrastructure | ✅ Valida |

**False criticità**: 2 su 6 (33%)

### Analisi Comparativa

| Aspetto | Single Agent | Multi Agent | Migliore |
|---------|--------------|-------------|----------|
| Criticità totali | 4 | 6 | - |
| Criticità valide | 4 (100%) | 4 (67%) | Single Agent |
| False criticità | 0 | 2 | Single Agent |
| Utilità feedback | Alta | Media | Single Agent |

**Vincitore Sezione**: Single Agent

---

## 7. Confronto Valutazione/Punteggio

### Single Agent
- **Copertura feature dichiarata**: 82%
- **Approccio**: Feature-based validation
- **Giudizio**: Realistico e giustificato

### Multi Agent
- **Punteggio assegnato**: 18/30
- **Punteggio suggerito (post-verifica)**: ~23/30
- **Approccio**: Scoring multi-criterio
- **Problema**: Sottostima significativa (-5 punti)

### Analisi Comparativa

| Aspetto | Single Agent | Multi Agent | Migliore |
|---------|--------------|-------------|----------|
| Metodo valutazione | Feature-based | Multi-criterio | Preferenza soggettiva |
| Accuratezza giudizio | Alta | Bassa (sottostima) | Single Agent |
| Giustificazione | Dettagliata | Presente ma errata | Single Agent |

**Vincitore Sezione**: Single Agent

---

## 8. Tabella Riassuntiva Accuratezze

| Metrica | Single Agent | Multi Agent | Differenza |
|---------|--------------|-------------|------------|
| **Accuratezza Requisiti** | 90% | 70% | +20% |
| **Accuratezza Use Case** | 100% | 40% | **+60%** |
| **Accuratezza Architettura** | 85% | 85% | 0% |
| **Accuratezza Test** | 95% | 77% | +18% |
| **Accuratezza Tracciabilità** | 90% | 50% | **+40%** |
| **MEDIA COMPLESSIVA** | **92%** | **64%** | **+28%** |

---

## 9. Punti di Forza e Debolezza

### Single Agent

#### Punti di Forza
1. ✅ Estrazione Use Case perfetta (100%)
2. ✅ Requisiti ben categorizzati e dettagliati
3. ✅ Tracciabilità accurate e specifiche
4. ✅ Nessuna criticità falsa
5. ✅ Identificazione corretta dei framework di test

#### Punti di Debolezza
1. ⚠️ Pattern Observer non rilevato
2. ⚠️ Metriche JaCoCo non integrate
3. ⚠️ Diagrammi di sequenza non analizzati
4. ⚠️ Raccomandazione API REST non applicabile (app desktop)

### Multi Agent

#### Punti di Forza
1. ✅ Architettura MVC + Service Layer + DAO corretta
2. ✅ Buona estrazione del Domain Model
3. ✅ Framework testing identificati
4. ✅ Raccomandazioni resilienza pertinenti

#### Punti di Debolezza
1. ❌ Use Case errati (17 vs 13, 40% accuratezza)
2. ❌ Use Case "fantasma" inventati
3. ❌ Requisiti troppo generici (solo 3)
4. ❌ Conteggio test errato (47 vs 61)
5. ❌ False criticità (33%)
6. ❌ Punteggio sottostimato (18/30 vs ~23/30 suggerito)

---

## 10. Conclusioni

### Verdetto Finale

| Criterio | Single Agent | Multi Agent | Vincitore |
|----------|--------------|-------------|-----------|
| Accuratezza complessiva | 92% | 64% | **Single Agent** |
| Estrazione requisiti | Eccellente | Insufficiente | **Single Agent** |
| Estrazione Use Case | Eccellente | Insufficiente | **Single Agent** |
| Analisi architettura | Buono | Buono | Pari |
| Analisi test | Eccellente | Buono | **Single Agent** |
| Tracciabilità | Eccellente | Sufficiente | **Single Agent** |
| Affidabilità criticità | 100% | 67% | **Single Agent** |

### 🏆 VINCITORE: SINGLE AGENT

Il sistema **Single Agent** ha dimostrato prestazioni superiori in quasi tutti gli aspetti dell'analisi:

- **+28%** di accuratezza complessiva
- **+60%** nell'estrazione degli Use Case
- **+40%** nelle tracciabilità
- **0%** di false criticità vs 33%

### Raccomandazioni

#### Per il Sistema Multi Agent
1. **Priorità Alta**: Correggere l'estrazione degli Use Case - non inventare UC e non frammentare quelli esistenti
2. **Priorità Alta**: Aumentare la granularità dell'estrazione requisiti
3. **Priorità Media**: Verificare criticità prima di riportarle
4. **Priorità Media**: Allineare conteggio test con risultati Maven
5. **Priorità Bassa**: Calibrare il sistema di punteggio

#### Per il Sistema Single Agent
1. **Priorità Media**: Aggiungere rilevamento pattern Observer e altri pattern secondari
2. **Priorità Media**: Integrare analisi metriche di copertura (JaCoCo)
3. **Priorità Bassa**: Verificare contesto tecnologico per raccomandazioni (web vs desktop)

---

## Appendice: Dati Grezzi

### Elaborato Originale
- **Titolo**: Library Management Application
- **Autori**: Luca Lascialfari, Marco Siani, Tommaso Puzzo
- **Use Case documentati**: 13
- **Test documentati**: 61 (tutti passati)
- **Architettura**: MVC + Singleton + DAO

### Report Single Agent
- **Titolo**: Library Management - Software Validation Report
- **Data**: 2025-06-05
- **Pagine**: 26
- **Requisiti estratti**: 18
- **Use Case estratti**: 13
- **Test identificati**: 12 classi

### Report Multi Agent
- **Titolo**: Architectural Blueprint Validation Report
- **Data**: November 29, 2025
- **Pagine**: 17
- **Requisiti estratti**: 3
- **Use Case estratti**: 17
- **Test identificati**: 47

---

*Report di confronto generato il 2025-12-03*
*Confronto tra sistemi di analisi automatizzata per validazione elaborati software*
