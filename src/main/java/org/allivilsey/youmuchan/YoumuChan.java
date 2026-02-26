package org.allivilsey.youmuchan;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

        ConfigurationNode config = loadConfig();

        String apiKey = config.node("api_key").getString("your-api-key");
        String borderModel = config.node("border_model").getString("qwen-plus");
        float borderTemperature = (float) config.node("border_temperature").getDouble(0.0D);
        String youmuModel = config.node("youmu_model").getString("qwen-plus");
        float youmuTemperature = (float) config.node("youmu_temperature").getDouble(0.7D);
        long timeWindowMs = config.node("time_window_ms").getLong(60000L);
        long baseIntervalMs = config.node("base_interval_ms").getLong(15000L);
        long cacheDurationMs = config.node("cache_duration_ms").getLong(300000L);
        int cacheMaxSize = config.node("cache_max_size").getInt(100);
        double halfLifeSeconds = config.node("half_life_seconds").getDouble(30.0);

        logger.info("YoumuChan 正在启动");

        InGameInfoCollector collector = new InGameInfoCollector(
                cacheDurationMs,
                cacheMaxSize
        );

        HeatController heatController = new HeatController(halfLifeSeconds);

        proxyServer.getEventManager().register(this, new InGameInfoListener(collector));
        proxyServer.getEventManager().register(this, new HeatControllerListener(heatController));

        AIContextBuilder contextBuilder = new AIContextBuilder(
                collector,
                heatController,
                youmuModel,
                youmuTemperature,
                timeWindowMs
        );

        ApiProcessor apiProcessor = new ApiProcessor(apiKey);

        KaianPassageway passageway = new KaianPassageway(
                contextBuilder,
                new AIBorderPromptFormatter(),
                new AIYoumuPromptFormatter(),
                apiProcessor,
                borderModel,
                borderTemperature,
                youmuModel,
                youmuTemperature
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
                heatController,
                messageSender,
                baseIntervalMs
        );

        ghostInThePlugin.youmuStart();

        logger.info("YoumuChan 已启动");
    }

    private ConfigurationNode loadConfig() {

        Path configFile = dataDirectory.resolve("config.yml");

        if (Files.notExists(configFile)) {
            saveDefaultFile(configFile);
        }

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configFile)
                .build();

        try {
            return loader.load();
        } catch (IOException e) {
            logger.error("配置文件加载失败", e);
        }

        return loader.createNode();
    }

    private void saveDefaultFile(Path file) {

        try (var in = getClass().getResourceAsStream("/config.yml")) {
            if (in == null) {
                logger.error("未找到默认配置文件: /config.yml");
                return;
            }
            Files.copy(in, file);
        } catch (IOException e) {
            logger.error("配置文件设置失败", e);
        }
    }
}
