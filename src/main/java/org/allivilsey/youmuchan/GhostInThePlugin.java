package org.allivilsey.youmuchan;

import com.velocitypowered.api.proxy.ProxyServer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GhostInThePlugin {
    private final ProxyServer server;
    private final Object plugin;

    private final KaianPassageway passageway;
    private final MentalStateController mentalStateController;
    private final FocusController focusController;
    private final HeatController heatController;
    private final MessageSender messageSender;

    private final long baseInterval;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean aiBusy = new AtomicBoolean(false);

    private long nextPulseTime = 0L;

    public GhostInThePlugin(
            ProxyServer server,
            Object plugin,
            KaianPassageway passageway,
            MentalStateController mentalStateController,
            FocusController focusController,
            HeatController heatController,
            MessageSender messageSender,
            long baseInterval) {
        this.server = server;
        this.plugin = plugin;
        this.passageway = passageway;
        this.mentalStateController = mentalStateController;
        this.focusController = focusController;
        this.heatController = heatController;
        this.messageSender = messageSender;
        this.baseInterval = baseInterval;
    }

    // 启动 AI 调度循环；若已在运行则直接返回，避免重复注册任务链。
    public void youmuStart() {
        if (!running.compareAndSet(false, true)) return;
        nextPulseTime = System.currentTimeMillis();
        scheduleNextTick();
    }

    private void scheduleNextTick() {
        server.getScheduler().buildTask(plugin, this::tick).delay(1, TimeUnit.SECONDS).schedule();
    }

    private void tick() {

        if (!running.get()) return;

        long now = System.currentTimeMillis();

        if (now >= nextPulseTime && aiBusy.compareAndSet(false, true)) {

            mentalStateController.evaluate();
            MentalState state = mentalStateController.getCurrentMentalState();

            // 在 SLEEP 状态下跳过本轮推理，仅推进下一次触发时间。
            if (state == MentalState.SLEEP) {
                aiBusy.set(false);
                nextPulseTime = now + baseInterval;
                scheduleNextTick();
                return;
            }

            nextPulseTime = now + computeInterval();

            triggerAICycle(state);
        }

        scheduleNextTick();
    }

    private long computeInterval() {
        double heat = heatController.getHeat();
        return (long) (baseInterval * heat);
    }

    private void triggerAICycle(MentalState state) {

        server.getScheduler().buildTask(plugin, () -> {

            try {
                String target = focusController.getCurrentFocus();
                String reply = passageway.pass(target, state);
                messageSender.send(reply);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                aiBusy.set(false);
            }
        }).schedule();
    }
}
