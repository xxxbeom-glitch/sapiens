import sys
import runpod

def handler(event):
    print("handler called", flush=True)
    return {"status": "ok"}

def main():
    print("Python version:", sys.version, flush=True)
    print("Starting...", flush=True)
    runpod.serverless.start({"handler": handler})

if __name__ == "__main__":
    main()
