package org.allivilsey.youmuchan;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;


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

        Component component = Component.text("<" + senderName + "> " + normalized);
        for (RegisteredServer server : proxyServer.getAllServers()) {
            for (Player player : server.getPlayersConnected()) {
                player.sendMessage(component);
            }
        }

        InGameInfo info = new InGameInfo(InfoType.CHAT, senderName, null, message);
        collector.addInfo(info);

        proxyServer.getConsoleCommandSource().sendMessage(component);
    }
}
