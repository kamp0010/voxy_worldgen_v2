package com.ethan.voxyworldgenv2.stats;

import java.util.concurrent.atomic.AtomicLong;

public class GenerationStats {
    private final AtomicLong chunksQueued = new AtomicLong(0);
    private final AtomicLong chunksCompleted = new AtomicLong(0);
    private final AtomicLong chunksFailed = new AtomicLong(0);
    private final AtomicLong chunksSkipped = new AtomicLong(0);

    // rolling average over 10s
    private final long[] rollingHistory = new long[10];
    private int historyIndex = 0;
    private long lastCompletedCount = 0;
    private long lastTickTime = 0; // 0 is the sentinel meaning "not yet initialized"

    public void incrementQueued() { chunksQueued.incrementAndGet(); }
    public void incrementCompleted() { chunksCompleted.incrementAndGet(); }
    public void incrementFailed() { chunksFailed.incrementAndGet(); }
    public void incrementSkipped() { chunksSkipped.incrementAndGet(); }

    public long getQueued() { return chunksQueued.get(); }
    public long getCompleted() { return chunksCompleted.get(); }
    public long getFailed() { return chunksFailed.get(); }
    public long getSkipped() { return chunksSkipped.get(); }

    // update rolling average; call every tick
    public synchronized void tick() {
        long now = System.currentTimeMillis();

        // first call: initialize the baseline and return without touching history
        if (lastTickTime == 0) {
            lastTickTime = now;
            lastCompletedCount = chunksCompleted.get() + chunksSkipped.get();
            return;
        }

        long secondsPassed = (now - lastTickTime) / 1000;
        if (secondsPassed < 1) return;

        long currentTotal = chunksCompleted.get() + chunksSkipped.get();
        long delta = currentTotal - lastCompletedCount;

        // limit updates to history length to avoid redundant overwrites
        int updateCount = (int) Math.min(secondsPassed, rollingHistory.length);

        // divide by updateCount (not secondsPassed) so remainder is bounded by the
        // number of slots actually being written — prevents under-allocation when
        // secondsPassed > rollingHistory.length
        long perSlot = delta / updateCount;
        long remainder = delta % updateCount;

        for (int i = 0; i < updateCount; i++) {
            // distribute any remainder across the earliest slots
            long val = perSlot + (i < remainder ? 1 : 0);
            rollingHistory[historyIndex] = val;
            historyIndex = (historyIndex + 1) % rollingHistory.length;
        }

        lastCompletedCount = currentTotal;
        lastTickTime += secondsPassed * 1000;
    }

    public synchronized double getChunksPerSecond() {
        long sum = 0;
        for (long val : rollingHistory) {
            sum += val;
        }
        return sum / 10.0;
    }

    public void reset() {
        chunksQueued.set(0);
        chunksCompleted.set(0);
        chunksFailed.set(0);
        chunksSkipped.set(0);
        synchronized (this) {
            for (int i = 0; i < rollingHistory.length; i++) rollingHistory[i] = 0;
            historyIndex = 0;
            lastCompletedCount = 0;
            lastTickTime = 0; // reset to sentinel so next tick() re-initializes cleanly
        }
    }
}
