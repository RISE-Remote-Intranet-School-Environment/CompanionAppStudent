from __future__ import annotations

import json
import re
import unicodedata
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple
from urllib.parse import urlsplit

import requests
from bs4 import BeautifulSoup

# Entry point page that lists every staff member with a link to their profile.
ANNUAIRE_PAGE_ENDPOINT = "https://www.ecam.be/wp-json/wp/v2/pages?slug=annuaire"

# Profile pages can live at /{slug}/ or /annuaire/{slug}/ depending on legacy links.
PROFILE_URL_CANDIDATES = [
    "https://www.ecam.be/{slug}/",
    "https://www.ecam.be/annuaire/{slug}/",
]

OUTPUT_PATH = Path(
    "composeApp/src/commonMain/composeResources/files/ecam_professors_2025_test.json"
)

HEADERS = {
    "User-Agent": "Mozilla/5.0 (compatible; ECAMFetcher/2.1; +https://www.ecam.be)"
}

DEFAULT_OFFICE = "Non renseigné"
DEFAULT_SPECIALITY = "Non renseigné"


def strip_accents(value: str) -> str:
    return "".join(
        char for char in unicodedata.normalize("NFKD", value) if not unicodedata.combining(char)
    )


def clean_text(value: str) -> str:
    return " ".join(value.replace("\xa0", " ").split()).strip()


def extract_slug(url: str) -> Optional[str]:
    path = urlsplit(url).path
    parts = [p for p in path.split("/") if p]
    if not parts:
        return None
    slug = parts[-1]
    if slug.lower() in {"annuaire", "en"}:
        return None
    return slug


session = requests.Session()
session.headers.update(HEADERS)


REQUEST_TIMEOUT = 8


def fetch_annuaire_content() -> str:
    response = session.get(ANNUAIRE_PAGE_ENDPOINT, timeout=REQUEST_TIMEOUT)
    response.raise_for_status()
    data = response.json()
    if not data:
        raise RuntimeError("Aucune donnee renvoyee par l'API annuaire")
    return data[0]["content"]["rendered"]


def collect_slugs_and_fallback_names(html: str) -> Iterable[Tuple[str, str]]:
    soup = BeautifulSoup(html, "html.parser")
    seen: Dict[str, str] = {}

    for anchor in soup.find_all("a"):
        href = anchor.get("href")
        if not href:
            continue
        slug = extract_slug(href)
        if not slug:
            continue
        slug_key = slug.lower()
        if slug_key in seen:
            continue
        fallback_name = clean_text(anchor.get_text(" ", strip=True))
        seen[slug_key] = slug
        yield slug, fallback_name


def fetch_profile_html(slug: str) -> Optional[str]:
    for template in PROFILE_URL_CANDIDATES:
        url = template.format(slug=slug)
        try:
            response = session.get(url, timeout=REQUEST_TIMEOUT)
            if response.status_code == 200:
                return response.text
        except Exception:
            continue
    return None


def extract_office(soup: BeautifulSoup) -> str:
    pattern = re.compile(r"Bureau\s*[:\-–]?\s*([0-9A-Z\- ]+)", re.IGNORECASE)
    for text in soup.stripped_strings:
        match = pattern.search(text)
        if match:
            return clean_text(match.group(1))
    return DEFAULT_OFFICE


def extract_email(slug: str, soup: BeautifulSoup) -> str:
    for anchor in soup.select("a[href^=mailto]"):
        href = anchor.get("href", "")
        email = href.split(":", 1)[-1].split("?", 1)[0].strip()
        if email:
            return email.lower()
    return f"{slug.lower()}@ecam.be"


def extract_speciality(soup: BeautifulSoup) -> str:
    # Try to capture the formation code that is usually in parentheses on the role line.
    role_pattern = re.compile(r"Enseign[^()]*\(([A-Z]{2,4})\)", re.IGNORECASE)
    for tag in soup.find_all(string=role_pattern):
        match = role_pattern.search(tag)
        if match:
            return match.group(1).upper()

    generic_pattern = re.compile(r"\(([A-Z]{2,4})\)")
    match = generic_pattern.search(soup.get_text(" ", strip=True))
    if match:
        return match.group(1).upper()
    return DEFAULT_SPECIALITY


def extract_photo_url(soup: BeautifulSoup) -> Optional[str]:
    main = soup.select_one("#main-content") or soup
    img = main.find("img")
    return img.get("src") if img and img.get("src") else None


def collect_list_items_after_heading(heading) -> List[str]:
    items: List[str] = []
    for sibling in heading.next_siblings:
        if getattr(sibling, "name", None) in {"h1", "h2", "h3"}:
            break
        if getattr(sibling, "name", None) in {"ul", "ol"}:
            for li in sibling.find_all("li"):
                txt = clean_text(li.get_text(" ", strip=True))
                if txt:
                    items.append(txt)
            if items:
                break
    return items


def extract_role_and_diplomas(soup: BeautifulSoup) -> Tuple[Optional[str], List[str], List[str]]:
    main = soup.select_one("#main-content") or soup
    headings = list(main.find_all(["h1", "h2", "h3"]))

    role_title: Optional[str] = None
    role_details: List[str] = []
    diplomas: List[str] = []

    for idx, heading in enumerate(headings):
        if idx == 0:
            continue  # first heading is the name
        text = clean_text(heading.get_text(" ", strip=True))
        if not text:
            continue
        if role_title is None:
            role_title = text
            role_details = collect_list_items_after_heading(heading)
        if "dipl" in text.lower():
            diplomas = collect_list_items_after_heading(heading)
    return role_title, role_details, diplomas


def extract_names(soup: BeautifulSoup, fallback: str, slug: str) -> Tuple[str, str]:
    title_tag = soup.find(["h1", "h2"])
    raw_name = title_tag.get_text(" ", strip=True) if title_tag else fallback
    raw_name = strip_accents(clean_text(raw_name)) or slug.lower()
    parts = raw_name.split()
    if not parts:
        return slug.lower(), slug.upper()
    if len(parts) == 1:
        return parts[0].title(), parts[0].upper()
    first_name = parts[0].title()
    last_name = " ".join(parts[1:]).upper()
    return first_name, last_name


def build_professor_entry(
    idx: int, slug: str, fallback_name: str, soup: BeautifulSoup
) -> Dict[str, str]:
    first_name, last_name = extract_names(soup, fallback_name, slug)
    email = extract_email(slug, soup)
    office = extract_office(soup)
    speciality = extract_speciality(soup)
    photo_url = extract_photo_url(soup)
    role_title, role_details, diplomas = extract_role_and_diplomas(soup)

    return {
        "id": idx,
        "professor_id": email.split("@", 1)[0],
        "first_name": first_name,
        "last_name": last_name,
        "email": email,
        "speciality": speciality,
        "office": office,
        "photo_url": photo_url or "",
        "role_title": role_title or "",
        "role_details": role_details,
        "diplomas": diplomas,
    }


def generate_json(professors: List[Dict[str, str]]) -> None:
    payload = {"professors": professors}
    OUTPUT_PATH.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print(f"{len(professors)} profils enregistres dans {OUTPUT_PATH}")


def main() -> None:
    annuaire_html = fetch_annuaire_content()
    professors: List[Dict[str, str]] = []

    for idx, (slug, fallback_name) in enumerate(
        collect_slugs_and_fallback_names(annuaire_html), start=1
    ):
        profile_html = fetch_profile_html(slug) or ""
        if not profile_html:
            print(f"[{idx}] WARNING: profil {slug} introuvable, valeurs par defaut utilisees")
        soup = BeautifulSoup(profile_html, "html.parser")
        entry = build_professor_entry(idx, slug, fallback_name, soup)
        professors.append(entry)
        print(
            f"[{idx}] {entry['first_name']} {entry['last_name']} -> {entry['email']} ({entry['office']})"
        )

    generate_json(professors)


if __name__ == "__main__":
    main()
