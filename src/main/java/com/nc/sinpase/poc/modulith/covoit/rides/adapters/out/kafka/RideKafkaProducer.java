/*package com.nc.sinpase.poc.modulith.covoit.rides.adapters.out.kafka;

import com.nc.sinpase.poc.modulith.covoit.rides.RidePublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RideKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(RideKafkaProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    RideKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(RidePublishedEvent event) {
        kafkaTemplate.send("ride-published", event.rideId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[KAFKA] Failed to send RidePublishedEvent id={}",
                                event.rideId(), ex);
                    } else {
                        log.info("[KAFKA] Sent to partition={} offset={}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

}*/
