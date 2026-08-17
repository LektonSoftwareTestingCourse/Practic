import os
from fastapi import FastAPI

app = FastAPI(
    title="SERVICE_NAME",
    description="Microservice for СМП processing simulator",
    version="1.0.0",
)


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "service": "SERVICE_NAME",
        "dependencies": {},
    }


if __name__ == "__main__":
    import uvicorn

    port = int(os.getenv("PORT", "8080"))
    uvicorn.run(app, host="0.0.0.0", port=port)
