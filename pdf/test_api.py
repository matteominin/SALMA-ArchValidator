"""
Script di test per l'API PDF Text Extractor.
Esempio pratico di come utilizzare tutti gli endpoint.
"""

import requests
import os


API_BASE_URL = "http://localhost:5000"


def test_health_check():
    """Test dell'endpoint health check."""
    print("=== Test Health Check ===")
    response = requests.get(f"{API_BASE_URL}/health")
    print(f"Status Code: {response.status_code}")
    print(f"Response: {response.json()}\n")


def test_simple_extraction(pdf_path: str):
    """Test estrazione semplice."""
    print("=== Test Estrazione Semplice ===")

    if not os.path.exists(pdf_path):
        print(f"File non trovato: {pdf_path}")
        print("Crea un file PDF di test prima di eseguire questo script.\n")
        return

    with open(pdf_path, 'rb') as f:
        files = {'file': f}
        data = {'placeholder': '[IMMAGINE]'}

        response = requests.post(f"{API_BASE_URL}/extract", files=files, data=data)

        print(f"Status Code: {response.status_code}")
        if response.status_code == 200:
            result = response.json()
            print(f"Filename: {result['filename']}")
            print(f"Placeholder usato: {result['placeholder_used']}")
            print(f"Testo estratto (primi 500 caratteri):")
            print(f"{result['text'][:500]}...\n")
        else:
            print(f"Errore: {response.json()}\n")


def test_detailed_extraction(pdf_path: str):
    """Test estrazione dettagliata."""
    print("=== Test Estrazione Dettagliata ===")

    if not os.path.exists(pdf_path):
        print(f"File non trovato: {pdf_path}")
        print("Crea un file PDF di test prima di eseguire questo script.\n")
        return

    with open(pdf_path, 'rb') as f:
        files = {'file': f}
        data = {'detailed': 'true'}

        response = requests.post(f"{API_BASE_URL}/extract", files=files, data=data)

        print(f"Status Code: {response.status_code}")
        if response.status_code == 200:
            result = response.json()
            data = result['data']
            print(f"Filename: {result['filename']}")
            print(f"Totale pagine: {data['total_pages']}")
            print(f"Totale immagini: {data['total_images']}")

            if data['pages']:
                first_page = data['pages'][0]
                print(f"\nPagina 1:")
                print(f"  Immagini trovate: {first_page['image_count']}")
                if first_page['images']:
                    for img in first_page['images']:
                        print(f"  - Immagine {img['image_index']}:")
                        print(f"    Coordinate: {img['coordinates']}")
                        print(f"    Dimensioni: {img['dimensions']}")
            print()
        else:
            print(f"Errore: {response.json()}\n")


def test_batch_extraction(pdf_paths: list):
    """Test estrazione batch."""
    print("=== Test Estrazione Batch ===")

    existing_files = [path for path in pdf_paths if os.path.exists(path)]

    if not existing_files:
        print("Nessun file PDF trovato per il test batch.")
        print("Crea alcuni file PDF di test prima di eseguire questo script.\n")
        return

    files = [('files', open(path, 'rb')) for path in existing_files]
    data = {'placeholder': '[IMG]'}

    try:
        response = requests.post(
            f"{API_BASE_URL}/extract-batch",
            files=files,
            data=data
        )

        print(f"Status Code: {response.status_code}")
        if response.status_code == 200:
            result = response.json()
            print(f"Totale file elaborati: {result['total_files']}")
            print(f"Placeholder usato: {result['placeholder_used']}")

            for file_result in result['results']:
                print(f"\n- {file_result['filename']}:")
                print(f"  Success: {file_result['success']}")
                if file_result['success']:
                    text_preview = file_result['text'][:200]
                    print(f"  Testo (preview): {text_preview}...")
                else:
                    print(f"  Errore: {file_result['error']}")
            print()
        else:
            print(f"Errore: {response.json()}\n")

    finally:
        # Chiudi tutti i file aperti
        for _, file_obj in files:
            file_obj.close()


def test_error_cases():
    """Test gestione errori."""
    print("=== Test Gestione Errori ===")

    # Test 1: Nessun file fornito
    print("Test 1: Nessun file fornito")
    response = requests.post(f"{API_BASE_URL}/extract")
    print(f"Status Code: {response.status_code}")
    print(f"Response: {response.json()}\n")

    # Test 2: File non valido (prova con un file di testo)
    print("Test 2: File non PDF")
    files = {'file': ('test.txt', b'contenuto di test', 'text/plain')}
    response = requests.post(f"{API_BASE_URL}/extract", files=files)
    print(f"Status Code: {response.status_code}")
    print(f"Response: {response.json()}\n")

    # Test 3: Endpoint non esistente
    print("Test 3: Endpoint non esistente")
    response = requests.get(f"{API_BASE_URL}/nonexistent")
    print(f"Status Code: {response.status_code}")
    print(f"Response: {response.json()}\n")


def main():
    """Funzione principale per eseguire tutti i test."""
    print("=" * 60)
    print("TEST API PDF TEXT EXTRACTOR")
    print("=" * 60)
    print()

    # Verifica che il server sia in esecuzione
    try:
        test_health_check()
    except requests.exceptions.ConnectionError:
        print("ERRORE: Il server non è raggiungibile.")
        print("Assicurati di aver avviato il server con: python app.py")
        return

    # Definisci i percorsi dei file PDF di test
    # Modifica questi percorsi con i tuoi file PDF
    test_pdf = "test_document.pdf"
    test_pdfs_batch = ["test_document.pdf", "another_document.pdf"]

    # Esegui i test
    test_simple_extraction(test_pdf)
    test_detailed_extraction(test_pdf)
    test_batch_extraction(test_pdfs_batch)
    test_error_cases()

    print("=" * 60)
    print("TEST COMPLETATI")
    print("=" * 60)


if __name__ == "__main__":
    main()
