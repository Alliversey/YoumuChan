package org.allivilsey.youmuchan.paper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class YoumuChan extends JavaPlugin {

    private Hanrei hanrei;

    @Override
    public void onEnable() {
        // 读取配置并初始化 TCP 通讯
        saveDefaultConfig();
        String host = getConfig().getString("velocity_tcp_host", "127.0.0.1");
        int port = getConfig().getInt("velocity_tcp_port", 55500);
        int timeoutMs = getConfig().getInt("velocity_tcp_timeout_ms", 3000);

        this.hanrei = new Hanrei(this, host, port, timeoutMs);
        this.hanrei.register();

        // 注册监听器
        Bukkit.getPluginManager().registerEvents(new YoumuListener(this, hanrei), this);

        getLogger().info("YoumuChan 已启动");
    }

    @Override
    public void onDisable() {
        if (hanrei != null) {
            hanrei.unregister();
        }
        getLogger().info("YoumuChan 已停止");
    }

    public Hanrei getVelocityMessenger() {
        return hanrei;
    }
}
