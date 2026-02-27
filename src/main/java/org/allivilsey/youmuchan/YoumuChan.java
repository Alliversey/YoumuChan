package org.allivilsey.youmuchan;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// 插件入口：加载配置并装配采集、决策、推理与消息发送组件。
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
    public YoumuChan(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // 确保插件数据目录可用，用于读取/落盘配置。
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            logger.error("无法创建插件数据目录", e);
            return;
        }

        ConfigurationNode config = loadConfig();

        // 读取运行参数；缺省值用于首次启动或配置缺失场景。
        String apiKey = config.node("api_key").getString("");
        String borderModel = config.node("border_model").getString("qwen3.5-flash");
        float borderTemperature = (float) config.node("border_temperature").getDouble(0.0D);
        String youmuModel = config.node("youmu_model").getString("qwen3.5-plus");
        float youmuTemperature = (float) config.node("youmu_temperature").getDouble(0.7D);
        long timeWindowMs = config.node("time_window_ms").getLong(60000L);
        long baseIntervalMs = config.node("base_interval_ms").getLong(15000L);
        long cacheDurationMs = config.node("cache_duration_ms").getLong(300000L);
        int cacheMaxSize = config.node("cache_max_size").getInt(100);
        double halfLifeSeconds = config.node("half_life_seconds").getDouble(30.0);
        String fictionalPlayerName = config.node("fictional_player_name").getString("YoumuChan");

        logger.info("YoumuChan 正在启动");

        // 采集层：记录游戏内事件并按时间窗口提供检索。
        InGameInfoCollector collector = new InGameInfoCollector(
                cacheDurationMs,
                cacheMaxSize
        );

        // 热度层：根据玩家行为动态调整 AI 调度节奏。
        HeatController heatController = new HeatController(halfLifeSeconds);

        // 注册事件监听器：信息采集与热度更新。
        proxyServer.getEventManager().register(this, new InGameInfoListener(collector));
        proxyServer.getEventManager().register(this, new HeatControllerListener(heatController));

        // 上下文构建层：从采集信息生成模型输入上下文。
        AIContextBuilder contextBuilder = new AIContextBuilder(
                collector,
                heatController,
                youmuModel,
                youmuTemperature,
                timeWindowMs
        );

        ApiProcessor apiProcessor = new ApiProcessor(apiKey);

        // 推理通道：边界分析模型 + 主对话模型串联调用。
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

        // 构造虚拟玩家并复用服务端聊天/命令分发链路。
        YoumuVirtualize youmuVirtualize = new YoumuVirtualize();
        Player fictionalPlayer = youmuVirtualize.create(fictionalPlayerName);
        MessageSender messageSender = new MessageSender(proxyServer, logger, fictionalPlayer);

        // 启动总调度器。
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

    // 加载配置文件；不存在时先写入默认模板。
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

    // 将类路径下的默认 config.yml 复制到插件数据目录。
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
