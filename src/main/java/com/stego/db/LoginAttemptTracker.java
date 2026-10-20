package com.stego.db;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** In-memory brute-force protection: lock a username after too many failed logins. */
public class LoginAttemptTracker {

    private static final class Entry {
        int failures;
        Instant lockedUntil;
    }

    private final int maxAttempts;
    private final Duration lockDuration;
    private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();

    public LoginAttemptTracker() {
        this(5, Duration.ofMinutes(15));
    }

    public LoginAttemptTracker(int maxAttempts, Duration lockDuration) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
    }

    public boolean isLocked(String username) {
        Entry e = entries.get(key(username));
        if (e == null) return false;
        synchronized (e) {
            if (e.lockedUntil == null) return false;
            if (Instant.now().isAfter(e.lockedUntil)) {   // lock expired
                entries.remove(key(username));
                return false;
            }
            return true;
        }
    }

    public long remainingLockMinutes(String username) {
        Entry e = entries.get(key(username));
        if (e == null || e.lockedUntil == null) return 0;
        synchronized (e) {
            long secs = Duration.between(Instant.now(), e.lockedUntil).getSeconds();
            return Math.max(1, (secs + 59) / 60);
        }
    }

    public void recordFailure(String username) {
        Entry e = entries.computeIfAbsent(key(username), k -> new Entry());
        synchronized (e) {
            e.failures++;
            if (e.failures >= maxAttempts) {
                e.lockedUntil = Instant.now().plus(lockDuration);
            }
        }
    }

    public void recordSuccess(String username) {
        entries.remove(key(username));
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
