package com.ayushsingh.doc_helper.features.doc_summary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "document_chunks",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_document_chunks_doc_index",
                columnNames = {"document_id", "chunk_index"}
        ),
        indexes = {
                @Index(name = "idx_document_chunks_document", columnList = "document_id"),
                @Index(name = "idx_document_chunks_document_index", columnList = "document_id, chunk_index")
        }
)
@Getter
@Setter
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "content_text", nullable = false, columnDefinition = "TEXT")
    private String contentText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DocumentChunk chunk = (DocumentChunk) o;
        return Objects.equals(id, chunk.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
