import runpod

def handler(event):
    return {"status": "success", "message": "handler works", "event": str(event)}

runpod.serverless.start({"handler": handler})
