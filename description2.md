# Workflow di Analisi del Documento

Questo documento descrive la pipeline di elaborazione che analizza e verifica i report di progetto, garantendo che le informazioni siano estratte in modo coerente e accurato.

## 1. Suddivisione del Documento

Il processo inizia suddividendo il documento sorgente in sezioni logiche. Ogni sezione viene etichettata in base al suo contenuto (es. **Requisiti Funzionali**, **Casi d'Uso**, **Dettagli Architettonici**). Questo passo è cruciale per indirizzare le informazioni corrette all'agente specializzato appropriato.

## 2. Estrazione Specializzata

Ogni sezione viene inviata a un agente LLM dedicato, ciascuno con un compito specifico:

- **Agente Requisiti**: Estrae tutti i requisiti, sia quelli esplicitamente elencati che quelli impliciti. Valuta la chiarezza e la qualità di ogni requisito.
- **Agente Casi d'Uso**: Analizza le descrizioni per identificare i casi d'uso. Questo agente è in grado di estrarre anche casi d'uso impliciti e di inferire i ruoli degli attori coinvolti.
- **Agente Architettura**: Identifica il pattern architetturale, estrae i componenti, le loro responsabilità e le relazioni di comunicazione, anche se non esplicitamente definite nel testo.

## 3. Consolidamento e Normalizzazione

Le estrazioni grezze di ogni agente vengono consolidate in un'unica struttura dati. Durante questo passaggio, il linguaggio viene normalizzato (ad esempio, traducendo tutti i flussi di lavoro in un'unica lingua) e vengono risolte le incongruenze di base.

## 4. Analisi di Verifica e Qualità

Questa è la fase più critica del workflow. Un agente di verifica analizza il report consolidato per:

- **Verifica della Coerenza**: Controlla la logica e la consistenza dei dati, assicurandosi che non ci siano incongruenze tra requisiti, casi d'uso e componenti.
- **Validazione della Tracciabilità**: Assicura che ogni requisito sia correttamente tracciato su uno o più casi d'uso e componenti dell'architettura. Questo include l'identificazione di lacune logiche.
- **Identificazione di Ambiguità**: Segnala eventuali responsabilità di componenti non chiare o flussi di comunicazione mancanti che potrebbero compromettere l'integrità del progetto.

## 5. Generazione del Report Finale

Infine, il report di verifica viene generato in un formato standard (come **JSON**). Questo documento finale fornisce un quadro completo e oggettivo del progetto, evidenziando punti di forza e debolezze critiche che necessitano di ulteriore attenzione.
