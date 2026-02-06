package com.ayushsingh.doc_helper.features.ui_components.registry;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.product_features.entity.UIComponentType;
import com.ayushsingh.doc_helper.features.ui_components.models.FeatureCard;
import com.ayushsingh.doc_helper.features.ui_components.models.UIComponent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UIComponentRegistry {

        // using HashMap since the map is constructed only once at the start-up
        // if this is changed to support dynamic registration, consider using ConcurrentHashMap
        private final Map<RegistryKey, Class<? extends UIComponent>> registry = new HashMap<>();

        public UIComponentRegistry() {
                register(UIComponentType.CARD, 1, FeatureCard.class);
        }

        public Class<? extends UIComponent> resolve(
                        UIComponentType type,
                        int version) {
                Class<? extends UIComponent> uiComponentClass = registry.get(new RegistryKey(type, version));

                if (uiComponentClass == null) {
                        throw new BaseException(
                                        "Unsupported UI component: " + type + " v" + version,
                                        ExceptionCodes.UNSUPPORTED_UI_COMPONENT);
                }
                return uiComponentClass;
        }

        private void register(
                        UIComponentType type,
                        int version,
                        Class<? extends UIComponent> uiComponentClass) {
                registry.put(new RegistryKey(type, version), uiComponentClass);
        }

        private record RegistryKey(UIComponentType type, int version) {
        }
}
