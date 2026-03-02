package org.allivilsey.youmuchan;

// 维护一个随时间衰减的“热度燃料”值，并映射为调度倍率。
public class HeatController {

    // 原始燃料值：由事件监听器注入，随时间指数衰减。
    private double fuel = 0.0;

    // 上次衰减计算时间戳（毫秒）。
    private long lastUpdateTime;

    // 指数衰减常数，由半衰期计算得出。
    private final double lambda;

    // halfLifeSeconds 越小，热度下降越快。
    public HeatController(double halfLifeSeconds) {
        this.lambda = Math.log(2) / halfLifeSeconds;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    // 增加燃料前先做一次衰减，保证不同事件在统一时间基准下叠加。
    public synchronized void addFuel(double f) {
        decay();
        fuel = fuel + f;
    }

    // 返回当前热度倍率。
    // 通过 mapHeat 将燃料压缩到平滑区间，避免调度间隔剧烈跳变。
    public synchronized double getHeat() {
        decay();
        return mapHeat();
    }

    public synchronized double getFuel() {
        decay();
        return fuel;
    }

    // 对燃料执行指数衰减
    private void decay() {
        long now = System.currentTimeMillis();
        double dt = (now - lastUpdateTime) / 1000.0;

        if (dt > 0) {
            fuel = fuel * Math.exp(-lambda * dt);
            lastUpdateTime = now;
        }
    }

    // 将燃料映射到 (0, 2] 区间，1.0 附近作为常规工作区间。
    private double mapHeat() {
        return Math.pow(2.0 / (1.0 + Math.exp(-0.5 * (fuel - 6.0))), 1.2);
    }
}
