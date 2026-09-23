"""Regenerate the bundled demon/undead NPC index from the OSRS Wiki Bucket API.

Run with Python 3: python tools/generate_monsters.py
The Wiki is queried only while maintaining the plugin, never by plugin users.
"""

import json
import sys
import urllib.parse
import urllib.request
from pathlib import Path


API = "https://oldschool.runescape.wiki/api.php"
SOURCE = "https://oldschool.runescape.wiki/w/Template:Infobox_Monster"
OUTPUT = Path(__file__).resolve().parents[1] / "src/main/resources/com/demonslayer/monsters.json"
FIELDS = "'page_name','page_name_sub','name','id','attribute','combat_level'"
PAGE_SIZE = 500
USER_AGENT = "DemonSlayerRuneLite/1.0 (monster metadata generator; https://github.com/runelite/plugin-hub)"


def fetch_rows(bosses_only=False):
    rows = []
    offset = 0
    while True:
        query = "bucket('infobox_monster').select({})".format(FIELDS)
        if bosses_only:
            query += ".where('Category:Bosses')"
        query += ".limit({}).offset({}).orderBy('page_name_sub','asc').run()".format(PAGE_SIZE, offset)
        url = API + "?" + urllib.parse.urlencode({"action": "bucket", "format": "json", "query": query})
        request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(request, timeout=30) as response:
            page = json.load(response).get("bucket")
        if not isinstance(page, list):
            raise ValueError("Wiki response lacks a bucket array at offset {}".format(offset))
        rows.extend(page)
        if len(page) < PAGE_SIZE:
            return rows
        offset += PAGE_SIZE


def make_index(rows, boss_rows):
    boss_pages = {row.get("page_name") for row in boss_rows}
    index = {}
    for row in rows:
        attributes = row.get("attribute") or []
        if isinstance(attributes, str):
            attributes = [attributes]
        attributes = sorted({str(a).strip().lower() for a in attributes})
        relevant = [a for a in attributes if a in ("demon", "undead")]
        if not relevant:
            continue
        page = row.get("page_name")
        name = row.get("name") or page
        level = row.get("combat_level")
        if not isinstance(page, str) or not page or not isinstance(name, str) or not name:
            raise ValueError("Eligible row has no name/page: {}".format(row))
        if level is None or level == "No":
            continue  # Some Wiki infoboxes describe non-combat entities.
        if not isinstance(level, int) or level <= 0:
            raise ValueError("Eligible row has invalid combat level: {}".format(row))
        ids = row.get("id") or []
        if not isinstance(ids, list):
            raise ValueError("Eligible row has invalid NPC IDs: {}".format(row))
        entry = {"name": name, "page": page, "level": level,
                 "attributes": relevant, "boss": page in boss_pages}
        for raw_id in ids:
            if not str(raw_id).isdigit():
                continue  # Wiki historical/non-game IDs are not live NPCs.
            npc_id = str(int(raw_id))
            previous = index.get(npc_id)
            if previous is not None and previous != entry:
                comparable = ("name", "page", "attributes", "boss")
                if any(previous[field] != entry[field] for field in comparable):
                    raise ValueError("Conflicting metadata for NPC {}: {} vs {}".format(npc_id, previous, entry))
                entry = dict(entry, level=min(previous["level"], entry["level"]))
            index[npc_id] = entry
    if not index:
        raise ValueError("Wiki query produced no eligible NPCs")
    ordered = {key: index[key] for key in sorted(index, key=lambda value: int(value))}
    eligible_pages = {entry["page"] for entry in ordered.values() if entry["boss"]}
    excluded = sorted({row["page_name"] for row in boss_rows
                       if row.get("page_name") and row["page_name"] not in eligible_pages})
    return ordered, excluded


def main():
    rows = fetch_rows()
    bosses = fetch_rows(bosses_only=True)
    index, excluded = make_index(rows, bosses)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps({"source": SOURCE, "npcs": index,
                                  "excludedBossNames": excluded}, ensure_ascii=False,
                                 separators=(",", ":")) + "\n", encoding="utf-8")
    print("Wrote {} eligible NPC IDs from {} Wiki rows".format(len(index), len(rows)))


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError) as error:
        print(error, file=sys.stderr)
        sys.exit(1)
