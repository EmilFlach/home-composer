#!/usr/bin/env python3
"""
Generate MdiCodepoints.kt from materialdesignicons.css.

Usage:
  python3 tools/gen_mdi_codepoints.py [path/to/materialdesignicons.css]

If no path is given, downloads the latest CSS from unpkg.
"""
import re
import sys
import urllib.request

CSS_URL = "https://unpkg.com/@mdi/font/css/materialdesignicons.css"
OUT_PATH = "composeApp/src/commonMain/kotlin/org/example/project/icons/MdiCodepoints.kt"


def parse_css(content: str) -> dict[str, int]:
    entries: dict[str, int] = {}
    current_name: str | None = None
    for line in content.split("\n"):
        line = line.strip()
        m = re.match(r"\.mdi-([\w-]+)::before\s*\{", line)
        if m:
            current_name = m.group(1)
        elif current_name:
            m2 = re.match(r'content:\s*"\\([0-9A-Fa-f]+)"\s*;', line)
            if m2:
                entries[current_name] = int(m2.group(1), 16)
                current_name = None
    return entries


def main() -> None:
    if len(sys.argv) > 1:
        with open(sys.argv[1], encoding="utf-8") as f:
            content = f.read()
        print(f"Read CSS from {sys.argv[1]}")
    else:
        print(f"Downloading {CSS_URL} ...")
        with urllib.request.urlopen(CSS_URL, timeout=30) as resp:
            content = resp.read().decode("utf-8")
        print("Download complete.")

    entries = parse_css(content)
    print(f"Parsed {len(entries)} icons.")

    sorted_entries = sorted(entries.items())
    chunk_size = 500
    chunks = [sorted_entries[i : i + chunk_size] for i in range(0, len(sorted_entries), chunk_size)]
    n = len(sorted_entries)

    lines = [
        "package org.example.project.icons",
        "",
        "internal val MDI_CODEPOINTS: Map<String, Int> = buildMdiMap()",
        "",
        "private fun buildMdiMap(): Map<String, Int> {",
        f"    val map = HashMap<String, Int>({n})",
    ]
    for i in range(len(chunks)):
        lines.append(f"    fillMdiChunk{i}(map)")
    lines.append("    return map")
    lines.append("}")

    for idx, chunk in enumerate(chunks):
        lines.append("")
        lines.append(f"private fun fillMdiChunk{idx}(map: HashMap<String, Int>) {{")
        for name, cp in chunk:
            lines.append(f'    map["{name}"] = 0x{cp:05X}')
        lines.append("}")
    lines.append("")

    with open(OUT_PATH, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print(f"Written {len(lines)} lines, {len(chunks)} chunks to {OUT_PATH}")


if __name__ == "__main__":
    main()
