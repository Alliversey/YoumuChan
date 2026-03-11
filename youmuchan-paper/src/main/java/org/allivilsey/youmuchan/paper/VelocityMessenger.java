package org.allivilsey.youmuchan.paper;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class VelocityMessenger implements PluginMessageListener {

    public static final String CHANNEL = "youmuchan:main";

    private final Plugin plugin;

    public VelocityMessenger(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        // 注册双向通道，便于后续与 Velocity 交换数据
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    public void unregister() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
    }

    public void sendPlayerPing(Player player) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(output)) {
            data.writeUTF("ping");
            data.writeUTF(player.getUniqueId().toString());
        } catch (IOException e) {
            plugin.getLogger().warning("发送到 Velocity 的数据打包失败");
            return;
        }
        player.sendPluginMessage(plugin, CHANNEL, output.toByteArray());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel)) {
            return;
        }
        // 仅记录长度，协议细节后续再约定
        plugin.getLogger().info("收到来自 Velocity 的消息，长度=" + message.length);
    }
}
