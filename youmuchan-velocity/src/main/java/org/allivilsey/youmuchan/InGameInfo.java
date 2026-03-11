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

    //获取方法数据类型
    public InfoType getInfoType() {
        return type;
    }
    //获取玩家名
    public String getPlayerName() {
        return playerName;
    }
    //获取服务器名
    public String getServerName() {
        return serverName;
    }
    //获取数据内容
    public String getContent() {
        return content;
    }
    //获取时间戳
    public Long getTimestamp() {
        return timestamp;
    }
}
