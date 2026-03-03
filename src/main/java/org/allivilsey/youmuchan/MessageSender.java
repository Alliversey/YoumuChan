package org.allivilsey.youmuchan;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class MessageSender {
    private final ProxyServer proxyServer;
    private final String senderName;
    private final InGameInfoCollector collector;

    public MessageSender(ProxyServer proxyServer, String senderName, InGameInfoCollector collector) {
        this.proxyServer = proxyServer;
        this.senderName = senderName;
        this.collector = collector;
    }

    //向全子服广播
    public void send(String message) {
        if (message == null) {
            return;
        }

        String normalized = message.strip();
        if (normalized.isEmpty()) {
            return;
        }
        Component component = Component.text("")
                .append(Component.text(" · ", NamedTextColor.WHITE))
                .append(Component.text("生存服主世界 ", NamedTextColor.GOLD))
                .append(Component.text(">> ", NamedTextColor.AQUA))
                .append(Component.text(senderName, NamedTextColor.WHITE))
                .append(Component.text(" > ", NamedTextColor.AQUA))
                .append(Component.text(normalized, NamedTextColor.WHITE));
        for (RegisteredServer server : proxyServer.getAllServers()) {
            server.sendMessage(component);
        }

        InGameInfo info = new InGameInfo(InfoType.CHAT, "you", null, message);
        collector.addInfo(info);

        proxyServer.getConsoleCommandSource().sendMessage(component);
    }
}
