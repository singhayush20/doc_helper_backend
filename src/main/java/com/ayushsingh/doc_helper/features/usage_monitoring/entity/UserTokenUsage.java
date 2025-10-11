package com.ayushsingh.doc_helper.features.usage_monitoring.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(name = "timestamp", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant timestamp;

    @Column(name = "prompt_tokens", nullable = false)
    private Long promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    private Long completionTokens;

    @Column(name = "total_tokens", nullable = false)
    private Long totalTokens;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "operation_type", length = 50)
    private String operationType; // "chat_stream", "chat_call", "embedding"

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel; // For tracking which embedding model was used

    @Column(name = "document_chunks")
    private Integer documentChunks; // Number of chunks embedded

    @Column(name = "estimated_cost", precision = 10, scale = 6)
    private BigDecimal estimatedCost;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant createdAt;
}
