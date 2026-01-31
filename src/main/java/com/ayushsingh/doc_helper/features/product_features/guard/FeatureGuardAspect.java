package com.ayushsingh.doc_helper.features.product_features.guard;

import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.auth.entity.AuthUser;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureAccessService;
import com.ayushsingh.doc_helper.features.product_features.service.UsageQuotaService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class FeatureGuardAspect {

    private final FeatureAccessService featureAccessService;
    private final UsageQuotaService usageQuotaService;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(requireFeature)")
    public Object guard(
            ProceedingJoinPoint pjp,
            RequireFeature requireFeature
    ) throws Throwable {

        AuthUser user = UserContext.getCurrentUser();

        featureAccessService.assertFeatureAccess(
                user.getUser().getId(),
                requireFeature.code()
        );

        long amount = evaluateAmount(requireFeature.amount(), pjp);

        usageQuotaService.consume(
                user.getUser().getId(),
                requireFeature.code(),
                requireFeature.metric(),
                amount
        );

        return pjp.proceed();
    }

    private long evaluateAmount(String expression, ProceedingJoinPoint pjp) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        Object[] args = pjp.getArgs();
        String[] names = ((MethodSignature) pjp.getSignature()).getParameterNames();

        for (int i = 0; i < names.length; i++) {
            ctx.setVariable(names[i], args[i]);
        }

        return parser.parseExpression(expression).getValue(ctx, Long.class);
    }
}

