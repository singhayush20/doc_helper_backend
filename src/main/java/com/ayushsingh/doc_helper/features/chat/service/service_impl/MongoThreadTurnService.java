package com.ayushsingh.doc_helper.features.chat.service.service_impl;

import com.ayushsingh.doc_helper.features.chat.entity.ChatThread;
import com.ayushsingh.doc_helper.features.chat.entity.TurnReservation;
import com.ayushsingh.doc_helper.features.chat.service.ThreadTurnService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MongoThreadTurnService implements ThreadTurnService {

    private final MongoTemplate mongoTemplate;

    @Override
    public TurnReservation reserveTurn(String threadId) {
        Query query = new Query(Criteria.where("_id").is(threadId));

        Update update = new Update().inc("lastTurnNumber", 1L);

        FindAndModifyOptions options = FindAndModifyOptions.options()
                .returnNew(true)
                .upsert(false);

        ChatThread updated = mongoTemplate.findAndModify(
                query,
                update,
                options,
                ChatThread.class);

        if (updated == null) {
            throw new IllegalStateException("Thread not found: " + threadId);
        }

        Long allocatedTurn = updated.getLastTurnNumber();
        return new TurnReservation(threadId, allocatedTurn);
    }
}
