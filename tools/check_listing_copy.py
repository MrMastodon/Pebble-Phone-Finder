#!/usr/bin/env python3
"""Checks store-assets/listing-copy.md against Google Play's field limits.

Play silently blocks saving an over-long field in the console, which is an
annoying way to find out. Run this after editing the copy:

    python3 tools/check_listing_copy.py

Exits non-zero if anything is over budget.
"""
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
COPY = os.path.join(REPO, "store-assets", "listing-copy.md")

# Play Console limits, in characters including spaces.
LIMITS = {
    "Title": 30,
    "Tittel": 30,
    "Short description": 80,
    "Kort beskrivelse": 80,
    "Full description": 4000,
    "Fullstendig beskrivelse": 4000,
}


def fields(markdown):
    """Yield (heading, body) for every '### Heading' followed by a fenced block."""
    pattern = re.compile(r"^### (.+?)\n+```\n(.*?)\n```", re.MULTILINE | re.DOTALL)
    for match in pattern.finditer(markdown):
        yield match.group(1).strip(), match.group(2)


def main():
    with open(COPY, encoding="utf-8") as handle:
        markdown = handle.read()

    failures = 0
    checked = 0
    for heading, body in fields(markdown):
        limit = LIMITS.get(heading)
        length = len(body)
        if limit is None:
            print(f"  {heading}: {length} chars (no limit)")
            continue
        checked += 1
        over = length > limit
        failures += over
        status = "OVER LIMIT" if over else "ok"
        print(f"  {heading}: {length}/{limit} chars — {status}")

    if not checked:
        print("No limit-checked fields found; has the file's structure changed?")
        return 1

    print()
    if failures:
        print(f"{failures} field(s) over the limit.")
        return 1
    print("All fields within Play's limits.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
