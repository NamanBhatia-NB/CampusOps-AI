"""
CampusOps AI — Python Worker Service
Flask API for Google Sheets sync, report generation, and automation support.
"""
import logging
from flask import Flask, jsonify, request
from worker.config import config
from worker.sheets_sync import SheetsService
from worker.report_generator import ReportGenerator

# Configure logging
logging.basicConfig(
    level=logging.DEBUG if config.DEBUG else logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

app = Flask(__name__)
app.config.from_object(config)

# Initialize services
sheets_service = SheetsService()
report_generator = ReportGenerator()


# ==========================================
# Health & Status
# ==========================================

@app.route("/health", methods=["GET"])
def health():
    """Health check endpoint."""
    return jsonify({
        "status": "healthy",
        "service": "campusops-worker",
        "version": "1.0.0",
        "sheets_configured": sheets_service.is_configured(),
    })


@app.route("/status", methods=["GET"])
def status():
    """Detailed status of the worker."""
    return jsonify({
        "status": "running",
        "backend_url": config.BACKEND_URL,
        "sheets_configured": sheets_service.is_configured(),
        "scheduler_enabled": config.SCHEDULER_ENABLED,
    })


# ==========================================
# Google Sheets Sync
# ==========================================

@app.route("/api/sync/leads", methods=["POST"])
def sync_leads():
    """Export leads data to Google Sheets."""
    try:
        data = request.get_json()
        if not data:
            return jsonify({"error": "Request body is required"}), 400

        leads = data.get("leads", [])
        spreadsheet_title = data.get("spreadsheet_title", "CampusOps - Leads Export")
        sheet_name = data.get("sheet_name", "Leads")
        job_id = data.get("job_id")

        if not leads:
            return jsonify({"error": "No leads data provided"}), 400

        if not sheets_service.is_configured():
            return jsonify({
                "error": "Google Sheets not configured",
                "message": "Please provide Google service account credentials",
                "help": "Set GOOGLE_CREDENTIALS_FILE environment variable"
            }), 503

        result = sheets_service.export_leads(leads, spreadsheet_title, sheet_name)

        # Notify backend of completion
        _notify_backend(job_id, "COMPLETED", result.get("sheet_url"), len(leads))

        return jsonify({
            "status": "success",
            "message": f"Exported {len(leads)} leads to Google Sheets",
            "sheet_url": result.get("sheet_url"),
            "record_count": len(leads),
        })

    except Exception as e:
        logger.error(f"Failed to sync leads: {e}", exc_info=True)
        if data and data.get("job_id"):
            _notify_backend(data["job_id"], "FAILED", error=str(e))
        return jsonify({"error": str(e)}), 500


@app.route("/api/sync/report", methods=["POST"])
def sync_report():
    """Export report/dashboard data to Google Sheets."""
    try:
        data = request.get_json()
        if not data:
            return jsonify({"error": "Request body is required"}), 400

        report_data = data.get("report_data", {})
        spreadsheet_title = data.get("spreadsheet_title", "CampusOps - Report Export")
        job_id = data.get("job_id")

        if not report_data:
            return jsonify({"error": "No report data provided"}), 400

        if not sheets_service.is_configured():
            return jsonify({
                "error": "Google Sheets not configured",
                "message": "Please provide Google service account credentials",
            }), 503

        result = sheets_service.export_report(report_data, spreadsheet_title)
        record_count = sum(
            len(v) for v in report_data.values() if isinstance(v, list)
        )

        _notify_backend(job_id, "COMPLETED", result.get("sheet_url"), record_count)

        return jsonify({
            "status": "success",
            "message": "Report exported to Google Sheets",
            "sheet_url": result.get("sheet_url"),
            "record_count": record_count,
        })

    except Exception as e:
        logger.error(f"Failed to sync report: {e}", exc_info=True)
        if data and data.get("job_id"):
            _notify_backend(data["job_id"], "FAILED", error=str(e))
        return jsonify({"error": str(e)}), 500


# ==========================================
# Report Generation
# ==========================================

@app.route("/api/reports/generate", methods=["POST"])
def generate_report():
    """Generate a formatted report from raw data."""
    try:
        data = request.get_json()
        report_type = data.get("type", "summary")
        report_data = data.get("data", {})

        result = report_generator.generate(report_type, report_data)
        return jsonify({"status": "success", "report": result})

    except Exception as e:
        logger.error(f"Failed to generate report: {e}", exc_info=True)
        return jsonify({"error": str(e)}), 500


# ==========================================
# Helpers
# ==========================================

def _notify_backend(job_id, status, sheet_url=None, record_count=0, error=None):
    """Notify the Spring Boot backend about sync job completion."""
    if not job_id:
        return

    try:
        import requests
        payload = {
            "jobId": job_id,
            "status": status,
            "sheetUrl": sheet_url,
            "recordCount": record_count,
            "errorMessage": error,
        }
        requests.put(
            f"{config.BACKEND_URL}/api/sync/jobs/{job_id}/status",
            json=payload,
            timeout=10,
        )
        logger.info(f"Notified backend: job={job_id}, status={status}")
    except Exception as e:
        logger.warning(f"Failed to notify backend: {e}")


# ==========================================
# Entry Point
# ==========================================

if __name__ == "__main__":
    logger.info("Starting CampusOps Worker Service...")
    app.run(host="0.0.0.0", port=5000, debug=config.DEBUG)
