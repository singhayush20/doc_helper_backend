package com.ayushsingh.doc_helper.features.user_activity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserActivityResponseDto {
    private List<UserActivityDto> userActivities;
}
