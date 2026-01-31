package com.ayushsingh.doc_helper.features.user_activity.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "user_activity",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "document_id"}
        ),
        indexes = {
                @Index(
                        name = "idx_user_activity_user_dominant_at",
                        columnList = "user_id, dominant_at DESC"
                )
        }
)
public class UserActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dominant_activity", nullable = false)
    private UserActivityType dominantActivity;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_action", nullable = false)
    private UserActivityType lastAction;

    @Column(name = "dominant_at", nullable = false)
    private Instant dominantAt;

    @Column(name = "last_action_at", nullable = false)
    private Instant lastActionAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @Type(JsonType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Override
    public String toString() {
        return "UserActivity{" +
                "id=" + id +
                ", userId=" + userId +
                ", documentId=" + documentId +
                ", dominantActivity=" + dominantActivity +
                ", lastAction=" + lastAction +
                ", dominantAt=" + dominantAt +
                ", lastActionAt=" + lastActionAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", metadata=" + metadata +
                '}';
    }
}

