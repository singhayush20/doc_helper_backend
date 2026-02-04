package com.ayushsingh.doc_helper.features.product_features.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "feature_ui_config",
        indexes = {
                @Index(
                        name = "idx_feature_ui_active_screen",
                        columnList = "feature_id, screen_name, active"
                ),
                @Index(
                        name = "idx_feature_ui_feature_version",
                        columnList = "feature_id, feature_ui_version"
                )
        }
)
@Entity
public class FeatureUIConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The feature with which this UIConfig is associated with - can be a product feature or any info ui component
    @Column(name = "feature_id", nullable = false)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", insertable = false, updatable = false)
    private Long featureId;

    @Column(name = "component_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private UIComponentType componentType;

    @Column(nullable = false)
    private int featureUiVersion;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String uiJson;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "screen_name", nullable = true)
    // This can be used to configure similar different UI components on different screens
    private String screen;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}