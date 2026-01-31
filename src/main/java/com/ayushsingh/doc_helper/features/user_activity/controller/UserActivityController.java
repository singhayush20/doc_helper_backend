package com.ayushsingh.doc_helper.features.user_activity.controller;

import com.ayushsingh.doc_helper.features.user_activity.dto.UserActivityResponseDto;
import com.ayushsingh.doc_helper.features.user_activity.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user-activities")
@RequiredArgsConstructor
public class UserActivityController {

    private final UserActivityService userActivityReadService;

    @GetMapping("/recent")
    public UserActivityResponseDto getRecentUserActivities(
            @RequestParam(defaultValue = "5") int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 10);

        return userActivityReadService.fetchUserActivity(
                safeLimit
        );
    }
}
