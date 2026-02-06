package com.ayushsingh.doc_helper.features.product_features.guard;

import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureAccessService;
import com.ayushsingh.doc_helper.features.product_features.service.UsageQuotaService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class FeatureGuardAspect {

    private final FeatureAccessService featureAccessService;
    private final UsageQuotaService usageQuotaService;
    private final SpelExpressionParser parser = new SpelExpressionParser();

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
                requireFeature.code()
        );

        // Different features consume usage differently
        long amount = evaluateAmount(requireFeature.amount(), proceedingJoinPoint);

        // Quota is enforced before business logic executes
        usageQuotaService.consume(
                user.getUser().getId(),
                requireFeature.code(),
                requireFeature.metric(),
                amount
        );

        return proceedingJoinPoint.proceed();
    }

    private long evaluateAmount(String expression, ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        MethodBasedEvaluationContext ctx = new MethodBasedEvaluationContext(
                pjp.getTarget(),
                signature.getMethod(),
                pjp.getArgs(),
                new DefaultParameterNameDiscoverer()
        );

        // Parses the expression string
        Long value = parser.parseExpression(expression).getValue(ctx, Long.class);

        if (value == null) {
            throw new IllegalStateException(
                    "SpEL expression '" + expression +
                            "' evaluated to null. " +
                            "Check @RequireFeature.amount configuration."
            );
        }

        if (value < 0) {
            throw new IllegalArgumentException(
                    "Usage amount cannot be negative: " + value
            );
        }

        return value;
    }
}

