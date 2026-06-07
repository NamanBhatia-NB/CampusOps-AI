"""
CampusOps AI — Automation Runner
Executes automation rules received from the Spring Boot backend.
"""
import logging
import requests
from datetime import datetime
from worker.config import config

logger = logging.getLogger(__name__)


class AutomationRunner:
    """Runs automation rules delegated by the backend."""

    def __init__(self):
        self.backend_url = config.BACKEND_URL

    def execute_rule(self, rule_data):
        """
        Execute an automation rule.

        Args:
            rule_data: Dict with rule configuration from backend

        Returns:
            Dict with execution result
        """
        rule_id = rule_data.get("ruleId")
        action_type = rule_data.get("actionType")
        action_config = rule_data.get("actionConfig", {})
        target_leads = rule_data.get("targetLeads", [])

        logger.info(
            f"Executing rule {rule_id}: {action_type} on {len(target_leads)} leads"
        )

        results = []
        for lead in target_leads:
            try:
                result = self._execute_action(action_type, action_config, lead)
                results.append({
                    "leadId": lead.get("id"),
                    "status": "SUCCESS",
                    "message": result.get("message", "Action completed"),
                })
            except Exception as e:
                logger.error(f"Rule execution failed for lead {lead.get('id')}: {e}")
                results.append({
                    "leadId": lead.get("id"),
                    "status": "FAILURE",
                    "message": str(e),
                })

        success_count = sum(1 for r in results if r["status"] == "SUCCESS")
        logger.info(
            f"Rule {rule_id} completed: {success_count}/{len(results)} successful"
        )

        return {
            "ruleId": rule_id,
            "totalProcessed": len(results),
            "successCount": success_count,
            "failureCount": len(results) - success_count,
            "results": results,
            "executedAt": datetime.now().isoformat(),
        }

    def _execute_action(self, action_type, action_config, lead):
        """Execute a specific action on a lead."""
        actions = {
            "CREATE_TASK": self._create_task,
            "SEND_NOTIFICATION": self._send_notification,
            "UPDATE_STATUS": self._update_status,
            "SEND_MESSAGE": self._send_message,
        }

        action_fn = actions.get(action_type)
        if not action_fn:
            raise ValueError(f"Unknown action type: {action_type}")

        return action_fn(action_config, lead)

    def _create_task(self, config_data, lead):
        """Create a task via the backend API."""
        payload = {
            "leadId": lead.get("id"),
            "title": config_data.get("taskTitle", "Follow up required"),
            "description": config_data.get(
                "taskDescription",
                f"Auto-generated task for lead: {lead.get('fullName', 'Unknown')}",
            ),
            "priority": config_data.get("taskPriority", "MEDIUM"),
            "assignedTo": lead.get("ownerId"),
        }

        response = requests.post(
            f"{self.backend_url}/api/tasks",
            json=payload,
            timeout=10,
        )
        response.raise_for_status()

        return {"message": f"Task created for lead {lead.get('fullName')}"}

    def _send_notification(self, config_data, lead):
        """Send a notification via the backend API."""
        payload = {
            "userId": config_data.get("notifyUserId") or lead.get("ownerId"),
            "title": config_data.get("notificationTitle", "Lead Alert"),
            "message": config_data.get(
                "notificationMessage",
                f"Alert for lead: {lead.get('fullName', 'Unknown')}",
            ),
            "type": config_data.get("notificationType", "ALERT"),
            "referenceType": "LEAD",
            "referenceId": lead.get("id"),
        }

        response = requests.post(
            f"{self.backend_url}/api/notifications",
            json=payload,
            timeout=10,
        )
        response.raise_for_status()

        return {"message": f"Notification sent for lead {lead.get('fullName')}"}

    def _update_status(self, config_data, lead):
        """Update lead status via the backend API."""
        new_status = config_data.get("newStatus", "FOLLOW_UP")
        payload = {"status": new_status}

        response = requests.patch(
            f"{self.backend_url}/api/leads/{lead.get('id')}/status",
            json=payload,
            timeout=10,
        )
        response.raise_for_status()

        return {
            "message": f"Lead {lead.get('fullName')} status updated to {new_status}"
        }

    def _send_message(self, config_data, lead):
        """Send a message to a lead via the backend API."""
        payload = {
            "leadId": lead.get("id"),
            "content": config_data.get(
                "messageTemplate",
                f"Hello {lead.get('fullName', '')}, this is an automated follow-up.",
            ),
            "channel": config_data.get("channel", "SYSTEM"),
            "messageType": "TEMPLATE",
        }

        response = requests.post(
            f"{self.backend_url}/api/conversations/send",
            json=payload,
            timeout=10,
        )
        response.raise_for_status()

        return {"message": f"Message sent to lead {lead.get('fullName')}"}
