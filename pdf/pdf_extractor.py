from typing import Optional
import base64
import fitz  # PyMuPDF
from openai import OpenAI
import logging

logger = logging.getLogger(__name__)


class PDFImageExtractor:
    """
    Estrattore che usa OpenAI Vision per analizzare solo le immagini nei PDF.
    """

    def __init__(self, openai_api_key: Optional[str] = None, model: str = "gpt-4o"):
        """
        Inizializza l'estrattore con configurazione OpenAI.

        Args:
            openai_api_key: API key di OpenAI (se None, usa variabile d'ambiente OPENAI_API_KEY)
            model: Modello OpenAI da usare (default: 'gpt-4o')
        """
        self.model = model
        self.client = OpenAI(api_key=openai_api_key) if openai_api_key else OpenAI()

    def extract_images_with_text(self, pdf_path: str) -> list:
        """
        Estrae tutte le immagini dal PDF e usa OpenAI Vision per estrarre il testo.

        Args:
            pdf_path: Percorso del file PDF

        Returns:
            Lista di dizionari con informazioni sulle immagini e testo estratto
        """
        try:
            document = fitz.open(pdf_path)
            all_images = []

            for page_num in range(len(document)):
                page = document.load_page(page_num)
                images = page.get_images(full=True)

                for img_index, img in enumerate(images):
                    xref = img[0]

                    # Estrai l'immagine
                    base_image = document.extract_image(xref)
                    image_bytes = base_image["image"]
                    image_ext = base_image.get("ext", "png")

                    # Analizza con OpenAI Vision
                    extracted_text = self._analyze_image_with_openai(image_bytes, image_ext)

                    # Ottieni le coordinate dell'immagine
                    img_rects = page.get_image_rects(xref)
                    coordinates = None
                    if img_rects:
                        img_rect = img_rects[0]
                        coordinates = {
                            'x0': round(img_rect.x0, 2),
                            'y0': round(img_rect.y0, 2),
                            'x1': round(img_rect.x1, 2),
                            'y1': round(img_rect.y1, 2),
                            'width': round(img_rect.width, 2),
                            'height': round(img_rect.height, 2)
                        }

                    all_images.append({
                        'page': page_num + 1,
                        'image_index': img_index + 1,
                        'xref': xref,
                        'format': image_ext,
                        'coordinates': coordinates,
                        'extracted_text': extracted_text
                    })

            document.close()
            logger.info(f"Extracted {len(all_images)} images from PDF")
            return all_images

        except Exception as e:
            raise Exception(f"Errore durante l'estrazione delle immagini: {str(e)}")

    def _analyze_image_with_openai(self, image_bytes: bytes, image_ext: str) -> Optional[str]:
        """
        Analizza un'immagine usando OpenAI Vision.

        Args:
            image_bytes: Bytes dell'immagine
            image_ext: Estensione/formato dell'immagine

        Returns:
            Testo estratto dall'immagine o None se fallisce
        """
        try:
            # Converti in base64
            base64_image = base64.b64encode(image_bytes).decode('utf-8')
            mime_type = f"image/{image_ext}"

            # Chiama OpenAI Vision API
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": """Analyze this image and extract all relevant information. Follow these instructions:

1. First, identify what type of image this is:
   - Logo/Brand image
   - Diagram (UML, flowchart, use case, wireframe, etc.)
   - Screenshot or UI mockup
   - Chart or graph
   - Photo or illustration
   - Table or data
   - Other

2. Based on the image type:

   IF IT'S A LOGO/BRAND IMAGE:
   - Simply describe it: "Logo: [organization/company name if visible]"
   - Do NOT try to extract diagrams or relationships

   IF IT'S A DIAGRAM:
   - Diagram Type: [specify type]
   - Elements: List key components
   - Relationships: Describe connections between elements
   - Text: Extract all visible text

   IF IT'S TEXT/TABLE:
   - Extract all text exactly as it appears

   IF IT'S A PHOTO/ILLUSTRATION:
   - Brief description: [what the image shows]

3. Be accurate - don't invent information that isn't clearly visible in the image.
4. If the image is unclear or you're uncertain or all black, say so.
5. Keep your response concise and relevant."""
                            },
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": f"data:{mime_type};base64,{base64_image}"
                                }
                            }
                        ]
                    }
                ],
                max_tokens=1000
            )

            extracted_text = response.choices[0].message.content.strip()
            return extracted_text if extracted_text else None

        except Exception as e:
            logger.error(f"Error analyzing image with OpenAI: {e}")
            return None
