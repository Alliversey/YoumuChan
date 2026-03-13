package org.allivilsey.youmuchan.paper;

import org.bukkit.plugin.Plugin;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Hanrei {

    private final Plugin plugin;
    private final String host;
    private final int port;
    private final int timeoutMs;
    private volatile boolean enabled;

    public Hanrei(Plugin plugin, String host, int port, int timeoutMs) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
    }

    public void register() {
        // 标记 TCP 通道启用
        this.enabled = true;
        plugin.getLogger().info("已启用 TCP 通道: " + host + ":" + port);
    }

    public void unregister() {
        // 标记 TCP 通道停用
        this.enabled = false;
    }

    public void sendInGameInfo(InGameInfo info) {
        if (!enabled || info == null) {
            return;
        }

        String payload = HanreiMessageFormatter.format(info);
        if (payload == null) {
            return;
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);

            try (DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                // 协议：UTF 字符串类型 + UTF 字符串载荷
                out.writeUTF(HanreiMessageFormatter.MESSAGE_TYPE);
                out.writeUTF(payload);
                out.flush();
            }
        } catch (IOException e) {
            plugin.getLogger().warning("发送失败: " + e.getMessage());
        }
    }
}
