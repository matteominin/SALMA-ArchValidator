from flask import Flask, request, jsonify
import os
import tempfile
from pdf_service import PdfService
from models import PdfIndex, Section
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

app = Flask(__name__)

# Configurazione
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16MB max file size
ALLOWED_EXTENSIONS = {'pdf'}


def allowed_file(filename: str) -> bool:
    """Verifica se il file ha un'estensione permessa."""
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


@app.route('/health', methods=['GET'])
def health_check():
    """Endpoint per verificare lo stato del servizio."""
    return jsonify({
        'status': 'ok',
        'service': 'PDF Text Extractor',
        'version': '2.0.0'
    })


@app.route('/extract', methods=['POST'])
def extract():
    """
    Estrae testo e immagini dal PDF.

    Supporta diversi modalità di estrazione tramite parametri opzionali.

    Richiesta:
        - file: File PDF (multipart/form-data) [obbligatorio]
        - mode: Modalità di estrazione (opzionale, default: "full")
            - "full": Estrae tutto il testo + immagini (con OpenAI Vision in parallelo)
            - "pages": Estrae solo da un intervallo di pagine
            - "sections": Estrae sezioni specifiche basate su indice (NO immagini)

        - save_images: Se "true", salva le immagini processate su disco (opzionale, default: "false")
        - output_folder: Cartella dove salvare le immagini (opzionale, default: "extracted_images")
        - use_placeholder: Se "true", usa placeholder invece di OpenAI Vision (opzionale, default: "false")
        - placeholder_text: Testo del placeholder (opzionale, default: "[IMAGE]")

        Parametri per mode="pages":
        - start_page: Pagina di inizio (1-based)
        - end_page: Pagina di fine (1-based, opzionale, default: -1 per fino alla fine)

        Parametri per mode="sections":
        - index: JSON con l'indice delle sezioni
          Formato: {
              "sections": [
                  {"section": "Titolo Sezione 1", "start": 1, "end": -1},
                  {"section": "Titolo Sezione 2", "start": 5, "end": -1}
              ]
          }

    Risposta:
        JSON con il testo estratto e metadata
    """
    # Verifica file
    if 'file' not in request.files:
        return jsonify({
            'error': 'Nessun file fornito',
            'message': 'È necessario caricare un file PDF con il campo "file"'
        }), 400

    file = request.files['file']

    if file.filename == '':
        return jsonify({
            'error': 'Nome file vuoto',
            'message': 'Il file caricato non ha un nome valido'
        }), 400

    if not allowed_file(file.filename):
        return jsonify({
            'error': 'Tipo file non valido',
            'message': 'È consentito solo il caricamento di file PDF'
        }), 400

    # Parametri
    mode = request.form.get('mode', 'full').lower()
    save_images = request.form.get('save_images', 'false').lower() == 'true'
    output_folder = request.form.get('output_folder', 'extracted_images')
    use_placeholder = request.form.get('use_placeholder', 'false').lower() == 'true'
    placeholder_text = request.form.get('placeholder_text', '[IMAGE]')

    temp_file = None
    try:
        # Salva il file temporaneamente
        with tempfile.NamedTemporaryFile(delete=False, suffix='.pdf') as temp_file:
            file.save(temp_file.name)
            temp_path = temp_file.name

        openai_api_key = os.getenv('OPENAI_API_KEY')

        pdf_service = PdfService(
            use_image_extraction=True,
            openai_api_key=openai_api_key,
            save_images=save_images,
            output_folder=output_folder,
            placeholder_mode=use_placeholder,
            placeholder_text=placeholder_text
        )

        # MODE: FULL - Estrae tutto (testo + immagini in parallelo)
        if mode == 'full':
            text = pdf_service.extract_text(temp_path)

            return jsonify({
                'success': True,
                'filename': file.filename,
                'mode': 'full',
                'text': text
            }), 200

        # MODE: PAGES - Estrae da intervallo di pagine
        elif mode == 'pages':
            try:
                start_page = int(request.form.get('start_page', 1))
                end_page = int(request.form.get('end_page', -1))
            except ValueError:
                return jsonify({
                    'error': 'Parametri non validi',
                    'message': 'start_page e end_page devono essere numeri interi'
                }), 400

            text = pdf_service.extract_pages(temp_path, start_page, end_page)

            return jsonify({
                'success': True,
                'filename': file.filename,
                'mode': 'pages',
                'start_page': start_page,
                'end_page': end_page,
                'text': text
            }), 200

        # MODE: SECTIONS - Estrae sezioni basate su indice (NO immagini)
        elif mode == 'sections':
            if 'index' not in request.form:
                return jsonify({
                    'error': 'Indice mancante',
                    'message': 'È necessario fornire un indice delle sezioni per mode=sections'
                }), 400

            try:
                import json
                index_data = json.loads(request.form['index'])

                # Converti in oggetti Python
                sections = [
                    Section(
                        section=s['section'],
                        start=s['start'],
                        end=s.get('end', -1)
                    )
                    for s in index_data['sections']
                ]
                index = PdfIndex(sections=sections)

            except (json.JSONDecodeError, KeyError, ValueError) as e:
                return jsonify({
                    'error': 'Formato indice non valido',
                    'message': f'Errore nel parsing dell\'indice: {str(e)}'
                }), 400

            # Usa servizio senza estrazione immagini per le sezioni
            pdf_service_no_images = PdfService(use_image_extraction=False)
            extracted_sections = pdf_service_no_images.extract_sections(temp_path, index)

            # Converti in dict per JSON
            result = [
                {
                    'section': sec.section,
                    'text': sec.text
                }
                for sec in extracted_sections
            ]

            return jsonify({
                'success': True,
                'filename': file.filename,
                'mode': 'sections',
                'sections': result
            }), 200

        else:
            return jsonify({
                'error': 'Modalità non valida',
                'message': f'Modalità "{mode}" non supportata. Usa: full, pages, sections'
            }), 400

    except Exception as e:
        return jsonify({
            'error': 'Errore durante l\'estrazione',
            'message': str(e)
        }), 500

    finally:
        # Rimuovi il file temporaneo
        if temp_file and os.path.exists(temp_path):
            try:
                os.unlink(temp_path)
            except Exception:
                pass


@app.errorhandler(413)
def request_entity_too_large(error):
    """Gestisce errori di file troppo grandi."""
    return jsonify({
        'error': 'File troppo grande',
        'message': 'Il file supera la dimensione massima consentita di 16MB'
    }), 413


@app.errorhandler(404)
def not_found(error):
    """Gestisce errori 404."""
    return jsonify({
        'error': 'Endpoint non trovato',
        'message': 'L\'endpoint richiesto non esiste'
    }), 404


@app.errorhandler(500)
def internal_error(error):
    """Gestisce errori interni del server."""
    return jsonify({
        'error': 'Errore interno del server',
        'message': 'Si è verificato un errore imprevisto'
    }), 500


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5001)
