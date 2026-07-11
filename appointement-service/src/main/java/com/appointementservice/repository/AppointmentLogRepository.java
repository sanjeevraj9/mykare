package com.appointementservice.repository;

import com.appointementservice.entity.AppointmentLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentLogRepository extends JpaRepository<AppointmentLog, Long> {
    List<AppointmentLog> findByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);
}
