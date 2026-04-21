#!/usr/bin/env python3
import os
import sys

from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from hooks.shared.emit_event import main  # noqa: E402


if __name__ == "__main__":
    os.environ.setdefault("TRACKABLE_SOURCE", "claude")
    main()
