package com.ayushsingh.doc_helper.features.user_activity.repository;

import com.ayushsingh.doc_helper.features.user_activity.dto.UserActivityWriteRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class UserActivityUpsertRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    private static final String UPSERT_SQL = """
        INSERT INTO user_activity (
            user_id,
            target_type,
            target_id,
            dominant_activity,
            dominant_at,
            last_action,
            last_action_at,
            metadata,
            created_at,
            updated_at
        )
        VALUES (
            :userId,
            :targetType,
            :targetId,
            :dominantActivity,
            :dominantAt,
            :lastAction,
            :lastActionAt,
            CAST(:metadata AS jsonb),
            now(),
            now()
        )
        ON CONFLICT (user_id, target_type, target_id)
        DO UPDATE SET
            dominant_activity = EXCLUDED.dominant_activity,
            dominant_at = EXCLUDED.dominant_at,
            last_action = EXCLUDED.last_action,
            last_action_at = EXCLUDED.last_action_at,
            metadata = user_activity.metadata || EXCLUDED.metadata,
            updated_at = now()
        """;

    public void batchUpsert(List<UserActivityWriteRequest> requests) {

        List<MapSqlParameterSource> batch = requests.stream()
                .map(this::toParams)
                .toList();

        jdbc.batchUpdate(
                UPSERT_SQL,
                batch.toArray(MapSqlParameterSource[]::new)
        );
    }

    private MapSqlParameterSource toParams(UserActivityWriteRequest cmd) {
        return new MapSqlParameterSource()
                .addValue("userId", cmd.userId())
                .addValue("targetType", cmd.targetType().name())
                .addValue("targetId", cmd.targetId())
                .addValue("dominantActivity", cmd.dominantActivity().name())
                .addValue(
                        "dominantAt",
                        Timestamp.from(cmd.dominantAt()),
                        Types.TIMESTAMP
                )
                .addValue("lastAction", cmd.lastAction().name())
                .addValue(
                        "lastActionAt",
                        Timestamp.from(cmd.lastActionAt()),
                        Types.TIMESTAMP
                )
                .addValue("metadata", serialize(cmd.metadata()), Types.OTHER);
    }

    private String serialize(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            throw new IllegalStateException("Metadata serialization failed", e);
        }
    }
}
