package com.appointementservice.repository;

import com.appointementservice.entity.Appointment;
import com.appointementservice.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    boolean existsByDoctorNameIgnoreCaseAndAppointmentTimeAndStatus(
            String doctorName,
            LocalDateTime appointmentTime,
            AppointmentStatus status
    );

    List<Appointment> findByPatientEmailIgnoreCaseOrderByAppointmentTimeDesc(String patientEmail);

    List<Appointment> findByDoctorNameIgnoreCaseOrderByAppointmentTimeDesc(String doctorName);

    List<Appointment> findByDoctorNameIgnoreCaseAndStatusAndAppointmentTimeBetween(
            String doctorName,
            AppointmentStatus status,
            LocalDateTime start,
            LocalDateTime end
    );
}
