package org.allivilsey.youmuchan.paper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class YoumuChanPaper extends JavaPlugin {

    private VelocityMessenger velocityMessenger;

    @Override
    public void onEnable() {
        // 初始化与 Velocity 通讯
        this.velocityMessenger = new VelocityMessenger(this);
        this.velocityMessenger.register();

        // 注册监听器
        Bukkit.getPluginManager().registerEvents(new YoumuListener(this, velocityMessenger), this);

        getLogger().info("YoumuChan Paper 已启动");
    }

    @Override
    public void onDisable() {
        if (velocityMessenger != null) {
            velocityMessenger.unregister();
        }
        getLogger().info("YoumuChan Paper 已停止");
    }

    public VelocityMessenger getVelocityMessenger() {
        return velocityMessenger;
    }
}
