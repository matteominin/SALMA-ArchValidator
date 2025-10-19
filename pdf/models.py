from dataclasses import dataclass
from typing import List, Optional


@dataclass
class Section:
    """Rappresenta una sezione nel PDF index."""
    section: str  # Nome della sezione
    start: int    # Pagina di inizio
    end: Optional[int] = -1  # Pagina di fine (-1 significa fino alla fine)


@dataclass
class PdfIndex:
    """Indice delle sezioni di un PDF."""
    sections: List[Section]

    def __post_init__(self):
        """Valida i dati dell'indice."""
        if not self.sections:
            raise ValueError("Index must contain at least one section")

        for section in self.sections:
            if not section.section or not section.section.strip():
                raise ValueError("Section name cannot be null or empty")

            if section.start < 1:
                raise ValueError("Section start page must be greater than 0")


@dataclass
class ExtractedSection:
    """Rappresenta una sezione estratta dal PDF."""
    section: str  # Nome della sezione
    text: str     # Testo estratto
