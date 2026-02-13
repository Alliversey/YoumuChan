package org.allivilsey.youmuchan;

import com.velocitypowered.api.proxy.ProxyServer;

import java.time.LocalTime;

public class MentalStateController {

    private final ProxyServer server;

    //设置默认状态
    private MentalState currentMentalState = MentalState.SLEEP;

    //下次状态切换时间
    private long nextStateChangeTime = 0L;
    //状态切换冷却时间
    private final long defaultCooldownMilliseconds = 60_000L;

    public MentalStateController(ProxyServer server) {
        this.server = server;
    }

    //外部周期调用
    public void evaluate() {
        MentalState decideState = decideState();

        if (canSwitch(decideState)) {
            setMentalState(decideState, defaultCooldownMilliseconds);
        }
    }

    //默认决策逻辑
    private MentalState decideState() {

        //获取当前时间（小时）
        int hour = LocalTime.now(java.time.ZoneId.of("Asia/Shanghai")).getHour();

        //获取在线玩家数量
        int onlinePlayers = server.getPlayerCount();

        //无人or夜间休眠
        if (onlinePlayers == 0 || hour < 10) {
            return MentalState.SLEEP;
        }

        //默认休眠
        return MentalState.DREAM;
    }

    //切换状态
    public void setMentalState(MentalState mentalState, long duration) {
        currentMentalState = mentalState;

        if (duration == -1) {
            nextStateChangeTime = Long.MAX_VALUE;//duration参数为-1时锁定状态
        } else {
            long now = System.currentTimeMillis();
            nextStateChangeTime = now + duration;
        }
    }

    //查询状态
    public MentalState getCurrentMentalState() {
        return currentMentalState;
    }

    //检测状态切换冷却
    private boolean canSwitch(MentalState mentalState) {
        if (currentMentalState == mentalState) {
            return false;
        }

        long now = System.currentTimeMillis();
        return now >= nextStateChangeTime;
    }
}
