"""
CampusOps AI — Google Sheets Sync Service
Handles exporting leads and report data to Google Sheets.
"""
import logging
import os
import json
from datetime import datetime

logger = logging.getLogger(__name__)


class SheetsService:
    """Service for Google Sheets integration using gspread."""

    def __init__(self):
        self._client = None
        self._initialized = False
        self._init_client()

    def _init_client(self):
        """Initialize the Google Sheets client with service account credentials."""
        try:
            credentials_file = os.getenv(
                "GOOGLE_CREDENTIALS_FILE", "credentials/service-account.json"
            )
            credentials_json = os.getenv("GOOGLE_SHEETS_CREDENTIALS", "")
            credentials_b64 = os.getenv("GOOGLE_SHEETS_CREDENTIALS_B64", "")

            if credentials_b64:
                # Load from base64 string
                import base64
                import gspread
                from google.oauth2.service_account import Credentials
                
                decoded_json = base64.b64decode(credentials_b64).decode('utf-8')
                creds_data = json.loads(decoded_json)
                scopes = [
                    "https://www.googleapis.com/auth/spreadsheets",
                    "https://www.googleapis.com/auth/drive",
                ]
                credentials = Credentials.from_service_account_info(
                    creds_data, scopes=scopes
                )
                self._client = gspread.authorize(credentials)
                self._initialized = True
                logger.info("Google Sheets client initialized from base64 env credentials")

            elif credentials_json:
                # Load from environment variable (JSON string)
                import gspread
                from google.oauth2.service_account import Credentials

                creds_data = json.loads(credentials_json)
                scopes = [
                    "https://www.googleapis.com/auth/spreadsheets",
                    "https://www.googleapis.com/auth/drive",
                ]
                credentials = Credentials.from_service_account_info(
                    creds_data, scopes=scopes
                )
                self._client = gspread.authorize(credentials)
                self._initialized = True
                logger.info("Google Sheets client initialized from env credentials")

            elif os.path.exists(credentials_file):
                # Load from file
                import gspread

                self._client = gspread.service_account(filename=credentials_file)
                self._initialized = True
                logger.info(
                    f"Google Sheets client initialized from file: {credentials_file}"
                )

            else:
                logger.warning(
                    "Google Sheets credentials not found. "
                    "Set GOOGLE_CREDENTIALS_FILE, GOOGLE_SHEETS_CREDENTIALS, or GOOGLE_SHEETS_CREDENTIALS_B64."
                )

        except ImportError:
            logger.warning("gspread not installed. Google Sheets sync disabled.")
        except Exception as e:
            logger.error(f"Failed to initialize Google Sheets client: {e}")

    def is_configured(self):
        """Check if Google Sheets integration is properly configured."""
        return self._initialized and self._client is not None

    def export_leads(self, leads, spreadsheet_title, sheet_name="Leads"):
        """
        Export leads data to a Google Sheets spreadsheet.

        Args:
            leads: List of lead dictionaries
            spreadsheet_title: Title for the spreadsheet
            sheet_name: Name for the worksheet

        Returns:
            dict with sheet_url
        """
        if not self.is_configured():
            raise RuntimeError("Google Sheets is not configured")

        logger.info(
            f"Exporting {len(leads)} leads to '{spreadsheet_title}/{sheet_name}'"
        )

        # Create or open spreadsheet
        try:
            spreadsheet = self._client.open(spreadsheet_title)
            logger.info(f"Opened existing spreadsheet: {spreadsheet_title}")
        except Exception:
            spreadsheet = self._client.create(spreadsheet_title)
            logger.info(f"Created new spreadsheet: {spreadsheet_title}")

        # Get or create worksheet
        try:
            worksheet = spreadsheet.worksheet(sheet_name)
            worksheet.clear()
        except Exception:
            worksheet = spreadsheet.add_worksheet(
                title=sheet_name, rows=len(leads) + 10, cols=20
            )

        # Build header row
        headers = [
            "ID", "Full Name", "Email", "Phone", "Source", "Status",
            "Priority", "Program Interest", "Owner", "City", "State",
            "Tags", "Lead Score", "Classification", "Last Contacted",
            "Next Follow-Up", "Created At"
        ]

        # Build data rows
        rows = [headers]
        for lead in leads:
            row = [
                str(lead.get("id", "")),
                lead.get("fullName", ""),
                lead.get("email", ""),
                lead.get("phone", ""),
                lead.get("source", ""),
                lead.get("status", ""),
                lead.get("priority", ""),
                lead.get("programInterest", ""),
                lead.get("ownerName", ""),
                lead.get("city", ""),
                lead.get("state", ""),
                lead.get("tags", ""),
                str(lead.get("leadScore", "")),
                lead.get("classification", ""),
                lead.get("lastContactedAt", ""),
                lead.get("nextFollowUp", ""),
                lead.get("createdAt", ""),
            ]
            rows.append(row)

        # Write to sheet
        worksheet.update(range_name="A1", values=rows)

        # Format header row (bold, background color)
        worksheet.format("A1:Q1", {
            "backgroundColor": {"red": 0.25, "green": 0.25, "blue": 0.75},
            "textFormat": {"bold": True, "foregroundColor": {"red": 1, "green": 1, "blue": 1}},
            "horizontalAlignment": "CENTER",
        })

        # Auto-resize columns
        try:
            spreadsheet.batch_update({
                "requests": [{
                    "autoResizeDimensions": {
                        "dimensions": {
                            "sheetId": worksheet.id,
                            "dimension": "COLUMNS",
                            "startIndex": 0,
                            "endIndex": len(headers),
                        }
                    }
                }]
            })
        except Exception as e:
            logger.warning(f"Could not auto-resize columns: {e}")

        sheet_url = spreadsheet.url
        logger.info(f"Leads exported successfully: {sheet_url}")

        return {"sheet_url": sheet_url, "record_count": len(leads)}

    def export_report(self, report_data, spreadsheet_title):
        """
        Export report/dashboard data to Google Sheets.
        Creates multiple worksheets for different sections.

        Args:
            report_data: Dict with section names as keys, list of dicts as values
            spreadsheet_title: Title for the spreadsheet

        Returns:
            dict with sheet_url
        """
        if not self.is_configured():
            raise RuntimeError("Google Sheets is not configured")

        logger.info(f"Exporting report to '{spreadsheet_title}'")

        # Create or open spreadsheet
        try:
            spreadsheet = self._client.open(spreadsheet_title)
        except Exception:
            spreadsheet = self._client.create(spreadsheet_title)

        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M")

        for section_name, section_data in report_data.items():
            if not isinstance(section_data, list) or not section_data:
                continue

            sheet_title = f"{section_name} ({timestamp})"[:100]

            try:
                worksheet = spreadsheet.add_worksheet(
                    title=sheet_title, rows=len(section_data) + 10, cols=20
                )
            except Exception:
                worksheet = spreadsheet.worksheet(sheet_title)
                worksheet.clear()

            # Headers from first item keys
            headers = list(section_data[0].keys())
            rows = [headers]

            for item in section_data:
                row = [str(item.get(h, "")) for h in headers]
                rows.append(row)

            worksheet.update(range_name="A1", values=rows)

            # Format header
            col_letter = chr(ord("A") + len(headers) - 1)
            worksheet.format(f"A1:{col_letter}1", {
                "backgroundColor": {"red": 0.15, "green": 0.15, "blue": 0.5},
                "textFormat": {"bold": True, "foregroundColor": {"red": 1, "green": 1, "blue": 1}},
            })

        sheet_url = spreadsheet.url
        logger.info(f"Report exported successfully: {sheet_url}")

        return {"sheet_url": sheet_url}
