package server.security;

/**
 * Tracks login attempts for brute force protection
 */
public class AttemptTracker {
    private int attempts = 0;
    private long lastAttemptTime = 0;
    private long lockoutUntil = 0;

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION = 15 * 60 * 1000; // 15 minutes
    private static final long RESET_TIME = 30 * 60 * 1000; // 30 minutes

    /**
     * Record a failed login attempt
     */
    public void recordFailedAttempt() {
        long now = System.currentTimeMillis();

        // Reset counter if last attempt was too long ago
        if (now - lastAttemptTime > RESET_TIME) {
            attempts = 0;
        }

        attempts++;
        lastAttemptTime = now;

        // Lock account if too many attempts
        if (attempts >= MAX_ATTEMPTS) {
            lockoutUntil = now + LOCKOUT_DURATION;
        }
    }

    /**
     * Record successful login (resets attempts)
     */
    public void recordSuccessfulLogin() {
        attempts = 0;
        lockoutUntil = 0;
        lastAttemptTime = 0;
    }

    /**
     * Check if account is currently locked
     */
    public boolean isLocked() {
        long now = System.currentTimeMillis();

        // Check if lockout has expired
        if (lockoutUntil > 0 && now > lockoutUntil) {
            lockoutUntil = 0;
            attempts = 0;
            return false;
        }

        return lockoutUntil > now;
    }

    /**
     * Get remaining lockout time in seconds
     */
    public long getRemainingLockoutSeconds() {
        if (!isLocked()) return 0;
        return (lockoutUntil - System.currentTimeMillis()) / 1000;
    }

    /**
     * Get current attempt count
     */
    public int getAttemptCount() {
        return attempts;
    }

    /**
     * Get attempts remaining before lockout
     */
    public int getAttemptsRemaining() {
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }
}