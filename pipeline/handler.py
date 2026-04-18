import runpod
from main import run

def handler(job):
    job_input = job.get("input", {})
    try:
        result = run()
        return {"status": "success", "message": "Pipeline completed successfully"}
    except Exception as e:
        return {"status": "error", "message": str(e)}

runpod.serverless.start({"handler": handler})
