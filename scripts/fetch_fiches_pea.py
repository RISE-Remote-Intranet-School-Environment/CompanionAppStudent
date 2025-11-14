from __future__ import annotations
import json
import sys
import time
from pathlib import Path
from typing import Optional
import requests
from bs4 import BeautifulSoup

INPUT_PATH = Path("composeApp/src/commonMain/composeResources/files/ecam_formations_2025.json")
OUTPUT_PATH = Path("composeApp/src/commonMain/composeResources/files/ecam_courses_details_2025.json")
BASE_URL = "https://plus.ecam.be"

HEADERS = {
    "User-Agent": "Mozilla/5.0 (compatible; ECAMFetcher/1.3; +https://plus.ecam.be)"
}


def fetch_html(url: str) -> Optional[str]:
    try:
        resp = requests.get(url, headers=HEADERS, timeout=20)
        if resp.status_code != 200:
            print(f"[⚠️] {resp.status_code} pour {url}")
            return None
        return resp.text
    except Exception as e:
        print(f"[⚠️] Erreur pour {url}: {e}")
        return None


def extract_code_from_url(url: str) -> Optional[str]:
    match = re.search(r"/fiche/\d{4}/([a-zA-Z0-9]+)$", url)
    return match.group(1).upper() if match else None


def parse_table_overview(soup: BeautifulSoup) -> dict:
    data = {}
    table = soup.find("table", class_="table-bordered")
    if not table:
        return data

    for row in table.find_all("tr"):
        cells = [c.get_text(strip=True) for c in row.find_all(["th", "td"])]
        if not cells:
            continue

        if "Nom de l'UE" in cells[0]:
            txt = cells[1]
            parts = txt.split(" ", 1)
            if len(parts) == 2:
                data["code"], data["title"] = parts
            else:
                data["title"] = txt
            data["mandatory"] = "Obligatoire" in "".join(cells)
        elif "Crédits" in cells[0]:
            data["credits"] = cells[1]
            data["hours"] = cells[3] if len(cells) > 3 else ""
        elif "Responsable" in cells[0]:
            data["responsable"] = cells[1]
            data["language"] = cells[3] if len(cells) > 3 else ""
        elif "Blocs" in cells[0]:
            data["bloc"] = re.search(r"(\d+[A-Z]+)", " ".join(cells)).group(1) if re.search(r"(\d+[A-Z]+)", " ".join(cells)) else None
            prog = re.search(r"BA\s+[^)]+", " ".join(cells))
            data["program"] = prog.group(0) if prog else None
    return data


def parse_activities_table(table: BeautifulSoup) -> list[dict]:
    rows = table.find_all("tr")[2:]  # saute les deux lignes d’en-tête
    activities = []
    for row in rows:
        cols = [c.get_text(strip=True) for c in row.find_all("td")]
        if len(cols) >= 6:
            activities.append({
                "code": cols[0],
                "title": cols[1],
                "hours_Q1": cols[2],
                "hours_Q2": cols[3],
                "teachers": [t.strip() for t in cols[4].split(",") if t.strip()],
                "language": cols[5]
            })
    return activities


def parse_evaluation_table(table: BeautifulSoup) -> list[dict]:
    rows = table.find_all("tr")[2:]
    evaluations = []
    for row in rows:
        cols = [c.get_text(" ", strip=True) for c in row.find_all("td")]
        if len(cols) >= 9:
            linked = [div.get_text(strip=True) for div in row.find_all("div")]
            evaluations.append({
                "code": cols[0],
                "title": cols[1],
                "weight": cols[2],
                "type_Q1": cols[3],
                "type_Q2": cols[4],
                "type_Q3": cols[5],
                "teachers": [t.strip() for t in cols[6].split(",") if t.strip()],
                "language": cols[7],
                "linked_activities": linked
            })
    return evaluations


def parse_sections(soup: BeautifulSoup) -> dict:
    """Capture les sections textuelles h5 -> paragraphes/lists."""
    sections = {}
    current = None
    for tag in soup.find_all(["h5", "p", "ul", "ol", "li"]):
        if tag.name == "h5":
            current = tag.get_text(strip=True)
            sections[current] = ""
        elif current:
            text = tag.get_text(" ", strip=True)
            if text:
                sections[current] += text + "\n"
    return {k: v.strip() for k, v in sections.items() if v.strip()}


def parse_course_page(html: str, url: str) -> dict:
    soup = BeautifulSoup(html, "lxml")
    course = {"details_url": url}

    # --- partie supérieure
    overview = parse_table_overview(soup)
    course.update(overview)

    # --- activités organisées
    act_table = soup.find("h5", string=lambda s: s and "Activités organisées" in s)
    if act_table:
        table = act_table.find_next("table")
        if table:
            course["organized_activities"] = parse_activities_table(table)

    # --- activités évaluées
    eval_table = soup.find("h5", string=lambda s: s and "Activités évaluées" in s)
    if eval_table:
        table = eval_table.find_next("table")
        if table:
            course["evaluated_activities"] = parse_evaluation_table(table)

    # --- sections textuelles
    course["sections"] = parse_sections(soup)

    # --- code depuis URL si manquant
    if "code" not in course or not course["code"]:
        course["code"] = extract_code_from_url(url)

    return course


def collect_courses(input_path: Path) -> list[dict]:
    with open(input_path, "r", encoding="utf-8") as f:
        base_data = json.load(f)

    seen_codes = set()
    all_courses = []

    for formation in base_data["formations"]:
        for block in formation["blocks"]:
            for course in block["courses"]:
                url = course.get("details_url")
                if not url:
                    continue
                code = (course.get("code") or extract_code_from_url(url) or "").lower()
                if code in seen_codes:
                    continue

                print(f"📘 {url}")
                html = fetch_html(url)
                if not html:
                    continue
                try:
                    data = parse_course_page(html, url)
                    data["formation"] = formation["name"]
                    data["block"] = block["name"]
                    all_courses.append(data)
                    seen_codes.add(code)
                    time.sleep(0.5)
                except Exception as e:
                    print(f"[⚠️] Erreur parsing {url}: {e}")
    return all_courses


def main():
    print(f"🔍 Lecture de {INPUT_PATH}")
    courses = collect_courses(INPUT_PATH)
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
        json.dump(courses, f, ensure_ascii=False, indent=2)
    print(f"💾 Sauvegardé dans {OUTPUT_PATH} ({len(courses)} cours)")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n⛔ Interrompu")
        sys.exit(0)
    except Exception as e:
        print(f"[❌] Erreur critique: {e}")
        sys.exit(1)