package com.ayushsingh.doc_helper.features.doc_summary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "document_summaries",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"document_id", "version_number"}
        ),
        indexes = {
                @Index(name = "idx_doc_summary_document", columnList = "document_id")
        }
)
@Getter
@Setter
public class DocumentSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SummaryTone tone;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SummaryLength length;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "word_count", nullable = false)
    private Integer wordCount;

    @Column(name = "tokens_used", nullable = false)
    private Integer tokensUsed;

    @CreationTimestamp
    private Instant createdAt;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DocumentSummary summary = (DocumentSummary) o;
        return Objects.equals(id, summary.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
