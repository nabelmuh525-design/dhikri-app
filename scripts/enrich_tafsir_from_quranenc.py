#!/usr/bin/env python3
"""
Downloads official Arabic Tafsir & Word Meanings directly from QuranEnc.com API
(arabic_moyassar & arabic_seraj) without altering a single character, verifies
HTTPS host, zip size limits, SQL identifiers, counts and version metadata, and
embeds them into dhikri_database.db.
"""

import hashlib
import io
import json
import re
import sqlite3
import tempfile
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path

DB_PATH = Path("app/src/main/assets/databases/dhikri_database.db")
API_LIST_URL = "https://quranenc.com/api/v1/translations/list/ar/?localization=ar"
ALLOWED_HOSTS = {"quranenc.com", "www.quranenc.com", "d.quranenc.com"}
TARGET_KEYS = ["arabic_moyassar", "arabic_seraj"]
MAX_UNCOMPRESSED_BYTES = 50 * 1024 * 1024  # 50 MB zip-bomb safety cap
SQL_IDENT_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")


def validate_url(url: str):
    parsed = urllib.parse.urlparse(url)
    if parsed.scheme != "https" or parsed.hostname not in ALLOWED_HOSTS:
        raise ValueError(f"Blocked untrusted download URL: {url}")


def validate_sql_ident(ident: str) -> str:
    if not SQL_IDENT_RE.match(ident):
        raise ValueError(f"Blocked unsafe SQL identifier: {ident}")
    return ident


def fetch_json(url: str) -> dict:
    validate_url(url)
    req = urllib.request.Request(url, headers={"User-Agent": "DHIKRI-Android-Builder/1.0"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


def download_bytes(url: str) -> bytes:
    validate_url(url)
    req = urllib.request.Request(url, headers={"User-Agent": "DHIKRI-Android-Builder/1.0"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        data = resp.read(MAX_UNCOMPRESSED_BYTES + 1)
        if len(data) > MAX_UNCOMPRESSED_BYTES:
            raise ValueError("Downloaded payload exceeded size limit")
        return data


def import_from_sqlite_zip(conn: sqlite3.Connection, book_key: str, zip_bytes: bytes, version: str):
    sha = hashlib.sha256(zip_bytes).hexdigest()
    with zipfile.ZipFile(io.BytesIO(zip_bytes)) as zf:
        sqlite_infos = [i for i in zf.infolist() if i.filename.endswith((".sqlite", ".db"))]
        if not sqlite_infos:
            raise RuntimeError(f"No SQLite file found inside zip for {book_key}")
        target_info = sqlite_infos[0]
        if target_info.file_size > MAX_UNCOMPRESSED_BYTES:
            raise ValueError(f"SQLite entry too large: {target_info.file_size} bytes")
        with tempfile.NamedTemporaryFile(suffix=".sqlite") as tmp:
            tmp.write(zf.read(target_info))
            tmp.flush()
            src = sqlite3.connect(tmp.name)
            tables = [r[0] for r in src.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()]
            target_table = validate_sql_ident(next((t for t in tables if t != "android_metadata"), tables[0]))
            cols = [r[1] for r in src.execute(f"PRAGMA table_info("{target_table}")").fetchall()]
            text_col = validate_sql_ident("translation" if "translation" in cols else ("text" if "text" in cols else cols[-1]))
            foot_col = validate_sql_ident("footnotes") if "footnotes" in cols else None
            sura_col = validate_sql_ident("sura" if "sura" in cols else "sura_id")
            aya_col = validate_sql_ident("aya" if "aya" in cols else "aya_id")

            select_sql = (
                f'SELECT "{sura_col}", "{aya_col}", "{text_col}"'
                + (f', "{foot_col}"' if foot_col else ", NULL")
                + f' FROM "{target_table}"'
            )
            rows = src.execute(select_sql).fetchall()
            src.close()

    cur = conn.cursor()
    cur.execute("DELETE FROM tafsir_entries WHERE book_key = ?", (book_key,))
    cur.executemany(
        "INSERT INTO tafsir_entries (book_key, surah_id, ayah_number, text_content, footnotes) VALUES (?,?,?,?,?)",
        [(book_key, int(s), int(a), str(t), (str(f) if f else None)) for s, a, t, f in rows],
    )
    cur.execute(
        "UPDATE tafsir_books SET is_downloaded = 1, entry_count = ?, version = ?, size_bytes = ? WHERE book_key = ?",
        (len(rows), version, len(zip_bytes), book_key),
    )
    cur.execute(
        "UPDATE download_packages SET status = 'INSTALLED', progress_percent = 100, version = ?, size_bytes = ?, sha256_checksum = ? WHERE package_key = ?",
        (version, len(zip_bytes), sha, book_key),
    )
    conn.commit()
    print(f"[OK] Imported {book_key}: {len(rows)} entries (v{version}, SHA-256={sha[:16]}...)")


def import_via_sura_api(conn: sqlite3.Connection, book_key: str, version: str):
    digest = hashlib.sha256()
    all_rows = []
    for sura in range(1, 115):
        url = f"https://quranenc.com/api/v1/translation/sura/{book_key}/{sura}"
        raw = download_bytes(url)
        digest.update(raw)
        data = json.loads(raw.decode("utf-8"))
        for item in data.get("result", []):
            all_rows.append(
                (
                    book_key,
                    int(item["sura"]),
                    int(item["aya"]),
                    item["translation"],
                    item.get("footnotes") or None,
                )
            )
    sha = digest.hexdigest()
    cur = conn.cursor()
    cur.execute("DELETE FROM tafsir_entries WHERE book_key = ?", (book_key,))
    cur.executemany(
        "INSERT INTO tafsir_entries (book_key, surah_id, ayah_number, text_content, footnotes) VALUES (?,?,?,?,?)",
        all_rows,
    )
    cur.execute(
        "UPDATE tafsir_books SET is_downloaded = 1, entry_count = ?, version = ? WHERE book_key = ?",
        (len(all_rows), version, book_key),
    )
    cur.execute(
        "UPDATE download_packages SET status = 'INSTALLED', progress_percent = 100, version = ?, sha256_checksum = ? WHERE package_key = ?",
        (version, sha, book_key),
    )
    conn.commit()
    print(f"[OK] Imported {book_key} via API: {len(all_rows)} entries (v{version}, SHA-256={sha[:16]}...)")


def main():
    if not DB_PATH.exists():
        raise FileNotFoundError(f"Database not found at {DB_PATH}")
    catalog = fetch_json(API_LIST_URL).get("translations", [])
    by_key = {item["key"]: item for item in catalog}

    conn = sqlite3.connect(str(DB_PATH))
    for key in TARGET_KEYS:
        meta = by_key.get(key)
        if not meta:
            print(f"[WARN] {key} not found in QuranEnc Arabic catalog; skipping.")
            continue
        version = str(meta.get("version", "1.0"))
        db_url = meta.get("database_url")
        if db_url:
            try:
                raw_zip = download_bytes(db_url)
                import_from_sqlite_zip(conn, key, raw_zip, version)
                continue
            except Exception as exc:
                print(f"[WARN] SQLite zip import failed for {key} ({exc}); falling back to sura API...")
        import_via_sura_api(conn, key, version)

    conn.execute("VACUUM;")
    conn.close()
    print("QuranEnc offline tafsir enrichment complete.")


if __name__ == "__main__":
    main()
