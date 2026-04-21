package com.mwangahakika.backend.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class LoginRateLimitService {

    private final RateLimitProperties properties;
    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public LoginRateLimitService(RateLimitProperties properties) {
        this.properties = properties;
    }

    public boolean isAllowed(String key) {
        Instant now = Instant.now();
        AttemptWindow window = attempts.compute(key, (ignored, current) -> updateWindow(current, now));
        return window.count() <= properties.maxAttempts();
    }

    public void reset() {
        attempts.clear();
    }

    private AttemptWindow updateWindow(AttemptWindow current, Instant now) {
        if (current == null || Duration.between(current.startedAt(), now).getSeconds() >= properties.windowSeconds()) {
            return new AttemptWindow(1, now);
        }
        return new AttemptWindow(current.count() + 1, current.startedAt());
    }

    private record AttemptWindow(int count, Instant startedAt) {
    }
}
