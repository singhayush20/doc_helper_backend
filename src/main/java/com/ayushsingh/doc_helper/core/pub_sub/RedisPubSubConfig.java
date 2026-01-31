package com.ayushsingh.doc_helper.core.pub_sub;

import com.ayushsingh.doc_helper.features.product_features.cache.FeatureInvalidationSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisPubSubConfig {

    @Bean
    RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            FeatureInvalidationSubscriber subscriber
    ) {
        RedisMessageListenerContainer container =
                new RedisMessageListenerContainer();

        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                subscriber,
                new ChannelTopic(RedisChannels.FEATURE_INVALIDATION)
        );
        return container;
    }
}
