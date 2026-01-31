package com.ayushsingh.doc_helper.features.user_activity.dto;

public record UserActivityResolution(
        boolean promoteDominant, // flag to check if dominant action must be updated
        boolean updateLastAction, // flag to check if last action must be updated
        boolean updateRedis, // flag to check if the updated record must be added to the redis buffer
        boolean scheduleDbWrite // flag to check if the updated record must be written to the database - might not be required if the record is already in the buffer
) {}

