package org.allivilsey.youmuchan;

public class ChatMessage {

    private final String playerName;

    private final String serverName;

    private final String content;

    private final Long timestamp;

    //聊天消息数据对象
    public ChatMessage(String playerName, String serverName, String content) {
        this.playerName = playerName;
        this.serverName = serverName;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    //获取方法⬇
    public String getPlayerName() {
        return playerName;
    }

    public String getServerName() {
        return serverName;
    }

    public String getContent() {
        return content;
    }

    public Long getTimestamp() {
        return timestamp;
    }
}
