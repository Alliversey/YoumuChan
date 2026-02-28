package org.allivilsey.youmuchan;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

// Broadcast AI replies to players connected to each backend server.
public class MessageSender {
    private final ProxyServer proxyServer;
    private final Logger logger;
    private final String senderName;

    public MessageSender(ProxyServer proxyServer, Logger logger, String senderName) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.senderName = senderName;
    }

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

        proxyServer.getConsoleCommandSource().sendMessage(component);
        logger.debug("Broadcasted AI message to {} backend server(s)", proxyServer.getAllServers().size());
    }
}
