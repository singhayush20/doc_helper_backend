package com.ayushsingh.doc_helper.features.user_activity.dto;

import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivityType;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UserActivityDto {
    private Long documentId;
    private UserActivityType dominantActivity;
    private Instant dominantAt;
    private UserActivityType lastAction;
    private String fileName;
}

