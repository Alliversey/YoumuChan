package org.allivilsey.youmuchan;

import org.slf4j.Logger;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Hanrei {

    private final Logger logger;
    private final String host;
    private final int port;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private ExecutorService clientPool;

    public Hanrei(Logger logger, String host, int port) {
        this.logger = logger;
        this.host = host;
        this.port = port;
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
        try (Socket client = socket; DataInputStream in = new DataInputStream(client.getInputStream())) {
            while (true) {
                try {
                    String type = in.readUTF();
                    if ("ping".equalsIgnoreCase(type)) {
                        String playerId = in.readUTF();
                        logger.info("收到 Paper ping: {}", playerId);
                    } else {
                        logger.info("收到未知 TCP 消息类型: {}", type);
                        break;
                    }
                } catch (EOFException eof) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.warn("TCP 处理连接失败: {}", e.getMessage());
        }
    }
}
