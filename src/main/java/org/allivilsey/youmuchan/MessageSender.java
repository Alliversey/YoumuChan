package org.allivilsey.youmuchan;

import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

// 以虚拟玩家身份分发 AI 回复，并复用 Velocity 的聊天事件链与命令处理链。
public class MessageSender {
    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Player fictionalPlayer;

    public MessageSender(ProxyServer proxyServer, Logger logger, Player fictionalPlayer) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.fictionalPlayer = fictionalPlayer;
    }

    // 发送入口：先标准化文本，再触发 PlayerChatEvent 交由其他插件拦截/改写。
    public void send(String message) {
        if (message == null) {
            return;
        }

        String normalized = message.strip();
        if (normalized.isEmpty()) {
            return;
        }

        PlayerChatEvent event = new PlayerChatEvent(fictionalPlayer, normalized);
        proxyServer.getEventManager().fire(event).thenCompose(this::dispatchByResult).exceptionally(throwable -> {
                    logger.error("虚构玩家发送消息失败", throwable);
                    return null;
                });
    }

    // 按事件结果执行最终分发：
    // 1) 被拒绝则终止；
    // 2) '/' 开头按命令执行；
    // 3) 其余按普通聊天广播。
    private CompletableFuture<Void> dispatchByResult(PlayerChatEvent event) {
        PlayerChatEvent.ChatResult result = event.getResult();

        if (!result.isAllowed()) {
            return CompletableFuture.completedFuture(null);
        }

        String output = result.getMessage().orElse(event.getMessage()).strip();
        if (output.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        if (output.startsWith("/")) {
            String commandLine = output.substring(1).strip();
            if (commandLine.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            // 以虚拟玩家身份执行命令，保持权限与事件上下文一致。
            return proxyServer.getCommandManager()
                    .executeAsync(fictionalPlayer, commandLine)
                    .thenApply(ignored -> null);
        }

        // 普通聊天消息同时投递给在线玩家与控制台。
        Component component = Component.text("<" + fictionalPlayer.getUsername() + "> " + output);
        for (Player player : proxyServer.getAllPlayers()) {
            player.sendMessage(component);
        }

        proxyServer.getConsoleCommandSource().sendMessage(component);
        return CompletableFuture.completedFuture(null);
    }
}
