package org.allivilsey.youmuchan;

import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class MessageSender {
    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Player fictionalPlayer;

    public MessageSender(ProxyServer proxyServer, Logger logger, Player fictionalPlayer) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.fictionalPlayer = fictionalPlayer;
    }

    // 以虚构玩家身份发送消息
    public void send(String message) {
        if (message == null) {
            return;
        }

        String normalized = message.strip();
        if (normalized.isEmpty()) {
            return;
        }

        PlayerChatEvent event = new PlayerChatEvent(fictionalPlayer, normalized);
        proxyServer.getEventManager().fire(event)
                .thenCompose(this::dispatchByResult)
                .exceptionally(throwable -> {
                    logger.error("虚构玩家发送消息失败", throwable);
                    return null;
                });
    }

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

            return proxyServer.getCommandManager()
                    .executeAsync(fictionalPlayer, commandLine)
                    .thenApply(ignored -> null);
        }

        Component component = Component.text("<" + fictionalPlayer.getUsername() + "> " + output);
        for (Player player : proxyServer.getAllPlayers()) {
            player.sendMessage(component);
        }

        proxyServer.getConsoleCommandSource().sendMessage(component);
        return CompletableFuture.completedFuture(null);
    }
}
