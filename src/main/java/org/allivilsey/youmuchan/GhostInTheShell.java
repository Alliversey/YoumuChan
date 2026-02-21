package org.allivilsey.youmuchan;

import com.velocitypowered.api.proxy.ProxyServer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GhostInTheShell {
    private final ProxyServer server;
    private final Object plugin;

    private final long standardSpeechCooldownMillis;

    private volatile double heatCoefficient = 1.0;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final AtomicBoolean aiBusy = new AtomicBoolean(false);

    private long nextPulseTime = 0L;

    private final Runnable aiCycle;

    public GhostInTheShell(ProxyServer server, Object plugin, long standardSpeechCooldownMillis) {
        this.server = server;
        this.plugin = plugin;
        this.standardSpeechCooldownMillis = standardSpeechCooldownMillis;
        this.aiCycle = aiCycle;
    }

    public void startYoumu() {
        if (!running.compareAndSet(false, true)) return;
        nextPulseTime = System.currentTimeMillis();

        scheduleNextTick();
    }

    public void stopYoumu() {
        running.set(false);
    }

    public void setPulseCoefficient(double k) {
        this.heatCoefficient = k;
    }

    private void scheduleNextTick() {
        server.getScheduler().buildTask(plugin, this::tick).delay(1, TimeUnit.SECONDS).schedule();
    }

    private void tick() {
        if (!running.get()) return;

        long now = System.currentTimeMillis();

        if (now >= nextPulseTime && aiBusy.compareAndSet(false, true)) {
            long interval = computeInterval();
            nextPulseTime = now + interval;


        }
    }

    private long computeInterval() {
        return (long) (standardSpeechCooldownMillis *heatCoefficient);
    }

    private void triggerAICycle() {
        server.getScheduler().buildTask(plugin, () -> {
            try {
                aiCycle.run();
            } finally {
                aiBusy.set(false);
            }
        }).schedule();
    }
}
