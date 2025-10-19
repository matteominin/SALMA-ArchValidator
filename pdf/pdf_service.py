import fitz  # PyMuPDF
import re
import os
import logging
import asyncio
import concurrent.futures
from typing import List, Optional
from pathlib import Path
from models import PdfIndex, ExtractedSection
from pdf_extractor import PDFImageExtractor

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class PdfService:
    """Servizio per l'estrazione di testo da PDF."""

    def __init__(self, use_image_extraction: bool = True, openai_api_key: Optional[str] = None,
                 save_images: bool = False, output_folder: str = "extracted_images",
                 placeholder_mode: bool = False, placeholder_text: str = "[IMAGE]"):
        """
        Inizializza il servizio PDF.

        Args:
            use_image_extraction: Se True, usa OpenAI Vision per estrarre testo dalle immagini
            openai_api_key: API key di OpenAI (opzionale)
            save_images: Se True, salva le immagini processate su disco
            output_folder: Cartella dove salvare le immagini
            placeholder_mode: Se True, usa placeholder invece di OpenAI Vision
            placeholder_text: Testo del placeholder (default: "[IMAGE]")
        """
        self.use_image_extraction = use_image_extraction
        self.placeholder_mode = placeholder_mode
        self.placeholder_text = placeholder_text
        self.save_images = save_images
        self.output_folder = output_folder
        self.image_extractor = PDFImageExtractor(openai_api_key) if (use_image_extraction and not placeholder_mode) else None

    def extract_text(self, file_path: str) -> str:
        """
        Estrae tutto il testo dal PDF, includendo il testo dalle immagini inline se abilitato.

        Args:
            file_path: Percorso del file PDF

        Returns:
            Testo estratto dal PDF con immagini integrate inline
        """
        logger.info(f"Starting text extraction for: {file_path}")
        self._validate_file_path(file_path)

        document = None
        try:
            document = self._load_pdf(file_path)
            total_pages = len(document)
            logger.info(f"Processing {total_pages} pages for extraction")

            # Se abilitato, estrai prima tutte le immagini per averle disponibili
            images_by_page = {}
            if self.use_image_extraction:
                if self.placeholder_mode:
                    logger.info("Extracting images with placeholders")
                    images_data = self._extract_images_with_placeholders(file_path)
                else:
                    logger.info("Extracting text from images using OpenAI Vision")
                    images_data = self._extract_images_data(file_path)

                # Organizza le immagini per pagina
                for img_data in images_data:
                    page_num = img_data['page']
                    if page_num not in images_by_page:
                        images_by_page[page_num] = []
                    images_by_page[page_num].append(img_data)

            # Estrai il testo pagina per pagina, integrando le immagini nelle posizioni corrette
            result = []
            for page_num in range(total_pages):
                page = document.load_page(page_num)

                # Estrai i blocchi di testo con coordinate
                text_blocks = page.get_text("blocks")

                # Se ci sono immagini in questa pagina, integrale
                if (page_num + 1) in images_by_page:
                    page_content = self._merge_text_and_images(text_blocks, images_by_page[page_num + 1])
                else:
                    # Se non ci sono immagini, usa solo il testo
                    page_content = page.get_text()

                result.append(page_content)

            final_result = "\n\n".join(result)
            logger.info(f"Completed extraction - total {len(final_result)} characters")

            return final_result

        except Exception as e:
            logger.error(f"Error during text extraction: {e}")
            raise RuntimeError(f"Error during text extraction: {e}")
        finally:
            self._close_document(document)

    def extract_pages(self, file_path: str, start_page: int, end_page: int = -1) -> str:
        """
        Estrae il testo da un intervallo di pagine, includendo il testo dalle immagini inline se abilitato.

        Args:
            file_path: Percorso del file PDF
            start_page: Pagina di inizio (1-based)
            end_page: Pagina di fine (1-based, -1 per fino alla fine)

        Returns:
            Testo estratto dalle pagine specificate con immagini integrate inline
        """
        logger.info(f"Extracting pages {start_page}-{end_page} from PDF: {file_path}")
        self._validate_file_path(file_path)
        self._validate_page_range(start_page, end_page)

        document = None
        try:
            document = self._load_pdf(file_path)
            total_pages = len(document)

            # Converti da 1-based a 0-based
            start_idx = start_page - 1
            end_idx = total_pages if end_page == -1 else min(end_page, total_pages)

            # Se abilitato, estrai prima tutte le immagini per averle disponibili
            images_by_page = {}
            if self.use_image_extraction:
                if self.placeholder_mode:
                    logger.info("Extracting images with placeholders")
                    images_data = self._extract_images_with_placeholders_in_range(file_path, start_idx, end_idx)
                else:
                    logger.info("Extracting text from images using OpenAI Vision")
                    images_data = self._extract_images_data_in_range(file_path, start_idx, end_idx)

                # Organizza le immagini per pagina
                for img_data in images_data:
                    page_num = img_data['page']
                    if page_num not in images_by_page:
                        images_by_page[page_num] = []
                    images_by_page[page_num].append(img_data)

            # Estrai il testo pagina per pagina, integrando le immagini nelle posizioni corrette
            result = []
            for page_num in range(start_idx, end_idx):
                page = document.load_page(page_num)

                # Estrai i blocchi di testo con coordinate
                text_blocks = page.get_text("blocks")

                # Se ci sono immagini in questa pagina, integrale
                if (page_num + 1) in images_by_page:
                    page_content = self._merge_text_and_images(text_blocks, images_by_page[page_num + 1])
                else:
                    # Se non ci sono immagini, usa solo il testo
                    page_content = page.get_text()

                result.append(page_content)

            extracted_text = "\n\n".join(result)
            logger.debug(f"Successfully extracted text from pages {start_page}-{end_page}, length: {len(extracted_text)}")

            return extracted_text

        except Exception as e:
            logger.error(f"Error extracting text from PDF pages {start_page}-{end_page}: {e}")
            raise RuntimeError(f"Error extracting text from PDF pages: {e}")
        finally:
            self._close_document(document)

    def extract_sections(self, file_path: str, index: PdfIndex) -> List[ExtractedSection]:
        """
        Estrae sezioni specifiche dal PDF basandosi su un indice.

        Args:
            file_path: Percorso del file PDF
            index: Indice delle sezioni da estrarre

        Returns:
            Lista di sezioni estratte
        """
        logger.info(f"Extracting {len(index.sections)} sections from PDF: {file_path}")
        self._validate_extraction_request(file_path, index)

        sections = []
        document = None

        try:
            document = self._load_pdf(file_path)

            # Estrai tutte le sezioni tranne l'ultima
            for i in range(len(index.sections) - 1):
                current_section = index.sections[i]
                next_section = index.sections[i + 1]

                text = self._extract_single_section(
                    document,
                    current_section.section,
                    next_section.section,
                    current_section.start,
                    next_section.start,
                    index
                )

                sections.append(ExtractedSection(
                    section=current_section.section,
                    text=text
                ))

            # Estrai l'ultima sezione
            last_section = index.sections[-1]
            text = self._extract_single_section(
                document,
                last_section.section,
                None,
                last_section.start,
                last_section.end,
                index
            )

            sections.append(ExtractedSection(
                section=last_section.section,
                text=text
            ))

            logger.info(f"Successfully extracted {len(sections)} sections from PDF")
            return sections

        except Exception as e:
            logger.error(f"Error extracting sections from PDF: {file_path}, {e}")
            raise RuntimeError(f"Error extracting sections from PDF: {file_path}, {e}")
        finally:
            self._close_document(document)

    def extract_index(self, file_path: str) -> dict:
        """
        Estrae l'indice/sommario dal PDF.

        Args:
            file_path: Percorso del file PDF

        Returns:
            Dizionario con l'indice estratto contenente:
            - has_toc: True se il PDF ha un indice integrato
            - toc: Lista delle voci dell'indice (se presente)
            - outline: Struttura gerarchica dell'indice
        """
        logger.info(f"Extracting index from: {file_path}")
        self._validate_file_path(file_path)

        document = None
        try:
            document = self._load_pdf(file_path)

            # Estrai l'outline/table of contents se presente
            toc = document.get_toc(simple=False)

            index_data = {
                'has_toc': len(toc) > 0,
                'total_entries': len(toc),
                'toc': []
            }

            if toc:
                # Converti l'outline in formato leggibile
                for entry in toc:
                    level = entry[0]  # Livello di indentazione (1, 2, 3, ...)
                    title = entry[1]  # Titolo della sezione
                    page = entry[2]   # Numero di pagina

                    index_data['toc'].append({
                        'level': level,
                        'title': title,
                        'page': page
                    })

                logger.info(f"Found {len(toc)} table of contents entries")
            else:
                logger.info("No built-in table of contents found")

            return index_data

        except Exception as e:
            logger.error(f"Error extracting index from PDF: {e}")
            raise RuntimeError(f"Error extracting index from PDF: {e}")
        finally:
            self._close_document(document)

    def extract_images(self, file_path: str, page_number: int = 5, output_folder: str = "extracted_images") -> List[str]:
        """
        Estrae le immagini da una pagina specifica del PDF.

        Args:
            file_path: Percorso del file PDF
            page_number: Numero della pagina (0-based)
            output_folder: Cartella dove salvare le immagini

        Returns:
            Lista dei percorsi delle immagini salvate
        """
        logger.info(f"Extracting images from page {page_number} of: {file_path}")
        document = None
        saved_images = []

        try:
            document = self._load_pdf(file_path)

            # Crea la cartella di output se non esiste
            output_dir = Path(output_folder)
            output_dir.mkdir(parents=True, exist_ok=True)
            logger.info(f"Output directory: {output_folder}")

            if page_number >= len(document):
                logger.warning(f"Page {page_number} does not exist in PDF")
                return saved_images

            page = document.load_page(page_number)
            images = page.get_images(full=True)

            for img_index, img in enumerate(images):
                xref = img[0]
                base_image = document.extract_image(xref)
                image_bytes = base_image["image"]
                image_ext = base_image["ext"]

                image_name = f"image_{xref}.{image_ext}"
                image_path = output_dir / image_name

                with open(image_path, "wb") as image_file:
                    image_file.write(image_bytes)

                logger.info(f"Saved image to: {image_path}")
                saved_images.append(str(image_path))

            return saved_images

        except Exception as e:
            logger.error(f"Error extracting images from PDF: {e}")
            raise RuntimeError(f"Error extracting images from PDF: {e}")
        finally:
            self._close_document(document)

    def _load_pdf(self, file_path: str) -> fitz.Document:
        """Carica un documento PDF."""
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"PDF file not found, filepath: {file_path}")
        return fitz.open(file_path)

    def _close_document(self, document: Optional[fitz.Document]):
        """Chiude un documento PDF."""
        if document is not None:
            try:
                document.close()
                logger.debug("Successfully closed PDF document")
            except Exception as e:
                logger.warning(f"Error closing PDF document: {e}")

    def _extract_single_section(
        self,
        document: fitz.Document,
        current_section_title: str,
        next_section_title: Optional[str],
        start_page: int,
        end_page: int,
        index: PdfIndex
    ) -> str:
        """Estrae una singola sezione dal documento."""
        try:
            offset = self._calculate_offset(document, index)

            # Converti da 1-based a 0-based
            start_idx = start_page - 1
            if end_page == -1:
                end_idx = len(document)
            elif end_page + offset <= len(document):
                end_idx = end_page + offset
            else:
                end_idx = len(document)

            # Estrai il testo dalle pagine
            extracted_text = []
            for page_num in range(start_idx, end_idx):
                page = document.load_page(page_num)
                text = page.get_text()
                extracted_text.append(text)

            full_text = "\n".join(extracted_text)

            # Rimuovi l'indice se necessario
            index_end = index.sections[0].start
            if start_page <= index_end:
                full_text = self._remove_index(document, index)

            # Estrai la sezione usando regex
            if next_section_title:
                pattern = f"(?s){re.escape(current_section_title)}(.*?){re.escape(next_section_title)}"
            else:
                pattern = f"(?s){re.escape(current_section_title)}(.*?)$"

            regex = re.compile(pattern)
            match = regex.search(full_text)

            if match:
                section_text = match.group(1).strip()
                logger.debug(f"Successfully extracted section '{current_section_title}', length: {len(section_text)}")
                return section_text
            else:
                logger.warning(f"No content found for section: {current_section_title}")
                return ""

        except Exception as e:
            logger.error(f"Error extracting section '{current_section_title}' from PDF: {e}")
            raise RuntimeError(f"Error extracting section from PDF: {e}")

    def _calculate_offset(self, document: fitz.Document, index: PdfIndex) -> int:
        """Calcola l'offset delle pagine."""
        try:
            first_section_title = index.sections[0].section
            occurrence = 0

            for page_num in range(len(document)):
                page = document.load_page(page_num)
                text = page.get_text()

                if first_section_title in text and occurrence == 0:
                    occurrence += 1
                elif first_section_title in text and occurrence > 0:
                    offset = page_num
                    logger.debug(f"Calculated page offset: {offset}")
                    return offset

            logger.debug("No offset calculated, using default: 0")
            return 0

        except Exception as e:
            logger.error(f"Error calculating page offset: {e}")
            raise RuntimeError(f"Error calculating page offset: {e}")

    def _remove_index(self, document: fitz.Document, index: PdfIndex) -> str:
        """Rimuove l'indice dal testo estratto."""
        try:
            first_content_title = index.sections[0].section

            # Estrai tutto il testo
            full_text = []
            for page_num in range(len(document)):
                page = document.load_page(page_num)
                text = page.get_text()
                full_text.append(text)

            full_text_str = "\n".join(full_text)

            # Pattern per rimuovere l'indice
            pattern = f"(?s){re.escape(first_content_title)}(.*?){re.escape(first_content_title)}"
            regex = re.compile(pattern)
            match = regex.search(full_text_str)

            if match:
                cleaned_text = (
                    full_text_str[:match.start()] +
                    first_content_title +
                    full_text_str[match.end():]
                )
                logger.debug("Successfully removed index from text")
                return cleaned_text
            else:
                logger.debug("No index pattern found, returning original text")
                return full_text_str

        except Exception as e:
            logger.error(f"Error removing index from PDF text: {e}")
            raise RuntimeError(f"Error removing index from PDF text: {e}")

    def _validate_file_path(self, file_path: str):
        """Valida il percorso del file."""
        if not file_path or not file_path.strip():
            raise ValueError("File path cannot be null or empty")

    def _validate_page_range(self, start_page: int, end_page: int):
        """Valida l'intervallo di pagine."""
        if start_page < 1:
            raise ValueError("Start page must be greater than 0")

        if end_page != -1 and end_page < start_page:
            raise ValueError("End page must be greater than or equal to start page")

    def _validate_extraction_request(self, file_path: str, index: PdfIndex):
        """Valida la richiesta di estrazione."""
        self._validate_file_path(file_path)

        if not index or not index.sections:
            raise ValueError("Index must contain at least one section")

    def _merge_text_and_images(self, text_blocks: list, images_data: List[dict]) -> str:
        """
        Merge testo e immagini in ordine basato sulle coordinate Y.

        Args:
            text_blocks: Lista di blocchi di testo da PyMuPDF (con coordinate)
            images_data: Lista di dati delle immagini con posizioni Y

        Returns:
            Testo con immagini integrate nelle posizioni corrette
        """
        # Crea lista combinata di elementi (testo + immagini) con posizione Y
        elements = []

        # Aggiungi blocchi di testo
        for block in text_blocks:
            # block format: (x0, y0, x1, y1, "text", block_no, block_type)
            if len(block) >= 5 and isinstance(block[4], str) and block[4].strip():
                y_position = (block[1] + block[3]) / 2  # Y centrale del blocco
                elements.append({
                    'type': 'text',
                    'y_position': y_position,
                    'content': block[4]
                })

        # Aggiungi immagini
        for img_data in images_data:
            if img_data.get('y_position') is not None:
                elements.append({
                    'type': 'image',
                    'y_position': img_data['y_position'],
                    'content': f"\n[IMAGE {img_data['image_index']}]: {img_data['text']}\n"
                })

        # Ordina tutti gli elementi per posizione Y (dall'alto verso il basso)
        elements.sort(key=lambda x: x['y_position'])

        # Costruisci il testo finale
        result_parts = []
        for element in elements:
            if element['type'] == 'text':
                result_parts.append(element['content'].rstrip('\n'))
            else:  # image
                result_parts.append(element['content'])

        return '\n'.join(result_parts)

    def _extract_images_with_placeholders(self, file_path: str) -> List[dict]:
        """
        Estrae le posizioni delle immagini e restituisce placeholder.

        Args:
            file_path: Percorso del file PDF

        Returns:
            Lista di dizionari con placeholder per le immagini
        """
        try:
            document = fitz.open(file_path)
            all_images = []

            for page_num in range(len(document)):
                page = document.load_page(page_num)
                images = page.get_images(full=True)

                for img_index, img in enumerate(images):
                    xref = img[0]

                    # Ottieni le coordinate dell'immagine
                    img_rects = page.get_image_rects(xref)
                    y_position = None
                    if img_rects:
                        img_rect = img_rects[0]
                        y_position = (img_rect.y0 + img_rect.y1) / 2

                    all_images.append({
                        'page': page_num + 1,
                        'image_index': img_index + 1,
                        'text': self.placeholder_text,
                        'y_position': y_position
                    })

            document.close()
            return all_images

        except Exception as e:
            logger.warning(f"Error extracting image placeholders: {e}")
            return []

    def _extract_images_with_placeholders_in_range(self, file_path: str, start_page: int, end_page: int) -> List[dict]:
        """
        Estrae le posizioni delle immagini in un intervallo e restituisce placeholder.

        Args:
            file_path: Percorso del file PDF
            start_page: Pagina di inizio (0-based)
            end_page: Pagina di fine (0-based)

        Returns:
            Lista di dizionari con placeholder per le immagini
        """
        try:
            document = fitz.open(file_path)
            all_images = []

            for page_num in range(start_page, end_page):
                if page_num >= len(document):
                    break

                page = document.load_page(page_num)
                images = page.get_images(full=True)

                for img_index, img in enumerate(images):
                    xref = img[0]

                    # Ottieni le coordinate dell'immagine
                    img_rects = page.get_image_rects(xref)
                    y_position = None
                    if img_rects:
                        img_rect = img_rects[0]
                        y_position = (img_rect.y0 + img_rect.y1) / 2

                    all_images.append({
                        'page': page_num + 1,
                        'image_index': img_index + 1,
                        'text': self.placeholder_text,
                        'y_position': y_position
                    })

            document.close()
            return all_images

        except Exception as e:
            logger.warning(f"Error extracting image placeholders: {e}")
            return []

    def _extract_images_data(self, file_path: str) -> List[dict]:
        """
        Estrae dati da tutte le immagini nel PDF usando OpenAI Vision in modo asincrono.

        Args:
            file_path: Percorso del file PDF

        Returns:
            Lista di dizionari con dati delle immagini (page, image_index, text, image_path)
        """
        if not self.image_extractor:
            return []

        try:
            # Estrai tutte le immagini e processale in parallelo
            document = fitz.open(file_path)
            all_image_info = []

            # Crea la cartella di output se necessario
            if self.save_images:
                output_dir = Path(self.output_folder)
                output_dir.mkdir(parents=True, exist_ok=True)
                logger.info(f"Saving images to: {self.output_folder}")

            # Raccogli informazioni su tutte le immagini
            for page_num in range(len(document)):
                page = document.load_page(page_num)
                images = page.get_images(full=True)

                for img_index, img in enumerate(images):
                    xref = img[0]
                    base_image = document.extract_image(xref)
                    image_bytes = base_image["image"]
                    image_ext = base_image.get("ext", "png")

                    # Ottieni le coordinate dell'immagine
                    img_rects = page.get_image_rects(xref)
                    y_position = None
                    if img_rects:
                        img_rect = img_rects[0]
                        y_position = (img_rect.y0 + img_rect.y1) / 2  # Y centrale

                    # Salva l'immagine se richiesto
                    image_filename = None
                    if self.save_images:
                        image_filename = f"page_{page_num + 1}_img_{img_index + 1}.{image_ext}"
                        image_path = Path(self.output_folder) / image_filename
                        with open(image_path, "wb") as img_file:
                            img_file.write(image_bytes)

                    all_image_info.append({
                        'page': page_num + 1,
                        'image_index': img_index + 1,
                        'image_bytes': image_bytes,
                        'image_ext': image_ext,
                        'y_position': y_position,
                        'saved_path': str(image_path) if self.save_images else None
                    })

            document.close()

            if not all_image_info:
                return ""

            logger.info(f"Processing {len(all_image_info)} images in parallel")

            # Processa le immagini in parallelo usando ThreadPoolExecutor
            with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
                future_to_img = {
                    executor.submit(
                        self.image_extractor._analyze_image_with_openai,
                        img_info['image_bytes'],
                        img_info['image_ext']
                    ): img_info
                    for img_info in all_image_info
                }

                results = []
                for future in concurrent.futures.as_completed(future_to_img):
                    img_info = future_to_img[future]
                    try:
                        extracted_text = future.result()
                        if extracted_text:
                            result_data = {
                                'page': img_info['page'],
                                'image_index': img_info['image_index'],
                                'text': extracted_text
                            }
                            if self.save_images and img_info.get('saved_path'):
                                result_data['image_path'] = img_info['saved_path']
                            results.append(result_data)

                            # Salva anche il testo estratto in un file .txt
                            if self.save_images and img_info.get('saved_path'):
                                txt_path = Path(img_info['saved_path']).with_suffix('.txt')
                                with open(txt_path, 'w', encoding='utf-8') as txt_file:
                                    txt_file.write(extracted_text)
                    except Exception as e:
                        logger.warning(f"Error processing image on page {img_info['page']}: {e}")

            # Ordina per pagina e posizione Y
            results.sort(key=lambda x: (x['page'], x.get('y_position') or 0))

            # Restituisci i risultati come lista di dizionari
            return results

        except Exception as e:
            logger.warning(f"Error extracting text from images: {e}")
            return []

    def _extract_images_data_in_range(self, file_path: str, start_page: int, end_page: int) -> List[dict]:
        """
        Estrae dati dalle immagini in un intervallo di pagine in modo asincrono.

        Args:
            file_path: Percorso del file PDF
            start_page: Pagina di inizio (0-based)
            end_page: Pagina di fine (0-based)

        Returns:
            Lista di dizionari con dati delle immagini (page, image_index, text, image_path)
        """
        if not self.image_extractor:
            return []

        try:
            # Estrai le immagini solo dalle pagine richieste
            document = fitz.open(file_path)
            all_image_info = []

            # Crea la cartella di output se necessario
            if self.save_images:
                output_dir = Path(self.output_folder)
                output_dir.mkdir(parents=True, exist_ok=True)
                logger.info(f"Saving images to: {self.output_folder}")

            for page_num in range(start_page, end_page):
                if page_num >= len(document):
                    break

                page = document.load_page(page_num)
                images = page.get_images(full=True)

                for img_index, img in enumerate(images):
                    xref = img[0]
                    base_image = document.extract_image(xref)
                    image_bytes = base_image["image"]
                    image_ext = base_image.get("ext", "png")

                    # Ottieni le coordinate dell'immagine
                    img_rects = page.get_image_rects(xref)
                    y_position = None
                    if img_rects:
                        img_rect = img_rects[0]
                        y_position = (img_rect.y0 + img_rect.y1) / 2  # Y centrale

                    # Salva l'immagine se richiesto
                    image_filename = None
                    if self.save_images:
                        image_filename = f"page_{page_num + 1}_img_{img_index + 1}.{image_ext}"
                        image_path = Path(self.output_folder) / image_filename
                        with open(image_path, "wb") as img_file:
                            img_file.write(image_bytes)

                    all_image_info.append({
                        'page': page_num + 1,
                        'image_index': img_index + 1,
                        'image_bytes': image_bytes,
                        'image_ext': image_ext,
                        'y_position': y_position,
                        'saved_path': str(image_path) if self.save_images else None
                    })

            document.close()

            if not all_image_info:
                return []

            logger.info(f"Processing {len(all_image_info)} images in parallel (pages {start_page+1}-{end_page})")

            # Processa le immagini in parallelo
            with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
                future_to_img = {
                    executor.submit(
                        self.image_extractor._analyze_image_with_openai,
                        img_info['image_bytes'],
                        img_info['image_ext']
                    ): img_info
                    for img_info in all_image_info
                }

                results = []
                for future in concurrent.futures.as_completed(future_to_img):
                    img_info = future_to_img[future]
                    try:
                        extracted_text = future.result()
                        if extracted_text:
                            result_data = {
                                'page': img_info['page'],
                                'image_index': img_info['image_index'],
                                'text': extracted_text,
                                'y_position': img_info.get('y_position')
                            }
                            if self.save_images and img_info.get('saved_path'):
                                result_data['image_path'] = img_info['saved_path']
                            results.append(result_data)

                            # Salva anche il testo estratto in un file .txt
                            if self.save_images and img_info.get('saved_path'):
                                txt_path = Path(img_info['saved_path']).with_suffix('.txt')
                                with open(txt_path, 'w', encoding='utf-8') as txt_file:
                                    txt_file.write(extracted_text)
                    except Exception as e:
                        logger.warning(f"Error processing image on page {img_info['page']}: {e}")

            # Ordina per pagina e posizione Y
            results.sort(key=lambda x: (x['page'], x.get('y_position') or 0))

            # Restituisci i risultati come lista di dizionari
            return results

        except Exception as e:
            logger.warning(f"Error extracting text from images in range: {e}")
            return []
