"""
CampusOps AI — Report Generator
Processes raw data into formatted report structures.
"""
import logging
from datetime import datetime

logger = logging.getLogger(__name__)


class ReportGenerator:
    """Generates formatted reports from raw CRM data."""

    def generate(self, report_type, data):
        """
        Generate a report based on type.

        Args:
            report_type: Type of report (summary, leads_by_status, counselor_performance, conversion)
            data: Raw data dictionary

        Returns:
            Formatted report dictionary
        """
        generators = {
            "summary": self._generate_summary,
            "leads_by_status": self._generate_leads_by_status,
            "counselor_performance": self._generate_counselor_performance,
            "conversion": self._generate_conversion_report,
            "follow_up": self._generate_follow_up_report,
        }

        generator = generators.get(report_type, self._generate_summary)
        return generator(data)

    def _generate_summary(self, data):
        """Generate an executive summary report."""
        leads = data.get("leads", [])
        total = len(leads)

        status_counts = {}
        source_counts = {}
        priority_counts = {}

        for lead in leads:
            status = lead.get("status", "UNKNOWN")
            source = lead.get("source", "Unknown")
            priority = lead.get("priority", "MEDIUM")

            status_counts[status] = status_counts.get(status, 0) + 1
            source_counts[source] = source_counts.get(source, 0) + 1
            priority_counts[priority] = priority_counts.get(priority, 0) + 1

        admitted = status_counts.get("ADMITTED", 0)
        conversion_rate = (admitted / total * 100) if total > 0 else 0

        return {
            "report_type": "Executive Summary",
            "generated_at": datetime.now().isoformat(),
            "total_leads": total,
            "conversion_rate": round(conversion_rate, 2),
            "status_breakdown": status_counts,
            "source_breakdown": source_counts,
            "priority_breakdown": priority_counts,
            "key_metrics": {
                "new_leads": status_counts.get("NEW", 0),
                "in_pipeline": total - status_counts.get("ADMITTED", 0) - status_counts.get("LOST", 0),
                "admitted": admitted,
                "lost": status_counts.get("LOST", 0),
            },
        }

    def _generate_leads_by_status(self, data):
        """Generate leads grouped by status."""
        leads = data.get("leads", [])
        grouped = {}

        for lead in leads:
            status = lead.get("status", "UNKNOWN")
            if status not in grouped:
                grouped[status] = []
            grouped[status].append({
                "name": lead.get("fullName", ""),
                "email": lead.get("email", ""),
                "source": lead.get("source", ""),
                "owner": lead.get("ownerName", ""),
                "created": lead.get("createdAt", ""),
            })

        return {
            "report_type": "Leads by Status",
            "generated_at": datetime.now().isoformat(),
            "groups": grouped,
            "totals": {status: len(items) for status, items in grouped.items()},
        }

    def _generate_counselor_performance(self, data):
        """Generate counselor performance report."""
        leads = data.get("leads", [])
        counselors = {}

        for lead in leads:
            owner = lead.get("ownerName", "Unassigned")
            if owner not in counselors:
                counselors[owner] = {
                    "total": 0,
                    "admitted": 0,
                    "lost": 0,
                    "in_progress": 0,
                    "conversion_rate": 0,
                }

            counselors[owner]["total"] += 1
            status = lead.get("status", "")
            if status == "ADMITTED":
                counselors[owner]["admitted"] += 1
            elif status == "LOST":
                counselors[owner]["lost"] += 1
            else:
                counselors[owner]["in_progress"] += 1

        # Calculate conversion rates
        for name, stats in counselors.items():
            if stats["total"] > 0:
                stats["conversion_rate"] = round(
                    stats["admitted"] / stats["total"] * 100, 2
                )

        # Sort by conversion rate descending
        sorted_counselors = dict(
            sorted(counselors.items(), key=lambda x: x[1]["conversion_rate"], reverse=True)
        )

        return {
            "report_type": "Counselor Performance",
            "generated_at": datetime.now().isoformat(),
            "counselors": sorted_counselors,
        }

    def _generate_conversion_report(self, data):
        """Generate conversion funnel report."""
        leads = data.get("leads", [])
        funnel_order = ["NEW", "CONTACTED", "FOLLOW_UP", "QUALIFIED", "ADMITTED"]
        funnel = {status: 0 for status in funnel_order}
        lost_at_stage = {status: 0 for status in funnel_order}

        for lead in leads:
            status = lead.get("status", "NEW")
            if status in funnel:
                funnel[status] += 1
            if status == "LOST":
                # Track where leads drop off (simplified)
                lost_at_stage["NEW"] += 1

        total = len(leads)
        funnel_rates = {}
        for status in funnel_order:
            funnel_rates[status] = {
                "count": funnel[status],
                "percentage": round(funnel[status] / total * 100, 2) if total > 0 else 0,
            }

        return {
            "report_type": "Conversion Funnel",
            "generated_at": datetime.now().isoformat(),
            "total_leads": total,
            "funnel": funnel_rates,
        }

    def _generate_follow_up_report(self, data):
        """Generate follow-up status report."""
        leads = data.get("leads", [])
        overdue = []
        upcoming = []
        no_follow_up = []

        now = datetime.now().isoformat()

        for lead in leads:
            follow_up = lead.get("nextFollowUp")
            if not follow_up:
                if lead.get("status") not in ("ADMITTED", "LOST"):
                    no_follow_up.append({
                        "name": lead.get("fullName", ""),
                        "status": lead.get("status", ""),
                        "owner": lead.get("ownerName", ""),
                    })
            elif follow_up < now:
                overdue.append({
                    "name": lead.get("fullName", ""),
                    "follow_up_date": follow_up,
                    "owner": lead.get("ownerName", ""),
                    "status": lead.get("status", ""),
                })
            else:
                upcoming.append({
                    "name": lead.get("fullName", ""),
                    "follow_up_date": follow_up,
                    "owner": lead.get("ownerName", ""),
                    "status": lead.get("status", ""),
                })

        return {
            "report_type": "Follow-up Report",
            "generated_at": datetime.now().isoformat(),
            "overdue_count": len(overdue),
            "upcoming_count": len(upcoming),
            "no_followup_count": len(no_follow_up),
            "overdue": overdue,
            "upcoming": upcoming[:20],
            "no_follow_up": no_follow_up[:20],
        }
