package com.spite.backend.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoginRateLimitService {

    private final ConcurrentHashMap<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    @Value("${auth.login-rate.max-attempts:8}")
    private int maxAttempts;

    @Value("${auth.login-rate.window-ms:900000}")
    private long windowMs;

    public boolean isBlocked(String key) {
        Deque<Long> queue = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (queue) {
            trimOld(queue);
            return queue.size() >= maxAttempts;
        }
    }

    public void registerFailure(String key) {
        Deque<Long> queue = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (queue) {
            trimOld(queue);
            queue.addLast(System.currentTimeMillis());
        }
    }

    public void registerSuccess(String key) {
        attempts.remove(key);
    }

    private void trimOld(Deque<Long> queue) {
        long minAllowed = System.currentTimeMillis() - windowMs;
        while (!queue.isEmpty() && queue.peekFirst() < minAllowed) {
            queue.removeFirst();
        }
    }
}