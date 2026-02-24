package org.allivilsey.youmuchan;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Plugin(
        id = "youmuchan",
        name = "YoumuChan",
        version = "2.0",
        authors = {"Allivilsey"}
)
public class YoumuChan {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;

    private GhostInThePlugin ghostInThePlugin;

    @Inject
    public YoumuChan(ProxyServer proxyServer, Logger logger, Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            logger.error("无法创建插件数据目录", e);
            return;
        }

        Properties config = loadConfig();

        logger.info("YoumuChan 正在启动");

        InGameInfoCollector collector = new InGameInfoCollector(
                Long.parseLong(config.getProperty("cache-duration-ms")),
                Integer.parseInt(config.getProperty("cache-max-size"))
        );

        proxyServer.getEventManager().register(this, new InGameInfoListener(collector));

        AIContextBuilder contextBuilder = new AIContextBuilder(
                collector,
                config.getProperty("youmu-model"),
                Float.parseFloat(config.getProperty("youmu-temperature")),
                Long.parseLong(config.getProperty("time-window-ms"))
        );

        ApiProcessor apiProcessor = new ApiProcessor(config.getProperty("api-key"));

        KaianPassageway passageway = new KaianPassageway(
                contextBuilder,
                new AIBorderPromptFormatter(),
                new AIYoumuPromptFormatter(),
                apiProcessor,
                config.getProperty("broder-model"),
                Float.parseFloat(config.getProperty("broder-temperature")),
                config.getProperty("youmu-moder"),
                Float.parseFloat(config.getProperty("youmu-temperature"))
        );

        MentalStateController mentalStateController = new MentalStateController(proxyServer);
        FocusController focusController = new FocusController();
        MessageSender messageSender = new MessageSender();

        ghostInThePlugin = new GhostInThePlugin(
                proxyServer,
                this,
                passageway,
                mentalStateController,
                focusController,
                messageSender,
                Long.parseLong(config.getProperty("base-interval-ms"))
        );

        ghostInThePlugin.youmuStart();

        logger.info("YoumuChan 已启动");
    }

    private Properties loadConfig() {

        Path configFile = dataDirectory.resolve("config.properties");

        if (Files.notExists(configFile)) {
            saveDefaultFile(configFile);
        }

        Properties properties = new Properties();

        try {
            properties.load(Files.newInputStream(configFile));
        } catch (IOException e) {
            logger.error("配置文件加载失败: " + e);
        }

        return properties;
    }

    private void saveDefaultFile(Path file) {

        try (var in = getClass().getResourceAsStream("/config.properties")) {
            Files.copy(in, file);
        } catch (IOException e) {
            logger.error("配置文件设置失败: " + e);
        }
    }
}