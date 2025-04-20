from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.analysis.router import router as analysis_router
from app.analysis.analytics_router import router as analytics_router
import uvicorn

app = FastAPI(title="Data Analysis Service")

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(analysis_router)
app.include_router(analytics_router)


@app.get("/")
async def root():
    return {
        "message": "Data Analysis Service API",
        "version": "1.0.0",
        "docs_url": "/docs"
    }


if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
