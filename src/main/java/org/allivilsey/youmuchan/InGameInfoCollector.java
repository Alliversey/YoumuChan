package org.allivilsey.youmuchan;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class InGameInfoCollector {

    private final Deque<InGameInfo> infoBuffer = new ConcurrentLinkedDeque<>();

    private final long maxCacheDurationMillis;

    public InGameInfoCollector(long maxCacheDurationMillis) {
        this.maxCacheDurationMillis = maxCacheDurationMillis;
    }

    //添加新数据
    public void addInfo(InGameInfo info) {
        if (info == null) {
            return;
        }
        infoBuffer.addLast(info);
        clearOldInfo();
    }

    //清除旧消息
    private void clearOldInfo() {

        long now = System.currentTimeMillis();

        while (!infoBuffer.isEmpty()) {

            InGameInfo oldest = infoBuffer.peekFirst();

            if (oldest == null) {
                break;
            }

            if (now - oldest.getTimestamp() > maxCacheDurationMillis) {
                infoBuffer.pollFirst();
            } else {
                break;
            }
        }
    }

    //按时间获取信息
    public List<InGameInfo> getInfoByTime(long durationMillis) {
        long now = System.currentTimeMillis();
        List<InGameInfo> result = new ArrayList<>();

        for (InGameInfo msg : infoBuffer) {
            if (now - msg.getTimestamp() <= durationMillis) {
                result.add(msg);
            }
        }

        return result;
    }

    //按玩家获取信息
    public List<InGameInfo> getInfoByPlayer(String playerName, long durationMillis) {

        if (playerName == null) {
            return new ArrayList<>();
        }

        long now = System.currentTimeMillis();
        List<InGameInfo> result = new ArrayList<>();

        for (InGameInfo info : infoBuffer) {
            String infoPlayer = info.getPlayerName();
            if (infoPlayer != null && infoPlayer.equalsIgnoreCase(playerName ) && now - info.getTimestamp() <= durationMillis) {
                result.add(info);
            }
        }

        return result;
    }

    //按服务器获取信息
    public List<InGameInfo> getInfoByServer(String serverName, long durationMillis) {
        long now = System.currentTimeMillis();
        List<InGameInfo> result = new ArrayList<>();

        for (InGameInfo message : infoBuffer) {
            if (message.getServerName().equalsIgnoreCase(serverName) && now - message.getTimestamp() <= durationMillis) {
                result.add(message);
            }
        }

        return result;
    }

    //获取缓存量
    public int getMessageListSize() {
        return infoBuffer.size();
    }

}
