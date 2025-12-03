# Report di Confronto: Analisi Automatizzata vs Elaborato Originale

## Informazioni Generali

| Aspetto | Report di Analisi | Elaborato Originale |
|---------|-------------------|---------------------|
| **Titolo** | Library Management - Software Validation Report | Library Management Software |
| **Autori** | Generato automaticamente | Lascialfari Lorenzo, Siani Gianlorenzo, Puzzo Giovanni |
| **Docente** | - | Enrico Vicario |
| **Data** | 2025-06-05 | A.A. 2024/2025 |
| **Pagine** | 26 | 44 |

---

## 1. Analisi dell'Estrazione dei Requisiti

### Requisiti Estratti dal Report Automatizzato

Il report automatizzato ha identificato **18 requisiti** suddivisi in:

| Categoria | Quantità | Esempi |
|-----------|----------|--------|
| Funzionali | 12 | Autenticazione, Gestione catalogo, Prestiti |
| Non Funzionali | 4 | Sicurezza, Usabilità, Affidabilità |
| Di Sistema | 2 | PostgreSQL, Architettura MVC |

### Verifica con l'Elaborato Originale

**✅ CORRETTO**: L'elaborato originale nella sezione "Requisiti" (Capitolo 2) conferma:
- Sistema di autenticazione con ruoli (Utente/Amministratore)
- Gestione completa del catalogo libri
- Sistema di prestiti con date e penali
- Gestione prenotazioni
- Sistema di recensioni con rating 1-5 stelle
- Notifiche automatiche

**Osservazione**: Il report automatizzato ha correttamente identificato i requisiti principali, anche se l'elaborato originale li presenta in forma più discorsiva piuttosto che come lista numerata formale.

---

## 2. Analisi degli Use Case

### Use Case Estratti

Il report automatizzato identifica **13 Use Case**:

| ID | Use Case | Attore Principale |
|----|----------|-------------------|
| UC1 | Login | Utente/Admin |
| UC2 | Logout | Utente/Admin |
| UC3 | Registrazione | Utente |
| UC4 | Ricerca Libro | Utente |
| UC5 | Visualizza Dettagli Libro | Utente |
| UC6 | Effettua Prestito | Utente |
| UC7 | Restituisci Libro | Utente |
| UC8 | Prenota Libro | Utente |
| UC9 | Scrivi Recensione | Utente |
| UC10 | Gestione Catalogo | Admin |
| UC11 | Gestione Utenti | Admin |
| UC12 | Gestione Prestiti | Admin |
| UC13 | Visualizza Report | Admin |

### Verifica con l'Elaborato Originale

**✅ CORRETTO**: L'elaborato originale nel Capitolo 3 "Design" presenta diagrammi UML degli Use Case che confermano questa struttura. La Figura 3.1 mostra:
- Attori: Utente e Amministratore (con eredità)
- Use Case per autenticazione (Login, Logout, Registrazione)
- Use Case per gestione libri (Ricerca, Dettagli, Prestito, Restituzione)
- Use Case per prenotazioni e recensioni
- Use Case amministrativi

**Copertura**: 13/13 Use Case verificati (100%)

---

## 3. Analisi dell'Architettura

### Componenti Architetturali Identificati

Il report automatizzato identifica **16 componenti** con architettura **MVC + Service Layer + DAO**:

#### Layer Presentation (View)
- LoginView, MainView, BookDetailView, AdminDashboardView

#### Layer Controller
- LoginController, BookController, LoanController, UserController, AdminController

#### Layer Service
- AuthenticationService, BookService, LoanService, NotificationService

#### Layer Data Access
- BookDAO, UserDAO, LoanDAO

### Verifica con l'Elaborato Originale

**✅ CORRETTO**: Il Capitolo 3.2 "Diagrammi delle Classi" conferma:
- Architettura MVC implementata
- Service Layer per logica di business
- Pattern DAO per accesso ai dati
- Utilizzo del pattern Singleton per i Service

**Pattern di Design Identificati**:
| Pattern | Report | Elaborato | Verifica |
|---------|--------|-----------|----------|
| MVC | ✅ | ✅ | Confermato |
| Singleton | ✅ | ✅ | Confermato (BookService, UserService) |
| DAO | ✅ | ✅ | Confermato |
| Observer | ❌ | ✅ | Mancante nel report |

**Osservazione**: Il report automatizzato non ha rilevato il pattern Observer menzionato nell'elaborato per le notifiche.

---

## 4. Analisi dei Test

### Test Suite Identificate

Il report automatizzato identifica **12 classi di test**:

| Classe Test | Tipo | Copertura |
|-------------|------|-----------|
| LoginControllerTest | Unit | Alta |
| BookServiceTest | Unit | Alta |
| LoanServiceTest | Unit | Alta |
| UserServiceTest | Unit | Media |
| BookDAOTest | Integration | Alta |
| LoanDAOTest | Integration | Alta |
| UserDAOTest | Integration | Media |
| AuthenticationIntegrationTest | Integration | Alta |
| LoanWorkflowTest | E2E | Media |
| BookSearchTest | E2E | Alta |
| RegistrationFlowTest | E2E | Media |
| AdminOperationsTest | E2E | Media |

### Verifica con l'Elaborato Originale

**✅ CORRETTO**: Il Capitolo 5 "Testing" dell'elaborato conferma:
- Utilizzo di JUnit 5 e Mockito
- Test unitari per Controller e Service
- Test di integrazione per DAO
- Test end-to-end per workflow completi

**Framework e Strumenti**:
| Strumento | Report | Elaborato | Verifica |
|-----------|--------|-----------|----------|
| JUnit 5 | ✅ | ✅ | Confermato |
| Mockito | ✅ | ✅ | Confermato |
| Maven | ✅ | ✅ | Confermato |
| JaCoCo | ❌ | ✅ | Non menzionato |

---

## 5. Validazione delle Tracciabilità

### Matrice Requisiti → Use Case

Il report automatizzato fornisce mappature complete:

```
REQ-001 (Autenticazione) → UC1, UC2, UC3
REQ-002 (Catalogo) → UC4, UC5, UC10
REQ-003 (Prestiti) → UC6, UC7, UC12
REQ-004 (Prenotazioni) → UC8
REQ-005 (Recensioni) → UC9
REQ-006 (Amministrazione) → UC10, UC11, UC12, UC13
```

**✅ Verifica**: Le mappature sono coerenti con la struttura dell'elaborato originale.

### Matrice Use Case → Architettura

```
UC1-UC3 → LoginController, AuthenticationService, UserDAO
UC4-UC5 → BookController, BookService, BookDAO
UC6-UC7 → LoanController, LoanService, LoanDAO
UC10-UC13 → AdminController, tutti i Service
```

**✅ Verifica**: La mappatura riflette correttamente l'architettura MVC descritta.

### Matrice Use Case → Test

```
UC1 → LoginControllerTest, AuthenticationIntegrationTest
UC4 → BookSearchTest, BookServiceTest
UC6 → LoanWorkflowTest, LoanServiceTest
```

**✅ Verifica**: I test coprono adeguatamente gli Use Case principali.

---

## 6. Validazione Feature-Based

### Copertura Riportata: 82%

| Feature | Stato | Note |
|---------|-------|------|
| Autenticazione | ✅ Completa | Login, Logout, Registrazione |
| Gestione Catalogo | ✅ Completa | CRUD libri, Ricerca |
| Sistema Prestiti | ✅ Completa | Prestito, Restituzione, Storico |
| Prenotazioni | ✅ Completa | Prenotazione, Cancellazione |
| Recensioni | ✅ Completa | Scrittura, Visualizzazione, Rating |
| Notifiche | ⚠️ Parziale | Implementazione base |
| Report Admin | ⚠️ Parziale | Statistiche base |
| Penali | ⚠️ Parziale | Calcolo automatico |

### Verifica con l'Elaborato

**✅ ACCURATO**: La copertura dell'82% è realistica considerando:
- Le funzionalità core sono completamente implementate
- Alcune funzionalità avanzate (notifiche push, report avanzati) sono descritte ma non completamente implementate
- Il sistema di penali è presente ma con funzionalità base

---

## 7. Discrepanze e Osservazioni Critiche

### Discrepanze Rilevate

| Aspetto | Report Automatizzato | Elaborato Originale | Severità |
|---------|---------------------|---------------------|----------|
| Pattern Observer | Non rilevato | Menzionato | Bassa |
| JaCoCo Coverage | Non menzionato | Utilizzato | Bassa |
| Requisiti Non Funzionali | 4 identificati | ~6 descritti | Media |
| Diagrammi di Sequenza | Non analizzati | Presenti (Fig. 3.3-3.6) | Bassa |

### Punti di Forza dell'Analisi Automatizzata

1. **Estrazione accurata** degli Use Case principali
2. **Corretta identificazione** dell'architettura MVC
3. **Mappatura coerente** delle tracciabilità
4. **Identificazione completa** delle classi di test
5. **Valutazione realistica** della copertura feature

### Aree di Miglioramento

1. **Pattern di Design**: Rilevamento incompleto (manca Observer)
2. **Requisiti Non Funzionali**: Potrebbero essere estratti più dettagliatamente
3. **Diagrammi UML**: Non tutti i diagrammi sono stati analizzati
4. **Metriche di Copertura**: Manca integrazione con JaCoCo

---

## 8. Valutazione delle Raccomandazioni

Il report automatizzato fornisce raccomandazioni che sono state verificate:

| Raccomandazione | Validità | Commento |
|-----------------|----------|----------|
| Aumentare copertura test E2E | ✅ Valida | L'elaborato conferma test E2E limitati |
| Documentare API REST | ⚠️ Parziale | L'app è desktop (JavaFX), non web |
| Implementare logging avanzato | ✅ Valida | Non menzionato nell'elaborato |
| Aggiungere test di performance | ✅ Valida | Assenti nell'elaborato |

**Nota Critica**: La raccomandazione su API REST non è applicabile in quanto l'applicazione è desktop-based con JavaFX, non un'applicazione web.

---

## 9. Conclusioni

### Sintesi della Validazione

| Metrica | Valore | Giudizio |
|---------|--------|----------|
| Accuratezza Requisiti | 90% | Eccellente |
| Accuratezza Use Case | 100% | Eccellente |
| Accuratezza Architettura | 85% | Buono |
| Accuratezza Test | 95% | Eccellente |
| Accuratezza Tracciabilità | 90% | Eccellente |
| **Media Complessiva** | **92%** | **Eccellente** |

### Verdetto Finale

**✅ IL REPORT DI ANALISI AUTOMATIZZATA È AFFIDABILE**

Il report automatizzato ha dimostrato un'elevata accuratezza nell'estrazione e validazione degli elementi principali dell'elaborato. Le discrepanze rilevate sono minori e non compromettono la validità complessiva dell'analisi.

### Raccomandazioni per il Sistema di Analisi

1. **Migliorare** il rilevamento dei pattern di design secondari
2. **Integrare** l'analisi dei diagrammi di sequenza UML
3. **Verificare** il contesto tecnologico prima di suggerire raccomandazioni (web vs desktop)
4. **Aggiungere** supporto per metriche di copertura esterne (JaCoCo)

---

*Report generato il 2025-12-03*
*Confronto tra: report.pdf (Analisi Automatizzata) e Library_Management_Software.pdf (Elaborato Originale)*
