package com.ayushsingh.doc_helper.config.ai;

import reactor.core.publisher.Flux;

import reactor.core.publisher.Mono;

public interface ChatCancellationRegistry {

    /**
     * Returns a Flux that completes when a cancel signal is received for this
     * generationId.
     */
    Flux<Void> getOrCreateCancelFlux(String generationId);

    /**
     * Sends a cancel signal for the given generationId.
     */
    Mono<Void> cancel(String generationId);
}
