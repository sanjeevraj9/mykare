package com.appointementservice.service;

import com.appointementservice.dto.AppointmentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AppointmentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventPublisher.class);

    private final KafkaTemplate<String, AppointmentEvent> kafkaTemplate;
    private final String topic;

    public AppointmentEventPublisher(
            KafkaTemplate<String, AppointmentEvent> kafkaTemplate,
            @Value("${app.kafka.appointment-topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Async("taskExecutor")
    public void publish(AppointmentEvent event) {
        try {
            kafkaTemplate.send(topic, String.valueOf(event.appointmentId()), event)
                    .get(10, java.util.concurrent.TimeUnit.SECONDS);
            log.info("Published appointment event {} to {}", event.appointmentId(), topic);
        } catch (Exception ex) {
            // Kafka is optional - log but don't fail the appointment creation
            log.warn("Failed to publish appointment event {} to Kafka (this is non-critical): {}", 
                    event.appointmentId(), ex.getMessage());
        }
    }
}
