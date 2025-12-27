package com.ayushsingh.doc_helper.features.usage_monitoring.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "user_token_usage", indexes = {
        @Index(name = "idx_user_created_at", columnList = "user_id, created_at"),
        @Index(name = "idx_document_user", columnList = "document_id, user_id"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_thread_id", columnList = "thread_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTokenUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "thread_id", length = 255)
    private String threadId;

    @Column(name = "message_id", length = 255)
    private String messageId;

    @Column(name = "prompt_tokens", nullable = false)
    private Long promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    private Long completionTokens;

    @Column(name = "total_tokens", nullable = false)
    private Long totalTokens;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", length = 50, nullable = false)
    private ChatOperationType operationType;

    @Column(name = "document_chunks")
    private Integer documentChunks;

    @Column(name = "estimated_cost", precision = 10, scale = 6)
    private BigDecimal estimatedCost;

    @Column(name = "duration_ms")
    private Long durationMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
