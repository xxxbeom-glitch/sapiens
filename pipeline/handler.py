import sys
import runpod

print("Python version:", sys.version, flush=True)
print("Starting handler module...", flush=True)

def handler(event):
    try:
        print("Pipeline starting...", flush=True)
        from main import run

        result = run()
        print("Pipeline completed!", flush=True)
        return {"status": "success", "message": "Pipeline completed successfully"}
    except Exception as e:
        import traceback

        error_trace = traceback.format_exc()
        print("ERROR:", str(e), flush=True)
        print(error_trace, flush=True)
        return {
            "status": "error",
            "message": str(e),
            "traceback": error_trace,
        }


runpod.serverless.start({"handler": handler})
