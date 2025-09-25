package com.dodam.plan.webhook;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 운영에선 Redis/DB로 바꾸기 권장 */
@Component
public class PlanInMemoryEventStore {
    private final Map<String, Instant> seen = new ConcurrentHashMap<>();
    public boolean isDuplicate(String id) { return id != null && !id.isBlank() && seen.containsKey(id); }
    public void markHandled(String id) { if (id != null && !id.isBlank()) seen.put(id, Instant.now()); }
}
