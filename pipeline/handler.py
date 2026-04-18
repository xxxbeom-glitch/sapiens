import runpod
import traceback

print("handler.py: starting")


def handler(event):
    try:
        print("handler.py: importing main...")
        from main import run

        print("handler.py: import OK, running pipeline...")
        job_input = event.get("input", {})
        result = run()
        print("handler.py: pipeline completed")
        return {"status": "success", "message": "Pipeline completed successfully"}
    except Exception as e:
        error_trace = traceback.format_exc()
        print("handler.py: ERROR:", str(e))
        print(error_trace)
        return {
            "status": "error",
            "message": str(e),
            "traceback": error_trace,
        }


runpod.serverless.start({"handler": handler})
