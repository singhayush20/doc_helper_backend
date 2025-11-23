package com.ayushsingh.doc_helper.features.chat.service;

import com.ayushsingh.doc_helper.features.chat.entity.TurnReservation;

public interface ThreadTurnService {

    TurnReservation reserveTurn(String threadId);
}
