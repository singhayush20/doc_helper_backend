package com.ayushsingh.doc_helper.features.usage_monitoring.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "user_token_usage", indexes = {
        @Index(name = "idx_user_timestamp", columnList = "user_id, timestamp"),
        @Index(name = "idx_document_user", columnList = "document_id, user_id"),
        @Index(name = "idx_timestamp", columnList = "timestamp"),
        @Index(name = "idx_thread_id", columnList = "thread_id")
})
@Data
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

    @Column(name = "thread_id")
    private String threadId;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "prompt_tokens", nullable = false)
    private Long promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    private Long completionTokens;

    @Column(name = "total_tokens", nullable = false)
    private Long totalTokens;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "operation_type", length = 50)
    @Enumerated(EnumType.STRING)
    private ChatOperationType operationType;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "document_chunks")
    private Integer documentChunks;

    @Column(name = "estimated_cost", precision = 10, scale = 6)
    private BigDecimal estimatedCost;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private Instant createdAt;
}
