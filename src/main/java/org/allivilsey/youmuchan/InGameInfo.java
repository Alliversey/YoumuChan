package org.allivilsey.youmuchan;

public class InGameInfo {

    private final InfoType type;

    private final String playerName;

    private final String serverName;

    private final String content;

    private final Long timestamp;

    //聊天消息数据对象
    public InGameInfo(InfoType type, String playerName, String serverName, String content) {
        this.type = type;
        this.playerName = playerName;
        this.serverName = serverName;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    //获取方法⬇
    public InfoType getInfoType() {
        return type;
    }

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
