# PDF Text Extractor API

Un servizio REST API per estrarre testo da file PDF sostituendo le immagini con placeholder personalizzabili.

## Caratteristiche

- Estrazione testo da PDF con PyMuPDF
- Sostituzione automatica delle immagini con placeholder
- Supporto per placeholder personalizzati
- Modalità dettagliata con informazioni su coordinate e dimensioni delle immagini
- Elaborazione batch di più file PDF
- API REST semplice e documentata

## Installazione

```bash
pip install -r requirements.txt
```

## Avvio del server

```bash
python app.py
```

Il server sarà disponibile su `http://localhost:5000`

## Endpoints API

### 1. Health Check

Verifica lo stato del servizio.

**Richiesta:**
```bash
GET /health
```

**Risposta:**
```json
{
  "status": "ok",
  "service": "PDF Text Extractor",
  "version": "1.0.0"
}
```

### 2. Estrazione Testo Semplice

Estrae il testo da un PDF sostituendo le immagini con placeholder.

**Richiesta:**
```bash
POST /extract
Content-Type: multipart/form-data

file: [file PDF]
placeholder: "[IMG PLACEHOLDER]" (opzionale)
detailed: "false" (opzionale)
```

**Esempio con curl:**
```bash
curl -X POST http://localhost:5000/extract \
  -F "file=@documento.pdf" \
  -F "placeholder=[IMMAGINE]"
```

**Risposta:**
```json
{
  "success": true,
  "filename": "documento.pdf",
  "placeholder_used": "[IMMAGINE]",
  "text": "Testo estratto dal PDF...\n[IMMAGINE]\nAltro testo..."
}
```

### 3. Estrazione Dettagliata

Estrae il testo con informazioni dettagliate sulle immagini trovate.

**Richiesta:**
```bash
curl -X POST http://localhost:5000/extract \
  -F "file=@documento.pdf" \
  -F "detailed=true"
```

**Risposta:**
```json
{
  "success": true,
  "filename": "documento.pdf",
  "placeholder_used": "[IMG PLACEHOLDER]",
  "data": {
    "total_pages": 3,
    "total_images": 5,
    "pages": [
      {
        "page_number": 1,
        "image_count": 2,
        "images": [
          {
            "image_index": 1,
            "coordinates": {
              "x0": 100.5,
              "y0": 200.3,
              "x1": 400.8,
              "y1": 500.2
            },
            "dimensions": {
              "width": 300.3,
              "height": 299.9
            }
          }
        ],
        "text": "Testo della pagina 1..."
      }
    ],
    "full_text": "Testo completo di tutte le pagine..."
  }
}
```

### 4. Estrazione Batch

Elabora più file PDF contemporaneamente.

**Richiesta:**
```bash
curl -X POST http://localhost:5000/extract-batch \
  -F "files=@documento1.pdf" \
  -F "files=@documento2.pdf" \
  -F "placeholder=[IMG]"
```

**Risposta:**
```json
{
  "success": true,
  "placeholder_used": "[IMG]",
  "total_files": 2,
  "results": [
    {
      "filename": "documento1.pdf",
      "success": true,
      "text": "Testo estratto..."
    },
    {
      "filename": "documento2.pdf",
      "success": true,
      "text": "Altro testo estratto..."
    }
  ]
}
```

## Gestione Errori

L'API restituisce messaggi di errore descrittivi:

### File non fornito (400)
```json
{
  "error": "Nessun file fornito",
  "message": "È necessario caricare un file PDF con il campo 'file'"
}
```

### Tipo file non valido (400)
```json
{
  "error": "Tipo file non valido",
  "message": "È consentito solo il caricamento di file PDF"
}
```

### File troppo grande (413)
```json
{
  "error": "File troppo grande",
  "message": "Il file supera la dimensione massima consentita di 16MB"
}
```

### Errore durante l'estrazione (500)
```json
{
  "error": "Errore durante l'estrazione",
  "message": "Descrizione dell'errore..."
}
```

## Esempio di Utilizzo in Python

```python
import requests

# Estrazione semplice
with open('documento.pdf', 'rb') as f:
    files = {'file': f}
    data = {'placeholder': '[IMMAGINE]'}
    response = requests.post('http://localhost:5000/extract', files=files, data=data)
    result = response.json()
    print(result['text'])

# Estrazione dettagliata
with open('documento.pdf', 'rb') as f:
    files = {'file': f}
    data = {'detailed': 'true'}
    response = requests.post('http://localhost:5000/extract', files=files, data=data)
    result = response.json()
    print(f"Totale immagini: {result['data']['total_images']}")
    print(f"Totale pagine: {result['data']['total_pages']}")
```

## Esempio di Utilizzo in JavaScript

```javascript
// Estrazione semplice
const formData = new FormData();
formData.append('file', pdfFile);
formData.append('placeholder', '[IMG]');

fetch('http://localhost:5000/extract', {
  method: 'POST',
  body: formData
})
  .then(response => response.json())
  .then(data => {
    console.log('Testo estratto:', data.text);
  });

// Estrazione dettagliata
const formDataDetailed = new FormData();
formDataDetailed.append('file', pdfFile);
formDataDetailed.append('detailed', 'true');

fetch('http://localhost:5000/extract', {
  method: 'POST',
  body: formDataDetailed
})
  .then(response => response.json())
  .then(data => {
    console.log('Totale immagini:', data.data.total_images);
    console.log('Pagine:', data.data.pages);
  });
```

## Configurazione

È possibile modificare le seguenti impostazioni in `app.py`:

- `MAX_CONTENT_LENGTH`: Dimensione massima del file (default: 16MB)
- `ALLOWED_EXTENSIONS`: Estensioni file permesse (default: {'pdf'})
- `host` e `port`: Indirizzo e porta del server (default: 0.0.0.0:5000)

## Dipendenze

- Flask 3.0.0 - Framework web
- PyMuPDF 1.23.8 - Libreria per elaborazione PDF
- Pillow 10.1.0 - Elaborazione immagini

## Architettura

```
pdf/
├── app.py              # API Flask con endpoints REST
├── pdf_extractor.py    # Logica di estrazione PDF
├── requirements.txt    # Dipendenze Python
└── README.md          # Documentazione
```

## Note Tecniche

### Come funziona la sostituzione delle immagini

1. PyMuPDF identifica tutte le immagini nel PDF tramite `page.get_images()`
2. Per ogni immagine, vengono recuperate le coordinate con `page.get_image_rects(xref)`
3. Le coordinate includono:
   - `x0, y0`: Angolo superiore sinistro
   - `x1, y1`: Angolo inferiore destro
   - `width, height`: Dimensioni dell'immagine
4. I placeholder vengono inseriti nel testo estratto in base alla posizione delle immagini

### Limitazioni

- I placeholder vengono aggiunti alla fine del testo di ogni pagina
- Per un posizionamento più preciso, sarebbe necessario analizzare le coordinate del testo
- File molto grandi potrebbero richiedere tempo di elaborazione

## Licenza

MIT
