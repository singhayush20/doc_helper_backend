package com.ayushsingh.doc_helper.config.ai;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class RedisChatCancellationRegistry implements ChatCancellationRegistry {

    private static final String CHANNEL_PREFIX = "chat:cancel:";

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    public RedisChatCancellationRegistry(ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    private String channelName(String generationId) {
        return CHANNEL_PREFIX + generationId;
    }

    @Override
    public Flux<Void> getOrCreateCancelFlux(String generationId) {
        String channel = channelName(generationId);
        ChannelTopic topic = new ChannelTopic(channel);

        // Listen to Redis pub/sub; complete on first cancel message
        return reactiveRedisTemplate
                .listenTo(topic) 
                .next() // only first cancel event
                .map(msg -> (Void) null)
                .flux();
    }

    @Override
    public Mono<Void> cancel(String generationId) {
        String channel = channelName(generationId);
        return reactiveRedisTemplate
                .convertAndSend(channel, "CANCEL")
                .then();
    }
}
