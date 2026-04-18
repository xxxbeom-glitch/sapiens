import sys
import runpod

print("Python version:", sys.version, flush=True)
print("Starting handler module...", flush=True)

def handler(event):
    print("handler called", flush=True)
    return {"status": "ok"}

runpod.serverless.start({"handler": handler})
