package org.allivilsey.youmuchan;

// 统一定义可进入 AI 上下文的信息来源类型。
public enum InfoType {
    CHAT, // 玩家聊天内容。
    PLAYER_EVENT, // 玩家行为事件。
    SERVER_EVENT, // 服务器系统事件。
    PUNISH_EVENT //LiteBans 惩罚事件
}
