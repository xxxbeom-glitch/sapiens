import runpod
from main import run

def handler(event):
    try:
        job_input = event.get("input", {})
        result = run()
        return {"status": "success", "message": "Pipeline completed successfully"}
    except Exception as e:
        import traceback
        return {
            "status": "error",
            "message": str(e),
            "traceback": traceback.format_exc()
        }

if __name__ == "__main__":
    runpod.serverless.start({"handler": handler})
