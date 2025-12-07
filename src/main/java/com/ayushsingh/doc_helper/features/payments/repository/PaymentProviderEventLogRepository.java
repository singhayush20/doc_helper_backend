package com.ayushsingh.doc_helper.features.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ayushsingh.doc_helper.features.payments.entity.PaymentProviderEventLog;
import java.util.Optional;

public interface PaymentProviderEventLogRepository extends JpaRepository<PaymentProviderEventLog, Long> {

    Optional<PaymentProviderEventLog> findByProviderEventId(String providerEventId);
}
