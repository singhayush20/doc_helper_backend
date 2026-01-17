package com.ayushsingh.doc_helper.features.payments.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;

@Entity
@Table(name = "payment_event_log", indexes = {
        @Index(name = "idx_event_provider_id", columnList = "provider_event_id", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProviderEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_event_id", nullable = false, length = 100)
    private String providerEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed", nullable = false)
    private boolean processed;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Type(JsonType.class)
    @Column(name = "raw_payload", columnDefinition = "jsonb", nullable = false)
    private String rawPayload;

}
