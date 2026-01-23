package com.telegramcleaner.app;

import android.util.Log;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optimal rate limiter for Telegram deletions
 * Implements adaptive delay and batch control strategies
 */
public class DeletionRateLimiter {

    private static final String TAG = "DeletionRateLimiter";

    // Batch size limits
    private static final int BATCH_SIZE_PRIVATE_MIN = 20;
    private static final int BATCH_SIZE_PRIVATE_MAX = 50;
    private static final int BATCH_SIZE_GROUP_MIN = 10;
    private static final int BATCH_SIZE_GROUP_MAX = 30;

    // Delay settings (milliseconds)
    private static final int INITIAL_DELAY_MIN = 500;
    private static final int INITIAL_DELAY_MAX = 800;
    private static final int MIN_DELAY = 100;
    private static final int MAX_DELAY = 5000;
    private static final int DELAY_REDUCTION_STEP = 50;
    private static final double DELAY_INCREASE_FACTOR = 1.4; // 40% increase on flood

    // Global queue for single-threaded deletions
    private final BlockingQueue<DeletionTask> deletionQueue;
    private final AtomicBoolean isProcessing;
    private final Random random;

    // Adaptive delay tracking
    private AtomicInteger currentDelay;
    private int successiveSuccesses;
    private long lastRequestTime;

    public DeletionRateLimiter() {
        this.deletionQueue = new LinkedBlockingQueue<>();
        this.isProcessing = new AtomicBoolean(false);
        this.random = new Random();
        this.currentDelay = new AtomicInteger(randomBetween(INITIAL_DELAY_MIN, INITIAL_DELAY_MAX));
        this.successiveSuccesses = 0;
        this.lastRequestTime = 0;
    }

    /**
     * Get optimal batch size for chat type
     */
    public int getOptimalBatchSize(String chatType, long messageAgeHours) {
        int baseMin, baseMax;

        // Different limits for private vs group chats
        if ("private".equals(chatType)) {
            baseMin = BATCH_SIZE_PRIVATE_MIN;
            baseMax = BATCH_SIZE_PRIVATE_MAX;
        } else {
            baseMin = BATCH_SIZE_GROUP_MIN;
            baseMax = BATCH_SIZE_GROUP_MAX;
        }

        // Reduce batch size for old messages
        if (messageAgeHours > 24 * 7) { // Older than 1 week
            baseMin = Math.max(5, baseMin / 2);
            baseMax = Math.max(10, baseMax / 2);
        }

        return randomBetween(baseMin, baseMax);
    }

    /**
     * Get current adaptive delay
     */
    public int getCurrentDelay() {
        return currentDelay.get();
    }

    /**
     * Wait with current adaptive delay
     */
    public void waitBeforeRequest() {
        int delay = currentDelay.get();

        // Ensure minimum spacing between requests
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTime;

        if (elapsed < delay) {
            int actualWait = delay - (int) elapsed;
            try {
                Log.d(TAG, "Waiting " + actualWait + "ms before next request");
                Thread.sleep(actualWait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastRequestTime = System.currentTimeMillis();
    }

    /**
     * Called after successful deletion
     */
    public void onSuccess() {
        successiveSuccesses++;

        // Gradually reduce delay after multiple successes
        if (successiveSuccesses >= 3) {
            int newDelay = Math.max(MIN_DELAY, currentDelay.get() - DELAY_REDUCTION_STEP);
            currentDelay.set(newDelay);
            successiveSuccesses = 0;
            Log.d(TAG, "Reduced delay to " + newDelay + "ms after successive successes");
        }
    }

    /**
     * Called when FLOOD_WAIT_X error occurs
     */
    public void onFloodWait(int waitSeconds) {
        successiveSuccesses = 0;

        // Increase base delay
        int newDelay = (int) Math.min(MAX_DELAY, currentDelay.get() * DELAY_INCREASE_FACTOR);
        currentDelay.set(newDelay);

        Log.d(TAG, "FloodWait detected. Increased delay to " + newDelay + "ms");

        // Wait the required time + random jitter
        int totalWait = (waitSeconds * 1000) + randomBetween(2000, 5000);

        try {
            Log.d(TAG, "Sleeping for " + totalWait + "ms due to FloodWait");
            Thread.sleep(totalWait);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        lastRequestTime = System.currentTimeMillis();
    }

    /**
     * Called on any error (not flood)
     */
    public void onError() {
        successiveSuccesses = 0;

        // Small delay increase
        int newDelay = Math.min(MAX_DELAY, currentDelay.get() + DELAY_REDUCTION_STEP);
        currentDelay.set(newDelay);

        Log.d(TAG, "Error detected. Increased delay to " + newDelay + "ms");
    }

    /**
     * Add deletion task to global queue
     */
    public void queueDeletion(DeletionTask task) {
        deletionQueue.offer(task);
        processQueue();
    }

    /**
     * Process deletion queue (single-threaded)
     */
    private void processQueue() {
        if (!isProcessing.compareAndSet(false, true)) {
            return; // Already processing
        }

        new Thread(() -> {
            try {
                while (!deletionQueue.isEmpty()) {
                    DeletionTask task = deletionQueue.poll();
                    if (task != null) {
                        waitBeforeRequest();
                        task.execute();
                    }
                }
            } finally {
                isProcessing.set(false);

                // Check if new tasks arrived while we were finishing
                if (!deletionQueue.isEmpty()) {
                    processQueue();
                }
            }
        }, "DeletionQueue").start();
    }

    /**
     * Get random value between min and max (inclusive)
     */
    private int randomBetween(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    /**
     * Task interface for queued deletions
     */
    public interface DeletionTask {
        void execute();
    }
}
