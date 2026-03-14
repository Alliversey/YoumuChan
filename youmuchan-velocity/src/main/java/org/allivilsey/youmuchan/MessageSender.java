package org.allivilsey.youmuchan;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.util.Tristate;

public class MessageSender {
    private final ProxyServer proxyServer;
    private final String senderName;
    private final InGameInfoCollector collector;
    private final LuckPerms luckPerms;

    public MessageSender(ProxyServer proxyServer, String senderName, InGameInfoCollector collector, LuckPerms luckPerms) {
        this.proxyServer = proxyServer;
        this.senderName = senderName;
        this.collector = collector;
        this.luckPerms = luckPerms;
    }

    //向所有玩家发送
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
                .append(Component.text("[吉祥物]", NamedTextColor.AQUA))
                .append(Component.text(senderName, NamedTextColor.WHITE))
                .append(Component.text(" > ", NamedTextColor.AQUA))
                .append(Component.text(normalized, NamedTextColor.WHITE));
        for (Player player : proxyServer.getAllPlayers()) {
            if (canReceiveMessage(player)) {
                player.sendMessage(component);
            }
        }

        InGameInfo info = new InGameInfo(InfoType.CHAT, "you", null, message);
        collector.addInfo(info);

        proxyServer.getConsoleCommandSource().sendMessage(component);
    }

    private boolean canReceiveMessage(Player player) {
        if (luckPerms == null) {
            return true;
        }
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return true;
        }
        Tristate state = user.getCachedData().getPermissionData().checkPermission("youmu.visible");
        return state != Tristate.FALSE;
    }
}
