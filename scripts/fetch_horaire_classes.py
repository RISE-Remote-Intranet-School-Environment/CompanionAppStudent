import json
import re
from datetime import datetime, timedelta
from pathlib import Path

import pdfplumber

# mets ici le dossier contenant les fichiers P1_horsem_0X_....pdf
PDF_DIR = Path("Horaire cours")
OUTPUT_JSON = "composeApp\\src\\commonMain\\composeResources\\files\\ecam_calendar_courses_schedule_2025.json"


def extract_monday_date_from_filename(name: str) -> datetime:
    """Parse la date du lundi à partir du nom du pdf."""
    match = re.search(r"_Lu_(\d{2})_(\d{2})_(\d{4})_au__", name)
    if not match:
        raise ValueError(f"Impossible de trouver la date dans {name}")
    day, month, year = map(int, match.groups())
    return datetime(year, month, day)


def french_day_name_to_index(label: str) -> int:
    mapping = {
        "LUNDI": 0,
        "MARDI": 1,
        "MERCREDI": 2,
        "JEUDI": 3,
        "VENDREDI": 4,
        "SAMEDI": 5,
        "DIMANCHE": 6,
    }
    compact = label.replace(" ", "")
    upper = compact.upper()
    for key, idx in mapping.items():
        if key in upper:
            return idx
    raise ValueError(f"Jour inconnu dans: {label}")


def find_time_columns(table):
    time_re = re.compile(r"\d{2}:\d{2}\^")
    for row in table:
        if not row:
            continue
        cols = [i for i, v in enumerate(row) if v and time_re.match(v)]
        if cols:
            return cols
    return []


def extract_group_labels(row, start, end):
    """Retourne [(col_start, year_option, group)] en interprétant la ligne d'entête."""
    cells = []
    for idx in range(start, end + 1):
        cell = row[idx] if idx < len(row) else ""
        if cell:
            cells.append((idx, cell.strip()))

    labels = []
    i = 0
    while i < len(cells):
        col, text = cells[i]
        candidate = text
        # pattern: "1" + "B" => "1B"
        if text.isdigit() and i + 1 < len(cells) and cells[i + 1][1].isalpha():
            candidate = text + cells[i + 1][1]
            i += 1
        # pattern: "1B" + "A - 1"
        if i + 1 < len(cells) and re.search(r"-\s*\d+$", cells[i + 1][1]):
            candidate = candidate + cells[i + 1][1]
            i += 1

        clean = candidate.replace(" ", "").upper()
        match = re.match(r"([0-9]+[A-Z]+)(?:-?(\d))?", clean)
        if match:
            year_option, group = match.groups()
            group_num = int(group) if group else 1
            labels.append((col, year_option, group_num))
        i += 1

    return labels


def build_column_bounds(page):
    xs = sorted(
        {round(r["x0"], 2) for r in page.rects if r.get("non_stroking_color") == (1.0, 1.0, 1.0)}
    )
    if not xs:
        return []
    last_x1 = max(r["x1"] for r in page.rects)
    bounds = [(xs[i], xs[i + 1]) for i in range(len(xs) - 1)]
    bounds.append((xs[-1], last_x1))
    return bounds


def build_time_mapper(page):
    words = page.extract_words()
    markers = []
    for w in words:
        m = re.match(r"(\d{2}:\d{2})\^?", w["text"])
        if m:
            mins = int(m.group(1)[:2]) * 60 + int(m.group(1)[3:])
            y = (w["top"] + w["bottom"]) / 2
            markers.append((round(y, 2), mins))
    markers = sorted(set(markers))

    grid_y = sorted(
        {round(r["top"], 2) for r in page.rects if r.get("non_stroking_color") == (1.0, 1.0, 1.0)}
    )
    start_map = []
    for y_center, mins in markers:
        below = [gy for gy in grid_y if gy <= y_center + 0.1]
        if not below:
            continue
        start_y = max(below)
        start_map.append((start_y, mins))
    start_map = sorted(set(start_map))

    def map_y(y):
        if not start_map:
            return None
        candidates = [mins for y_start, mins in start_map if y >= y_start - 0.5]
        if not candidates:
            return start_map[0][1]
        return candidates[-1]

    return map_y


def split_room_tokens(text: str):
    """Retourne (rooms_in_text, tokens_sans_rooms)."""
    tokens = text.split()
    room_pattern = re.compile(r"[12][A-Z]\d{2}")
    rooms = []
    remaining = []
    for t in tokens:
        if room_pattern.fullmatch(t):
            rooms.append(t)
        else:
            remaining.append(t)
    return rooms, remaining


def clean_course_name(raw_text: str, course_code: str, teachers, rooms_extra) -> str:
    to_remove = set()
    if course_code:
        to_remove.add(course_code)
    to_remove.update(teachers or [])
    to_remove.update(rooms_extra or [])

    def merge_split_tokens(tokens):
        merged = []
        i = 0
        stopwords = {"of", "de", "du", "la", "le", "les", "des", "et", "to", "in", "en", "da", "di", "and"}
        suffixes = {
            "atory",
            "age",
            "tion",
            "sion",
            "ment",
            "ance",
            "ence",
            "able",
            "ible",
            "ique",
            "ology",
            "ologie",
            "metry",
            "graphy",
            "nomy",
            "sis",
        }
        while i < len(tokens):
            tok = tokens[i]
            # recoller les fragments d'une lettre (ex: "l" + "abo" -> "labo")
            if (
                len(tok) == 1
                and tok.isalpha()
                and tok.islower()
                and i + 1 < len(tokens)
                and tokens[i + 1].isalpha()
                and tokens[i + 1].islower()
            ):
                tok = tok + tokens[i + 1]
                i += 1
            # Essayer de recoller les mots cassés ("Labor" + "atory", "huma" + "n", "motio" + "n", "Tourn" + "age")
            while (
                i + 1 < len(tokens)
                and tok.isalpha()
                and tokens[i + 1].isalpha()
                and tokens[i + 1].islower()
                and tok.lower() not in stopwords
                and tokens[i + 1].lower() not in stopwords
                and (
                    len(tokens[i + 1]) <= 3
                    or len(tok) <= 3
                    or tokens[i + 1].lower() in suffixes
                )
            ):
                tok += tokens[i + 1]
                i += 1
            merged.append(tok)
            i += 1
        return merged

    tokens = raw_text.split()
    filtered = [t for t in tokens if t not in to_remove]
    merged = merge_split_tokens(filtered)
    for idx, tok in enumerate(merged):
        if tok == "communicatio":
            merged[idx] = "communication"
    if merged and merged[0] == "abo":
        merged[0] = "labo"
    return " ".join(merged).strip()


def minutes_to_hhmm(minutes):
    h = minutes // 60
    m = minutes % 60
    return f"{h:02d}:{m:02d}"


def within_bounds(col_bounds, rect, tol=0.5):
    cols = []
    for idx, (x0, x1) in enumerate(col_bounds):
        if rect["x1"] > x0 + tol and rect["x0"] < x1 - tol:
            cols.append(idx)
    return cols


def parse_week_file(pdf_path: Path):
    monday = extract_monday_date_from_filename(pdf_path.name)
    week_num = int(re.search(r"_0?(\d+)_Semaine_du__", pdf_path.name).group(1))
    entries = []

    with pdfplumber.open(pdf_path) as pdf:
        for page in pdf.pages:
            table = page.extract_table()
            if not table or len(table) < 5:
                continue

            header_row = table[0]
            header_text = " ".join(filter(None, header_row)).upper().replace(" ", "")
            if "SEM" not in header_text:
                continue

            time_cols = find_time_columns(table)
            if not time_cols:
                continue

            group_header_row = table[1]
            series_row = table[2]
            col_bounds = build_column_bounds(page)
            time_mapper = build_time_mapper(page)
            if not col_bounds or not time_mapper:
                continue

            # chaque col de temps = un jour sur la page
            blocks = []
            for i, start_col in enumerate(time_cols):
                end_col = time_cols[i + 1] - 1 if i + 1 < len(time_cols) else len(header_row) - 1
                blocks.append((start_col, end_col))

            for block_start, block_end in blocks:
                day_header = " ".join(filter(None, header_row[block_start:block_end + 1]))
                try:
                    day_idx = french_day_name_to_index(day_header)
                except ValueError:
                    continue
                date = (monday + timedelta(days=day_idx)).strftime("%Y-%m-%d")
                day_name_fr = ["Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"][day_idx]

                group_chunks = extract_group_labels(group_header_row, block_start, block_end)
                starts = [c[0] for c in group_chunks]
                if not starts:
                    continue

                # associer chaque colonne à son groupe le plus proche
                col_to_group = {}
                for col in range(block_start + 1, block_end + 1):
                    nearest = min(starts, key=lambda s: abs(col - s))
                    col_to_group[col] = nearest

                groups = {}
                for start_idx, year_option, group in group_chunks:
                    cols = [c for c, g in col_to_group.items() if g == start_idx]
                    series = [series_row[c] for c in cols if c < len(series_row) and series_row[c]]
                    groups[start_idx] = {
                        "year_option": year_option,
                        "group": group,
                        "cols": cols,
                        "series": series,
                    }

                if not groups:
                    continue

                # rects colorés = blocs de cours
                course_rects = [
                    r
                    for r in page.rects
                    if r.get("non_stroking_color") and r["non_stroking_color"] != (1.0, 1.0, 1.0)
                ]

                for rect in course_rects:
                    cols_in_rect = within_bounds(col_bounds, rect)
                    # filtrer ceux qui sont dans le bloc de colonnes du jour
                    cols_in_block = [c for c in cols_in_rect if block_start < c <= block_end]
                    if not cols_in_block:
                        continue
                    # déterminer le groupe via la colonne dominante
                    dominant_col = cols_in_block[0]
                    group_start = col_to_group.get(dominant_col)
                    if group_start is None or group_start not in groups:
                        continue
                    g_val = groups[group_start]

                    series_list = [series_row[c] for c in cols_in_block if c < len(series_row) and series_row[c]]

                    y_start = rect["top"]
                    y_end = rect["bottom"]
                    start_min = time_mapper(y_start)
                    end_min = time_mapper(y_end)
                    if start_min is None or end_min is None:
                        continue
                    bbox = (
                        rect["x0"] + 0.5,
                        rect["top"] + 0.5,
                        rect["x1"] - 0.5,
                        rect["bottom"] - 0.5,
                    )
                    cropped = page.within_bbox(bbox, relative=False)
                    text = cropped.extract_text(x_tolerance=1) or ""
                    text = " ".join(text.split())
                    code_match = re.search(r"\b([A-Z]{2}\d[A-Z])\b", text)
                    teachers = re.findall(r"\b[A-Z0-9]{2,3}\b", text)
                    course_code = code_match.group(1) if code_match else ""
                    rooms_found, tokens_wo_rooms = split_room_tokens(text)
                    course_name = clean_course_name(" ".join(tokens_wo_rooms), course_code, teachers, rooms_found)
                    room = rooms_found

                    entry = {
                        "week": week_num,
                        "year_option": g_val["year_option"],
                        "group": g_val["group"],
                        "series": series_list if series_list else g_val["series"],
                        "date": date,
                        "day_name": day_name_fr,
                        "start_time": minutes_to_hhmm(start_min),
                        "end_time": minutes_to_hhmm(end_min),
                        "course_code": course_code,
                        "teachers": teachers,
                        "room": room,
                        "course_name": course_name,
                    }
                    entries.append(entry)

    return entries


def main():
    all_entries = []
    for pdf in sorted(PDF_DIR.glob("P1_horsem_*_Semaine_du__Lu_*_2025*.pdf")):
        week_num = int(re.search(r"_0?(\d+)_Semaine_du__", pdf.name).group(1))
        print(f"Traitement {pdf.name} (semaine {week_num})")
        all_entries.extend(parse_week_file(pdf))

    with open(OUTPUT_JSON, "w", encoding="utf-8") as f:
        json.dump(all_entries, f, ensure_ascii=False, indent=2)

    print(f"JSON écrit dans {OUTPUT_JSON}")


if __name__ == "__main__":
    main()
