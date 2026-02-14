package org.allivilsey.youmuchan;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class InGameInfoCollector {

    private final Deque<InGameInfo> infoBuffer = new ConcurrentLinkedDeque<>();

    //记录时间限制
    private final long maxCacheDurationMillis;

    //记录条目数量限制
    private final int maxBufferSize;

    public InGameInfoCollector(long maxCacheDurationMillis, int maxBufferSize) {
        this.maxCacheDurationMillis = maxCacheDurationMillis;
        this.maxBufferSize = maxBufferSize;
    }

    //添加新数据
    public void addInfo(InGameInfo info) {
        if (info == null) {
            return;
        }
        infoBuffer.addLast(info);
        clearOldInfo();
    }

    //清除过期数据
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

    //清除过多数据
    private void enforceSizeLimit() {
        while (infoBuffer.size() > maxBufferSize) {
            infoBuffer.pollFirst();
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
    public int getInfoListSize() {
        return infoBuffer.size();
    }

    //数据重置
    public void clearInfo() {
        infoBuffer.clear();
    }

}
