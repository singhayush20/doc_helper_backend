package com.ayushsingh.doc_helper.features.product_features.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "feature_ui_config", indexes = {
                @Index(name = "idx_feature_ui_active_screen", columnList = "feature_id, screen_name, active"),
                @Index(name = "idx_feature_ui_feature_version", columnList = "feature_id, feature_ui_version")
})
@Entity
public class FeatureUIConfig {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        // The feature with which this UIConfig is associated with - can be a product
        // feature or any info ui component
        @Column(name = "feature_id", nullable = false)
        private Long featureId;

        @Column(name = "component_type", nullable = false)
        @Enumerated(EnumType.STRING)
        @JdbcTypeCode(SqlTypes.NAMED_ENUM)
        private UIComponentType componentType;

        @Column(name = "feature_ui_version", nullable = false)
        private Integer featureUiVersion; // this is the version of the ui which is
        // enabled for a particular feature, there can be multiple ui versions
        // for a feature

        @Column(name = "component_version", nullable = false)
        private Integer componentVersion; // this is the version of the ui component, for eg: banner v1, banner v2, etc

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(columnDefinition = "jsonb", nullable = false)
        private String uiJson;

        @Column(nullable = false)
        private boolean active;

        @Column(name = "screen_name", nullable = true)
        // This can be used to configure similar different UI components on different
        // screens
        private String screen;

        @CreationTimestamp
        private Instant createdAt;

        @UpdateTimestamp
        private Instant updatedAt;
}
