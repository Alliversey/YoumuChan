package org.allivilsey.youmuchan;

import com.velocitypowered.api.proxy.ProxyServer;

import java.time.LocalTime;

public class MentalStateController {

    private final ProxyServer server;

    // 控制器初始化时默认处于 SLEEP，避免插件刚启动就触发主动发言。
    private MentalState currentMentalState = MentalState.SLEEP;

    // 下一次允许状态切换的时间戳（毫秒）。
    private long nextStateChangeTime = 0L;
    // 默认状态切换冷却时长，用于抑制短时间内频繁抖动。
    private final long defaultCooldownMilliseconds = 60_000L;

    public MentalStateController(ProxyServer server) {
        this.server = server;
    }

    // 由外部调度器周期调用：按当前环境重新评估目标状态，并在满足冷却条件时执行切换。
    public void evaluate() {
        MentalState decideState = decideState();

        if (canSwitch(decideState)) {
            setMentalState(decideState, defaultCooldownMilliseconds);
        }
    }

    // 默认决策逻辑：依据北京时间与在线人数在 SLEEP 和 DREAM 间切换。
    private MentalState decideState() {

        // 获取北京时间的小时值，作为昼夜判定依据。
        int hour = LocalTime.now(java.time.ZoneId.of("Asia/Shanghai")).getHour();

        // 获取代理当前在线人数。
        int onlinePlayers = server.getPlayerCount();

        // 在线人数为 0 或时间早于 10:00 时进入休眠，以减少空场景下的无效调用。
        if (onlinePlayers == 0 || hour < 8) {
            return MentalState.SLEEP;
        }

        // 其余场景进入对话模式，允许后续 AI 周期生成回复。
        return MentalState.DREAM;
    }

    // 设置当前状态并刷新下一次切换时间。
    // duration == -1 表示无限期锁定当前状态（直到外部再次显式设置）。
    public void setMentalState(MentalState mentalState, long duration) {
        currentMentalState = mentalState;

        if (duration == -1) {
            nextStateChangeTime = Long.MAX_VALUE;
        } else {
            long now = System.currentTimeMillis();
            nextStateChangeTime = now + duration;
        }
    }

    // 返回当前心智状态，供调度器与业务流程判断是否执行对话链路。
    public MentalState getCurrentMentalState() {
        return currentMentalState;
    }

    // 仅当目标状态与当前状态不同且已超过冷却窗口时，才允许状态切换。
    private boolean canSwitch(MentalState mentalState) {
        if (currentMentalState == mentalState) {
            return false;
        }

        long now = System.currentTimeMillis();
        return now >= nextStateChangeTime;
    }
}
