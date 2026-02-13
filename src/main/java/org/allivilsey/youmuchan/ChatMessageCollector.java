package org.allivilsey.youmuchan;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ChatMessageCollector {

    private final Deque<ChatMessage> messageBuffer = new ConcurrentLinkedDeque<>();

    private final long maxCacheDurationMillis;

    public ChatMessageCollector(long maxCacheDurationMillis) {
        this.maxCacheDurationMillis = maxCacheDurationMillis;
    }

    //添加新消息
    public void addMessage(ChatMessage message) {
        messageBuffer.addLast(message);
        clearOldMessage();
    }

    //清除旧消息
    private void clearOldMessage() {

        while (!messageBuffer.isEmpty()) {
            long now = System.currentTimeMillis();
            ChatMessage oldest = messageBuffer.peekFirst();
            if (now - oldest.getTimestamp() > maxCacheDurationMillis) {
                messageBuffer.pollFirst();
            } else {
                break;
            }
        }
    }

    //按时间获取聊天消息
    public List<ChatMessage> getMessageByTime(long durationMillis) {
        long now = System.currentTimeMillis();
        List<ChatMessage> result = new ArrayList<>();

        for (ChatMessage msg : messageBuffer) {
            if (now - msg.getTimestamp() <= durationMillis) {
                result.add(msg);
            }
        }

        return result;
    }

    //按玩家获取聊天信息
    public List<ChatMessage> getMessageByPlayer(String playerName, long durationMillis) {
        long now = System.currentTimeMillis();
        List<ChatMessage> result = new ArrayList<>();

        for (ChatMessage message : messageBuffer) {
            if (message.getPlayerName().equalsIgnoreCase(playerName) && now - message.getTimestamp() <= durationMillis) {
                result.add(message);
            }
        }

        return result;
    }

    //按服务器获取聊天消息
    public List<ChatMessage> getMessageByServer(String serverName, long durationMillis) {
        long now = System.currentTimeMillis();
        List<ChatMessage> result = new ArrayList<>();

        for (ChatMessage message : messageBuffer) {
            if (message.getServerName().equalsIgnoreCase(serverName) && now - message.getTimestamp() <= durationMillis) {
                result.add(message);
            }
        }

        return result;
    }

    //获取缓存量
    public int getMessageListSize() {
        return messageBuffer.size();
    }

}
