package com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class ActionProviderRegistry {

    private final Map<String, ActionProvider> providers;

    public ActionProviderRegistry(List<ActionProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(
                        p -> p.getClass().getAnnotation(Component.class).value(),
                        Function.identity()));
    }

    public ActionProvider get(String key) {
        ActionProvider provider = providers.get(key);

        if (provider == null) {
            throw new RuntimeException("No ActionProvider found for: " + key);
        }

        return provider;
    }
}