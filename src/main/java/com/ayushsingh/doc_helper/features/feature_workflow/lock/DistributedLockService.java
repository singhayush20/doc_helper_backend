package com.ayushsingh.doc_helper.features.feature_workflow.lock;

public interface DistributedLockService {

    boolean acquireLock(String key, long timeoutMillis);

    void releaseLock(String key);
}
