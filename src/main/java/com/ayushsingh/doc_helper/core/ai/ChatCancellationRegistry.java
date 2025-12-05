package com.ayushsingh.doc_helper.core.ai;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatCancellationRegistry {

    private final ConcurrentHashMap<String, Sinks.Many<Void>> sinks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> manuallyCancelled = new ConcurrentHashMap<>();

    public Flux<Void> getOrCreateCancelFlux(String generationId) {
        return sinks
                .computeIfAbsent(generationId,
                        id -> Sinks.many().unicast().onBackpressureBuffer())
                .asFlux();
    }

    public void cancel(String generationId) {
        // mark as user-cancelled
        manuallyCancelled.put(generationId, true);

        Sinks.Many<Void> sink = sinks.remove(generationId);
        if (sink != null) {
            sink.tryEmitComplete(); // triggers takeUntilOther termination
        }
    }

    public boolean isManuallyCancelled(String generationId) {
        return manuallyCancelled.getOrDefault(generationId, false);
    }

    public void clear(String generationId) {
        sinks.remove(generationId);
        manuallyCancelled.remove(generationId);
    }
}
