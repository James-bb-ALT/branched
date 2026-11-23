#!/usr/bin/env python3
"""Python launcher wrapper for Branched's Phase 7 WorldMigrationTool."""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def main() -> int:
    if len(sys.argv) < 2:
        print(
            "Usage: python tools/migrate_dt_world.py "
            "--dry-run|--apply|--rollback --world <path> [--backup <path>] [--report <path>]",
            file=sys.stderr,
        )
        return 2
    args_str = " ".join(f'"{a}"' for a in sys.argv[1:])
    cmd = ["gradlew.bat" if os.name == "nt" else "./gradlew", "migrateWorld", f"--args={args_str}"]
    return subprocess.call(cmd, cwd=ROOT)


if __name__ == "__main__":
    raise SystemExit(main())
