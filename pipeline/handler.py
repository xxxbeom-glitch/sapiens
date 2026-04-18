"""RunPod Serverless entrypoint."""

from __future__ import annotations

import runpod

from main import run


def handler(job):
    try:
        run()
        return {"status": "success"}
    except Exception as e:
        return {"status": "error", "message": str(e)}


runpod.serverless.start({"handler": handler})
