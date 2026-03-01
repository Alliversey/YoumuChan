package org.allivilsey.youmuchan;

import com.velocitypowered.api.proxy.ProxyServer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

// AI 调度器：按热度驱动周期执行，串行触发“构建上下文 -> 推理 -> 发送”流程。
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
        if (!running.compareAndSet(false, true))
            return;
        nextPulseTime = System.currentTimeMillis();
        scheduleNextTick();
    }

    // 停止 AI 调度循环；下一次 tick 检查到 running == false 后自然退出。
    public void youmuStop() {
        running.set(false);
    }

    private void scheduleNextTick() {
        // 固定 1s 心跳，仅用于轮询是否到达下一次推理时间。
        server.getScheduler().buildTask(plugin, this::tick).delay(1, TimeUnit.SECONDS).schedule();
    }

    private void tick() {

        if (!running.get())
            return;

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

            // 将推理放到独立任务中，避免阻塞轮询心跳。
            triggerAICycle(state);
        }

        scheduleNextTick();
    }

    private long computeInterval() {
        // 热度越高，倍率越大，触发间隔随之调整。
        double heat = heatController.getHeat();

        if (heat != 0.0) {
            return (long) (baseInterval / heat);
        }

        return baseInterval;
    }

    private void triggerAICycle(MentalState state) {

        server.getScheduler().buildTask(plugin, () -> {

            try {
                String target = focusController.getCurrentFocus();
                String reply = passageway.pass(target, state);
                messageSender.send(reply);
            } catch (Exception e) {
                // 当前实现仅记录堆栈，保持调度循环可继续运行。
                e.printStackTrace();
            } finally {
                // 无论成功失败都释放忙碌标记，避免后续周期永久阻塞。
                aiBusy.set(false);
            }
        }).schedule();
    }
}
