package org.allivilsey.youmuchan;

import org.slf4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public class Hanrei {

    private static final long PLAYER_NAME_REQUEST_TIMEOUT_MS = 3000L;

    private final Logger logger;
    private final String host;
    private final int port;
    private final HanreiParser hanreiParser;
    private final Map<String, ClientChannel> clientChannels = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> pendingPlayerNameRequests = new ConcurrentHashMap<>();
    private final AtomicLong requestIdSequence = new AtomicLong(0L);
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private ExecutorService clientPool;

    public Hanrei(Logger logger, String host, int port, InGameInfoCollector collector, HeatController heatController, FocusController focusController) {
        this.logger = logger;
        this.host = host;
        this.port = port;
        this.hanreiParser = new HanreiParser(collector, heatController, focusController);
    }

    public synchronized void startHanrei() {
        if (serverSocket != null) {
            return;
        }
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(host, port));

            clientPool = Executors.newCachedThreadPool(r -> {
                Thread thread = new Thread(r, "youmuchan-tcp-client");
                thread.setDaemon(true);
                return thread;
            });

            acceptThread = new Thread(this::acceptLoop, "youmuchan-tcp-accept");
            acceptThread.setDaemon(true);
            acceptThread.start();

            logger.info("TCP 接收器已启动: {}:{}", host, port);
        } catch (IOException e) {
            logger.error("TCP 接收器启动失败", e);
            stopHanrei();
        }
    }

    public synchronized void stopHanrei() {
        if (serverSocket == null) {
            return;
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.warn("TCP 接收器关闭失败: {}", e.getMessage());
        }
        serverSocket = null;

        if (acceptThread != null) {
            acceptThread.interrupt();
            acceptThread = null;
        }

        if (clientPool != null) {
            clientPool.shutdownNow();
            clientPool = null;
        }

        clientChannels.clear();
        pendingPlayerNameRequests.values().forEach(future -> future.complete(null));
        pendingPlayerNameRequests.clear();

        logger.info("TCP 接收器已停止");
    }

    private void acceptLoop() {
        while (serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                clientPool.execute(() -> handleClient(socket));
            } catch (SocketException e) {
                if (serverSocket == null || serverSocket.isClosed()) {
                    break;
                }
                logger.warn("TCP 接收异常: {}", e.getMessage());
            } catch (IOException e) {
                logger.warn("TCP 接收异常: {}", e.getMessage());
            }
        }
    }

    private void handleClient(Socket socket) {
        try (Socket client = socket;
             DataInputStream in = new DataInputStream(client.getInputStream());
             DataOutputStream out = new DataOutputStream(client.getOutputStream())) {
            String channelId = client.getRemoteSocketAddress() + "-" + System.nanoTime();
            clientChannels.put(channelId, new ClientChannel(out));

            try {
                while (true) {
                    try {
                        String type = in.readUTF();
                        String payload = in.readUTF();
                        switch (type) {
                            case "info" -> hanreiParser.parseInfo(payload);
                            case "fuel" -> hanreiParser.parseFuel(payload);
                            case "focus" -> hanreiParser.parseFocus(payload);
                            case "player_name_response" -> handlePlayerNameResponse(payload);
                            default -> logger.info("收到未知 TCP 消息类型: {}", type);
                        }
                    } catch (EOFException eof) {
                        break;
                    }
                }
            } finally {
                clientChannels.remove(channelId);
            }
        } catch (IOException e) {
            logger.warn("TCP 处理连接失败: {}", e.getMessage());
        }
    }

    // 向 Paper 端请求玩家名：输入 uuid，返回玩家名（失败返回 null）
    public String sendPlayerNameRequest(String uuidText) {
        String normalizedUuid = trimToNull(uuidText);
        if (normalizedUuid == null) {
            return null;
        }

        List<ClientChannel> channels = new ArrayList<>(clientChannels.values());
        if (channels.isEmpty()) {
            return null;
        }

        for (ClientChannel channel : channels) {
            String requestId = "player-name-" + requestIdSequence.incrementAndGet();
            CompletableFuture<String> future = new CompletableFuture<>();
            pendingPlayerNameRequests.put(requestId, future);

            String payload = HanreiFormatter.formatPlayerNameRequest(requestId, normalizedUuid);
            if (payload == null) {
                pendingPlayerNameRequests.remove(requestId);
                continue;
            }
            if (!channel.send("player_name_request", payload)) {
                pendingPlayerNameRequests.remove(requestId);
                continue;
            }

            try {
                String playerName = future.get(PLAYER_NAME_REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                playerName = trimToNull(playerName);
                if (playerName != null) {
                    return playerName;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (TimeoutException | ExecutionException ignored) {
            } finally {
                pendingPlayerNameRequests.remove(requestId);
            }
        }

        return null;
    }

    private void handlePlayerNameResponse(String payload) {
        HanreiFormatter.PlayerNameResponse response = HanreiFormatter.parsePlayerNameResponse(payload);
        if (response == null) {
            return;
        }

        String requestId = response.requestId();
        if (requestId == null) {
            return;
        }

        String playerName = trimToNull(response.playerName());
        CompletableFuture<String> future = pendingPlayerNameRequests.remove(requestId);
        if (future != null) {
            future.complete(playerName);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    private static class ClientChannel {
        private final DataOutputStream out;
        private final Object writeLock = new Object();

        private ClientChannel(DataOutputStream out) {
            this.out = out;
        }

        private boolean send(String type, String payload) {
            synchronized (writeLock) {
                try {
                    out.writeUTF(type);
                    out.writeUTF(payload);
                    out.flush();
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }
        }
    }
}
