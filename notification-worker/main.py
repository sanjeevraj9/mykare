import json
import logging
import os
import signal
import sys
from dataclasses import dataclass

import requests
from kafka import KafkaConsumer


logging.basicConfig(
    level=os.getenv("LOG_LEVEL", "INFO"),
    format="%(asctime)s %(levelname)s %(message)s",
)
logger = logging.getLogger("notification-worker")


@dataclass(frozen=True)
class AppointmentEvent:
    appointment_id: int
    patient_name: str
    patient_email: str
    doctor_name: str
    appointment_time: str
    status: str
    event_type: str

    @staticmethod
    def from_message(payload: dict) -> "AppointmentEvent":
        return AppointmentEvent(
            appointment_id=payload["appointmentId"],
            patient_name=payload["patientName"],
            patient_email=payload["patientEmail"],
            doctor_name=payload["doctorName"],
            appointment_time=payload["appointmentTime"],
            status=payload["status"],
            event_type=payload["eventType"],
        )


def build_consumer() -> KafkaConsumer:
    return KafkaConsumer(
        os.getenv("APPOINTMENT_TOPIC", "appointment-events"),
        bootstrap_servers=os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
        group_id=os.getenv("KAFKA_CONSUMER_GROUP", "notification-worker"),
        auto_offset_reset=os.getenv("KAFKA_AUTO_OFFSET_RESET", "earliest"),
        enable_auto_commit=True,
        value_deserializer=lambda raw: json.loads(raw.decode("utf-8")),
    )


def send_notification(event: AppointmentEvent) -> None:
    # Assignment-friendly stub: replace with SMTP/SMS/WhatsApp integration later.
    logger.info(
        "Notification: %s for appointment %s. Patient=%s <%s>, Doctor=%s, Time=%s, Status=%s",
        event.event_type,
        event.appointment_id,
        event.patient_name,
        event.patient_email,
        event.doctor_name,
        event.appointment_time,
        event.status,
    )


def update_processing_status(event: AppointmentEvent, status: str, message: str) -> None:
    service_url = os.getenv("APPOINTMENT_SERVICE_URL", "http://localhost:8087")
    endpoint = f"{service_url}/api/appointments/internal/{event.appointment_id}/notification-status"
    try:
        response = requests.patch(
            endpoint,
            json={"status": status, "message": message},
            timeout=5,
        )
        response.raise_for_status()
        logger.info("Updated appointment %s notification status to %s", event.appointment_id, status)
    except requests.RequestException as exc:
        logger.warning("Could not update notification status for appointment %s: %s", event.appointment_id, exc)


def main() -> int:
    running = True

    def stop(_signum, _frame):
        nonlocal running
        running = False

    signal.signal(signal.SIGINT, stop)
    signal.signal(signal.SIGTERM, stop)

    consumer = build_consumer()
    logger.info("Notification worker started")
    try:
        while running:
            records = consumer.poll(timeout_ms=1000)
            for batch in records.values():
                for record in batch:
                    try:
                        event = AppointmentEvent.from_message(record.value)
                        update_processing_status(event, "PROCESSING", "Python worker started processing event")
                        send_notification(event)
                        update_processing_status(event, "SENT", "Notification processed by Python worker")
                    except (KeyError, TypeError, ValueError) as exc:
                        logger.warning("Skipping invalid appointment event: %s", exc)
    finally:
        consumer.close()
        logger.info("Notification worker stopped")

    return 0


if __name__ == "__main__":
    sys.exit(main())
