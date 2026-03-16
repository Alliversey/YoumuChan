package org.allivilsey.youmuchan.paper;

import org.bukkit.plugin.Plugin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

public class Hanrei {

    private final Plugin plugin;
    private final String host;
    private final int port;
    private final int timeoutMs;
    private final PlayerNameRequestBridge playerNameRequestBridge;
    private volatile boolean enabled;
    private final Object socketLock = new Object();
    private final Object sendLock = new Object();
    private Socket channelSocket;
    private DataInputStream channelIn;
    private DataOutputStream channelOut;
    private Thread receiveThread;

    public Hanrei(Plugin plugin, String host, int port, int timeoutMs) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
        this.playerNameRequestBridge = new PlayerNameRequestBridge();
    }

    public synchronized void register() {
        // 标记 TCP 通道启用
        this.enabled = true;
        plugin.getLogger().info("已启用 TCP 通道: " + host + ":" + port);
        startReceiveLoop();
    }

    public synchronized void unregister() {
        // 标记 TCP 通道停用
        this.enabled = false;
        stopReceiveLoop();
    }

    // 发送游戏内信息
    public void sendInGameInfo(InGameInfo info) {
        if (!enabled || info == null) {
            return;
        }

        String payload = HanreiFormatter.formatInfo(info);

        if (payload == null) {
            return;
        }

        send(payload, "info");
    }

    // 发送 fuel
    public void sendFuel(FuelInfo info) {
        if (!enabled) {
            return;
        }

        String payload = HanreiFormatter.formatFuel(info);

        if (payload == null) {
            return;
        }

        send(payload, "fuel");
    }

    // 发送 focus
    public void sendFocus(FocusInfo info) {
        if (!enabled || info == null) {
            return;
        }

        String payload = HanreiFormatter.formatFocus(info);

        if (payload == null) {
            return;
        }

        send(payload, "focus");
    }

    private void startReceiveLoop() {
        if (receiveThread != null) {
            return;
        }

        receiveThread = new Thread(this::receiveLoop, "youmuchan-paper-tcp-receive");
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    private void stopReceiveLoop() {
        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread = null;
        }
        closeChannel();
    }

    private void receiveLoop() {
        while (enabled) {
            if (!ensureConnected()) {
                sleepQuietly(1000L);
                continue;
            }

            DataInputStream in = channelIn;
            if (in == null) {
                closeChannel();
                sleepQuietly(200L);
                continue;
            }

            try {
                String type = in.readUTF();
                String payload = in.readUTF();
                if ("player_name_request".equals(type)) {
                    handlePlayerNameRequest(payload);
                } else {
                    plugin.getLogger().info("收到未知 TCP 消息类型: " + type);
                }
            } catch (EOFException | SocketException e) {
                if (enabled) {
                    plugin.getLogger().warning("TCP 连接已断开: " + e.getMessage());
                }
                closeChannel();
            } catch (IOException e) {
                if (enabled) {
                    plugin.getLogger().warning("TCP 读取失败: " + e.getMessage());
                }
                closeChannel();
            }
        }
    }

    private boolean ensureConnected() {
        synchronized (socketLock) {
            if (channelSocket != null && channelSocket.isConnected() && !channelSocket.isClosed()) {
                return true;
            }

            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), timeoutMs);
                channelSocket = socket;
                channelIn = new DataInputStream(socket.getInputStream());
                channelOut = new DataOutputStream(socket.getOutputStream());
                plugin.getLogger().info("已连接 Velocity TCP: " + host + ":" + port);
                return true;
            } catch (IOException e) {
                plugin.getLogger().warning("连接 Velocity TCP 失败: " + e.getMessage());
                closeChannel();
                return false;
            }
        }
    }

    private void closeChannel() {
        synchronized (socketLock) {
            if (channelIn != null) {
                try {
                    channelIn.close();
                } catch (IOException ignored) {
                }
                channelIn = null;
            }

            if (channelOut != null) {
                try {
                    channelOut.close();
                } catch (IOException ignored) {
                }
                channelOut = null;
            }

            if (channelSocket != null) {
                try {
                    channelSocket.close();
                } catch (IOException ignored) {
                }
                channelSocket = null;
            }
        }
    }

    private void send(String payload, String type) {
        if (!enabled) {
            return;
        }

        if (!ensureConnected()) {
            return;
        }

        synchronized (sendLock) {
            DataOutputStream out;
            synchronized (socketLock) {
                out = channelOut;
            }
            if (out == null) {
                closeChannel();
                return;
            }

            try {
                out.writeUTF(type);
                out.writeUTF(payload);
                out.flush();
            } catch (IOException e) {
                plugin.getLogger().warning("发送失败: " + e.getMessage());
                closeChannel();
            }
        }
    }

    private void handlePlayerNameRequest(String payload) {
        String responsePayload = playerNameRequestBridge.buildPlayerNameResponse(payload);
        if (responsePayload == null) {
            return;
        }
        send(responsePayload, "player_name_response");
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
