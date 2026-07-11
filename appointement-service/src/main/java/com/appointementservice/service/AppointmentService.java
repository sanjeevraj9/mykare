package com.appointementservice.service;

import com.appointementservice.dto.AppointmentEvent;
import com.appointementservice.dto.AppointmentLogResponse;
import com.appointementservice.dto.AppointmentRequest;
import com.appointementservice.dto.AppointmentResponse;
import com.appointementservice.dto.AvailableSlotResponse;
import com.appointementservice.entity.Appointment;
import com.appointementservice.entity.AppointmentLog;
import com.appointementservice.entity.AppointmentStatus;
import com.appointementservice.entity.NotificationStatus;
import com.appointementservice.exception.DuplicateResourceException;
import com.appointementservice.exception.ResourceNotFoundException;
import com.appointementservice.repository.AppointmentLogRepository;
import com.appointementservice.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentLogRepository appointmentLogRepository;
    private final AppointmentEventPublisher eventPublisher;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AppointmentLogRepository appointmentLogRepository,
            AppointmentEventPublisher eventPublisher
    ) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentLogRepository = appointmentLogRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AppointmentResponse create(AppointmentRequest request) {
        // Validate inputs
        if (request.patientEmail() == null || request.patientEmail().isBlank()) {
            throw new IllegalArgumentException("Patient email is required");
        }
        if (request.doctorName() == null || request.doctorName().isBlank()) {
            throw new IllegalArgumentException("Doctor name is required");
        }
        if (request.appointmentTime() == null) {
            throw new IllegalArgumentException("Appointment time is required");
        }
        
        // Check slot availability with a pessimistic lock to prevent race conditions
        ensureDoctorSlotAvailable(request.doctorName(), request.appointmentTime());

        Appointment appointment = new Appointment();
        appointment.setPatientName(request.patientName() != null ? request.patientName().trim() : "");
        appointment.setPatientEmail(request.patientEmail().trim().toLowerCase());
        appointment.setDoctorName(request.doctorName().trim());
        appointment.setAppointmentTime(request.appointmentTime());
        appointment.setReason(request.reason() != null ? request.reason().trim() : "");
        appointment.setNotes(request.notes() != null ? request.notes().trim() : "");

        Appointment saved = appointmentRepository.save(appointment);
        appointmentRepository.flush(); // Ensure DB commit before event publishing
        addLog(saved.getId(), "CREATED", "Appointment booked and notification event queued");
        
        // Publish event after successful DB commit
        eventPublisher.publish(toEvent(saved, "APPOINTMENT_BOOKED"));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findAll() {
        return appointmentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findById(Long id) {
        return toResponse(getAppointment(id));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findByPatientEmail(String patientEmail) {
        if (patientEmail == null || patientEmail.isBlank()) {
            throw new IllegalArgumentException("Patient email is required");
        }
        return appointmentRepository.findByPatientEmailIgnoreCaseOrderByAppointmentTimeDesc(patientEmail)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findByDoctorName(String doctorName) {
        if (doctorName == null || doctorName.isBlank()) {
            throw new IllegalArgumentException("Doctor name is required");
        }
        return appointmentRepository.findByDoctorNameIgnoreCaseOrderByAppointmentTimeDesc(doctorName)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> findAvailableSlots(String doctorName, LocalDate date) {
        LocalDateTime start = date.atTime(LocalTime.of(9, 0));
        LocalDateTime end = date.atTime(LocalTime.of(17, 0));
        Set<LocalDateTime> bookedSlots = new HashSet<>(
                appointmentRepository.findByDoctorNameIgnoreCaseAndStatusAndAppointmentTimeBetween(
                                doctorName.trim(),
                                AppointmentStatus.BOOKED,
                                start,
                                end
                        )
                        .stream()
                        .map(Appointment::getAppointmentTime)
                        .toList()
        );

        return java.util.stream.IntStream.range(9, 17)
                .mapToObj(hour -> date.atTime(hour, 0))
                .map(slot -> new AvailableSlotResponse(
                        doctorName.trim(),
                        slot,
                        !bookedSlots.contains(slot)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentLogResponse> findLogs(Long appointmentId) {
        if (!appointmentRepository.existsById(appointmentId)) {
            throw new ResourceNotFoundException("Appointment not found");
        }
        return appointmentLogRepository.findByAppointmentIdOrderByCreatedAtDesc(appointmentId)
                .stream()
                .map(log -> new AppointmentLogResponse(
                        log.getId(),
                        log.getAppointmentId(),
                        log.getAction(),
                        log.getMessage(),
                        log.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public AppointmentResponse update(Long id, AppointmentRequest request) {
        Appointment appointment = getAppointment(id);
        boolean slotChanged = !appointment.getDoctorName().equalsIgnoreCase(request.doctorName())
                || !appointment.getAppointmentTime().equals(request.appointmentTime());
        if (slotChanged) {
            ensureDoctorSlotAvailable(request.doctorName(), request.appointmentTime());
        }

        appointment.setPatientName(request.patientName().trim());
        appointment.setPatientEmail(request.patientEmail().trim().toLowerCase());
        appointment.setDoctorName(request.doctorName().trim());
        appointment.setAppointmentTime(request.appointmentTime());
        appointment.setReason(request.reason().trim());
        appointment.setNotes(request.notes());
        appointment.markUpdated();

        Appointment saved = appointmentRepository.save(appointment);
        addLog(saved.getId(), "UPDATED", "Appointment details updated");
        eventPublisher.publish(toEvent(saved, "APPOINTMENT_UPDATED"));
        return toResponse(saved);
    }

    @Transactional
    public AppointmentResponse updateStatus(Long id, AppointmentStatus status) {
        Appointment appointment = getAppointment(id);
        if (status == AppointmentStatus.BOOKED) {
            boolean sameSlotBooked = appointmentRepository.existsByDoctorNameIgnoreCaseAndAppointmentTimeAndStatus(
                    appointment.getDoctorName(),
                    appointment.getAppointmentTime(),
                    AppointmentStatus.BOOKED
            );
            if (sameSlotBooked && appointment.getStatus() != AppointmentStatus.BOOKED) {
                throw new DuplicateResourceException("Doctor already has a booked appointment at this time");
            }
        }
        appointment.setStatus(status);
        appointment.markUpdated();
        Appointment saved = appointmentRepository.save(appointment);
        addLog(saved.getId(), status.name(), "Appointment status changed to " + status.name());
        eventPublisher.publish(toEvent(saved, "APPOINTMENT_" + status.name()));
        return toResponse(saved);
    }

    @Transactional
    public AppointmentResponse cancel(Long id) {
        return updateStatus(id, AppointmentStatus.CANCELLED);
    }

    @Transactional
    public AppointmentResponse updateNotificationStatus(Long id, NotificationStatus status, String message) {
        Appointment appointment = getAppointment(id);
        appointment.setNotificationStatus(status);
        appointment.markUpdated();
        Appointment saved = appointmentRepository.save(appointment);
        addLog(
                saved.getId(),
                "NOTIFICATION_" + status.name(),
                message == null || message.isBlank()
                        ? "Notification status changed to " + status.name()
                        : message.trim()
        );
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Appointment appointment = getAppointment(id);
        appointmentRepository.delete(appointment);
        addLog(id, "DELETED", "Appointment deleted");
        eventPublisher.publish(toEvent(appointment, "APPOINTMENT_DELETED"));
    }

    private Appointment getAppointment(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    }

    private void ensureDoctorSlotAvailable(String doctorName, java.time.LocalDateTime appointmentTime) {
        boolean exists = appointmentRepository.existsByDoctorNameIgnoreCaseAndAppointmentTimeAndStatus(
                doctorName.trim(),
                appointmentTime,
                AppointmentStatus.BOOKED
        );
        if (exists) {
            throw new DuplicateResourceException("Doctor already has a booked appointment at this time");
        }
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatientName(),
                appointment.getPatientEmail(),
                appointment.getDoctorName(),
                appointment.getAppointmentTime(),
                appointment.getReason(),
                appointment.getNotes(),
                appointment.getStatus(),
                appointment.getNotificationStatus(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }

    private AppointmentEvent toEvent(Appointment appointment, String eventType) {
        return new AppointmentEvent(
                appointment.getId(),
                appointment.getPatientName(),
                appointment.getPatientEmail(),
                appointment.getDoctorName(),
                appointment.getAppointmentTime(),
                appointment.getStatus(),
                eventType
        );
    }

    private void addLog(Long appointmentId, String action, String message) {
        AppointmentLog log = new AppointmentLog();
        log.setAppointmentId(appointmentId);
        log.setAction(action);
        log.setMessage(message);
        appointmentLogRepository.save(log);
    }
}
