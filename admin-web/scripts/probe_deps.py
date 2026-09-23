# -*- coding: utf-8 -*-
import sys
ok = []
missing = []
for m in ("edge_tts", "imageio_ffmpeg", "PIL"):
    try:
        __import__(m)
        ok.append(m)
    except Exception as e:
        missing.append(f"{m}: {type(e).__name__}")
print("PY:", sys.executable)
print("OK:", ", ".join(ok) or "none")
print("MISSING:", "; ".join(missing) or "none")
