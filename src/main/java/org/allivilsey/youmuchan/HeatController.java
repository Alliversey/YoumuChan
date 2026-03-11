package org.allivilsey.youmuchan;

// 维护一个随时间衰减的 fuel 值，并映射为调度倍率
public class HeatController {

    // 原始 fuel 值：由事件监听器注入，随时间指数衰减
    private double fuel = 0.0;

    // 记录近期触发事件的玩家，用于计算反比增益
    private final java.util.List<PlayerRecord> recentPlayers = new java.util.ArrayList<>();

    // 玩家记录的缓存时长（毫秒）
    private final long cacheDurationMs;

    // 上次衰减计算时间戳（毫秒）
    private long lastUpdateTime;

    // 指数衰减常数，由半衰期计算得出
    private final double lambda;

    // halfLifeSeconds 越小，热度下降越快
    public HeatController(double halfLifeSeconds, long cacheDurationMs) {
        this.lambda = Math.log(2) / halfLifeSeconds;
        this.cacheDurationMs = cacheDurationMs;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    // 增加 fuel 前先做一次衰减，保证不同事件在统一时间基准下叠加
    public synchronized void addFuel(double f, String player, double timestamp) {
        decay();
        recordPlayer(player, timestamp);
        // 根据活跃玩家数量修改 fuel 添加倍率
        int playerCount = countActivePlayers();
        double factor = playerCount > 0 ? 1.0 / playerCount : 1.0;
        fuel = fuel + f * factor;
    }

    // 返回当前 heat 倍率
    // 通过 mapHeat 将燃料压缩到平滑区间，避免调度间隔剧烈跳变
    public synchronized double getHeat() {
        decay();
        return mapHeat();
    }

    public synchronized double getFuel() {
        decay();
        return fuel;
    }

    // 对 fuel 执行指数衰减
    private void decay() {
        long now = System.currentTimeMillis();
        double dt = (now - lastUpdateTime) / 1000.0;

        if (dt > 0) {
            fuel = fuel * Math.exp(-lambda * dt);
            lastUpdateTime = now;
        }
    }

    // 将 fuel 映射到 (0, 2] 区间，1.0 附近作为常规工作区间
    private double mapHeat() {
        return Math.pow(2.0 / (1.0 + Math.exp(-0.5 * (fuel - 6.0))), 1.2);
    }

    // 记录玩家触发事件，并清理超时记录
    private void recordPlayer(String player, double timestamp) {
        recentPlayers.add(new PlayerRecord(player, timestamp));
        purgeExpired(timestamp);
    }

    // 根据缓存时长清理过久的玩家记录
    private void purgeExpired(double nowTimestamp) {
        double cutoff = nowTimestamp - cacheDurationMs;
        recentPlayers.removeIf(record -> record.timestamp() < cutoff);
    }

    // 统计当前缓存窗口内的玩家数量（去重）
    private int countActivePlayers() {
        java.util.Set<String> players = new java.util.HashSet<>();
        for (PlayerRecord record : recentPlayers) {
            players.add(record.player());
        }
        return players.size();
    }

    // 保存玩家与触发时间
    private record PlayerRecord(String player, double timestamp) {
    }
}
