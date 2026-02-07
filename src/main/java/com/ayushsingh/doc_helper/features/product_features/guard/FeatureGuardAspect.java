package com.ayushsingh.doc_helper.features.product_features.guard;

import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureCodes;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureAccessService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class FeatureGuardAspect {

    private final FeatureAccessService featureAccessService;

    // Intercepts any method annotated with @RequireFeature
    @Around("@annotation(requireFeature)")
    public Object guard(
            // Represents the intercepted method call
            ProceedingJoinPoint proceedingJoinPoint,
            // The annotation placed on the intercepted method
            RequireFeature requireFeature
    ) throws Throwable {

        var user = UserContext.getCurrentUser();

        /*
        Verifies:
            User has an active subscription.
            Subscription’s billing product allows this feature
            Throws exception if not allowed.
            Method execution stops here if access is denied
         */
        featureAccessService.assertFeatureAccess(
                user.getUser().getId(),
                FeatureCodes.valueOf(requireFeature.code())
        );

        return proceedingJoinPoint.proceed();
    }
}

