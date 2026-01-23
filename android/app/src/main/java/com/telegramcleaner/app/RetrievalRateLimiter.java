package com.telegramcleaner.app;

import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Optimal rate limiter for Telegram message retrieval
 * Implements adaptive pacing for fetching messages
 */
public class RetrievalRateLimiter {

    private static final String TAG = "RetrievalRateLimiter";

    // Optimal page size (Telegram tolerates up to 100)
    public static final int OPTIMAL_PAGE_SIZE = 100;

    // Delay settings (milliseconds)
    private static final int INITIAL_DELAY_MIN = 200;
    private static final int INITIAL_DELAY_MAX = 300;
    private static final int MIN_DELAY = 100;
    private static final int MAX_DELAY = 1000;
    private static final int DELAY_REDUCTION_STEP = 25;
    private static final int DELAY_INCREASE_STEP = 100;

    // Adaptive delay tracking
    private final AtomicInteger currentDelay;
    private final AtomicLong lastRequestTime;
    private int successiveSuccesses;
    private final Object lock;

    public RetrievalRateLimiter() {
        this.currentDelay = new AtomicInteger((INITIAL_DELAY_MIN + INITIAL_DELAY_MAX) / 2);
        this.lastRequestTime = new AtomicLong(0);
        this.successiveSuccesses = 0;
        this.lock = new Object();
    }

    /**
     * Wait before next retrieval request
     */
    public void waitBeforeRequest() {
        synchronized (lock) {
            int delay = currentDelay.get();

            // Ensure minimum spacing between requests
            long now = System.currentTimeMillis();
            long elapsed = now - lastRequestTime.get();

            if (elapsed < delay) {
                int actualWait = delay - (int) elapsed;
                try {
                    Log.d(TAG, "Waiting " + actualWait + "ms before next retrieval");
                    Thread.sleep(actualWait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            lastRequestTime.set(System.currentTimeMillis());
        }
    }

    /**
     * Called after successful retrieval
     */
    public void onSuccess() {
        synchronized (lock) {
            successiveSuccesses++;

            // Gradually reduce delay after multiple successes
            if (successiveSuccesses >= 10) {
                int newDelay = Math.max(MIN_DELAY, currentDelay.get() - DELAY_REDUCTION_STEP);
                currentDelay.set(newDelay);
                successiveSuccesses = 0;
                Log.d(TAG, "Reduced retrieval delay to " + newDelay + "ms");
            }
        }
    }

    /**
     * Called when FLOOD_WAIT_X error occurs
     */
    public void onFloodWait(int waitSeconds) {
        synchronized (lock) {
            successiveSuccesses = 0;

            // Increase delay significantly
            int newDelay = Math.min(MAX_DELAY, currentDelay.get() + DELAY_INCREASE_STEP * 2);
            currentDelay.set(newDelay);

            Log.d(TAG, "FloodWait on retrieval. Increased delay to " + newDelay + "ms");

            // Wait the required time + jitter
            int totalWait = (waitSeconds * 1000) + 2000;

            try {
                Log.d(TAG, "Sleeping for " + totalWait + "ms due to FloodWait");
                Thread.sleep(totalWait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            lastRequestTime.set(System.currentTimeMillis());
        }
    }

    /**
     * Called on error
     */
    public void onError() {
        synchronized (lock) {
            successiveSuccesses = 0;

            int newDelay = Math.min(MAX_DELAY, currentDelay.get() + DELAY_INCREASE_STEP);
            currentDelay.set(newDelay);

            Log.d(TAG, "Error on retrieval. Increased delay to " + newDelay + "ms");
        }
    }

    /**
     * Get current delay
     */
    public int getCurrentDelay() {
        return currentDelay.get();
    }

    /**
     * Get optimal page size for retrieval
     */
    public int getOptimalPageSize() {
        return OPTIMAL_PAGE_SIZE;
    }
}
