FROM python:3.11-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application
COPY worker/ ./worker/

# Create non-root user
RUN addgroup --system worker && adduser --system --ingroup worker worker
RUN mkdir -p /app/credentials && chown -R worker:worker /app
USER worker

EXPOSE 5000

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:5000/health')" || exit 1

CMD ["python", "-m", "worker.main"]
