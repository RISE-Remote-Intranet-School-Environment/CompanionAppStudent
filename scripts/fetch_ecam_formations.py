#!/usr/bin/env python3
"""
Fetches ECAM course programmes from https://plus.ecam.be/public and stores them
as a structured JSON database inside data/ecam_formations_2025.json.
"""

from __future__ import annotations

import json
import sys
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import List, Optional
from urllib.parse import urljoin

import requests
from bs4 import BeautifulSoup, Tag

BASE_URL = "https://plus.ecam.be"
YEAR = "2025"
OUTPUT_PATH = Path("composeApp/src/commonMain/composeResources/files/ecam_formations_2025.json")


@dataclass
class FormationSource:
    formation_id: str
    name: str
    code: Optional[str]
    path_kind: str  # "cursus", "bloc", or "manual"
    notes: Optional[str] = None

    @property
    def url(self) -> Optional[str]:
        if not self.code:
            return None
        if self.path_kind == "cursus":
            return f"{BASE_URL}/public/cursus/{YEAR}/{self.code}"
        if self.path_kind == "bloc":
            return f"{BASE_URL}/public/bloc/{YEAR}/{self.code}"
        return None


def fetch_html(url: str) -> str:
    resp = requests.get(url, timeout=30)
    resp.raise_for_status()
    return resp.content.decode("utf-8")


def parse_course_cells(cells: List[Tag]) -> dict:
    def cell_text(cell: Tag) -> str:
        return " ".join(cell.stripped_strings).strip()

    raw_title = cell_text(cells[0])
    code, title = (raw_title.split(" ", 1) + [""])[:2]
    credits_raw = cell_text(cells[1]) if len(cells) > 1 else ""
    try:
        credits_val = float(credits_raw.replace(",", "."))
        credits = int(credits_val) if credits_val.is_integer() else credits_val
    except ValueError:
        credits = credits_raw

    extra_cells = cells[2:]
    periods = [cell_text(c) for c in extra_cells if cell_text(c)]

    link = cells[0].find("a")
    course_url = urljoin(BASE_URL, link["href"]) if link and link.get("href") else None

    return {
        "code": code.strip(),
        "title": title.strip(),
        "credits": credits,
        "periods": periods,
        "details_url": course_url,
    }


def parse_cursus_table(html: str, default_block: str) -> List[dict]:
    soup = BeautifulSoup(html, "lxml")
    table = soup.find("table")
    if table is None:
        raise ValueError("No table found in cursus page")

    blocks: List[dict] = []
    current_block: Optional[dict] = None

    for row in table.find_all("tr"):
        header = row.find("th")
        if header and header.has_attr("colspan"):
            block_name = header.get_text(strip=True)
            current_block = {"name": block_name or default_block, "courses": []}
            blocks.append(current_block)
            continue

        cells = row.find_all("td")
        if not cells:
            continue
        if current_block is None:
            current_block = {"name": default_block, "courses": []}
            blocks.append(current_block)
        current_block["courses"].append(parse_course_cells(cells))

    return blocks


def parse_bloc_table(html: str, block_name: str) -> List[dict]:
    soup = BeautifulSoup(html, "lxml")
    table = soup.find("table")
    if table is None:
        raise ValueError("No table found in bloc page")
    block = {"name": block_name, "courses": []}
    for row in table.find_all("tr"):
        cells = row.find_all("td")
        if not cells:
            continue
        block["courses"].append(parse_course_cells(cells))
    return [block]


def collect_formations() -> dict:
    formations = [
        FormationSource("automatisation", "Automatisation", "MAU", "cursus"),
        FormationSource("construction", "Construction", "MCO", "cursus"),
        FormationSource("electromecanique", "Electromecanique", "MEM", "cursus"),
        FormationSource("electronique", "Electronique", "MEO", "cursus"),
        FormationSource("geometre", "Geometre", "MGE", "cursus"),
        FormationSource("informatique", "Informatique", "MIN", "cursus"),
        FormationSource("ingenierie_sante", "Ingenierie de la sante", "MIS", "cursus"),
        FormationSource(
            "ingenieur_industriel_commercial",
            "Ingenieur industriel et commercial",
            "5MIC",
            "bloc",
        ),
        FormationSource(
            "business_analyst",
            "Master Business Analyst",
            None,
            "manual",
            notes=(
                "Programme detaille non publie sur plus.ecam.be. "
                "Consulter https://www.ecam.be/formations/business-analyst/ "
                "ou les PDFs lies pour le contenu complet."
            ),
        ),
    ]

    dataset = {
        "year": YEAR,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "source": BASE_URL,
        "formations": [],
    }

    for formation in formations:
        entry = {
            "id": formation.formation_id,
            "name": formation.name,
            "source_url": formation.url,
            "blocks": [],
        }
        if formation.notes:
            entry["notes"] = formation.notes

        if formation.url:
            html = fetch_html(formation.url)
            if formation.path_kind == "cursus":
                entry["blocks"] = parse_cursus_table(html, default_block=formation.name)
            elif formation.path_kind == "bloc":
                heading = BeautifulSoup(html, "lxml").find("h4")
                block_name = heading.get_text(strip=True) if heading else formation.name
                entry["blocks"] = parse_bloc_table(html, block_name)
        dataset["formations"].append(entry)

    return dataset


def main() -> None:
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    dataset = collect_formations()
    OUTPUT_PATH.write_text(json.dumps(dataset, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {OUTPUT_PATH} with {len(dataset['formations'])} formations.")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:  # pragma: no cover
        print(f"Failed to fetch formations: {exc}", file=sys.stderr)
        sys.exit(1)
