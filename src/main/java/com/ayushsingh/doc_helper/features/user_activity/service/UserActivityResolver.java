package com.ayushsingh.doc_helper.features.user_activity.service;

import com.ayushsingh.doc_helper.features.user_activity.dto.UserActivityResolution;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivity;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivityMetadata;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivityType;
import org.springframework.stereotype.Service;

@Service
public class UserActivityResolver {

    public static UserActivityResolution resolve(
            UserActivity existing,
            UserActivityType incoming
    ) {
        if (existing == null) {
            return new UserActivityResolution(
                    true, true, true, true
            );
        }

        int incomingP = UserActivityMetadata.precedenceOf(incoming);
        int existingP =
                UserActivityMetadata.precedenceOf(existing.getDominantActivity());

        if (incomingP > existingP) {
            return new UserActivityResolution(
                    true, true, true, true
            );
        }

        return new UserActivityResolution(
                false, true, false, false
        );
    }
}

