package com.appointementservice.controller;

import com.appointementservice.dto.AppointmentLogResponse;
import com.appointementservice.dto.AppointmentRequest;
import com.appointementservice.dto.AppointmentResponse;
import com.appointementservice.dto.AvailableSlotResponse;
import com.appointementservice.dto.NotificationStatusUpdateRequest;
import com.appointementservice.dto.StatusUpdateRequest;
import com.appointementservice.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    public AppointmentResponse create(@Valid @RequestBody AppointmentRequest request) {
        return appointmentService.create(request);
    }

    @GetMapping
    public List<AppointmentResponse> findAll(
            @RequestParam(required = false) String patientEmail,
            @RequestParam(required = false) String doctorName
    ) {
        if (patientEmail != null && !patientEmail.isBlank()) {
            return appointmentService.findByPatientEmail(patientEmail);
        }
        if (doctorName != null && !doctorName.isBlank()) {
            return appointmentService.findByDoctorName(doctorName);
        }
        return appointmentService.findAll();
    }

    @GetMapping("/{id}")
    public AppointmentResponse findById(@PathVariable Long id) {
        return appointmentService.findById(id);
    }

    @GetMapping("/available-slots")
    public List<AvailableSlotResponse> findAvailableSlots(
            @RequestParam String doctorName,
            @RequestParam LocalDate date
    ) {
        return appointmentService.findAvailableSlots(doctorName, date);
    }

    @GetMapping("/{id}/logs")
    public List<AppointmentLogResponse> findLogs(@PathVariable Long id) {
        return appointmentService.findLogs(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    public AppointmentResponse update(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRequest request
    ) {
        return appointmentService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public AppointmentResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        return appointmentService.updateStatus(id, request.status());
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    public AppointmentResponse cancel(@PathVariable Long id) {
        return appointmentService.cancel(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        appointmentService.delete(id);
    }

    @PatchMapping("/internal/{id}/notification-status")
    public AppointmentResponse updateNotificationStatus(
            @PathVariable Long id,
            @Valid @RequestBody NotificationStatusUpdateRequest request
    ) {
        return appointmentService.updateNotificationStatus(id, request.status(), request.message());
    }
}
