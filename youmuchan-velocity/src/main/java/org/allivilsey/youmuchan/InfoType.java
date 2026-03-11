package org.allivilsey.youmuchan;

// 统一定义可进入 AI 上下文的信息来源类型。
public enum InfoType {
    CHAT, // 玩家聊天内容。
    PLAYER_EVENT, // 玩家上下线等行为事件。
    SERVER_EVENT // 服务器侧系统事件。
}
