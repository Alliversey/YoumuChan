package org.allivilsey.youmuchan;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// 插件入口：加载配置并装配采集、决策、推理与消息发送组件。
@Plugin(id = "youmuchan", name = "YoumuChan", version = "2.0", authors = { "Allivilsey" })
public class YoumuChan {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;

    private GhostInThePlugin ghostInThePlugin;
    private MentalStateController mentalStateController;
    private DebugInfo debugInfo;
    private InGameInfoCollector collector;
    FocusController focusController = new FocusController();

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

        startPlugin();

        // 注册 /youmu 命令。
        CommandMeta meta = proxyServer.getCommandManager()
                .metaBuilder("youmu")
                .build();
        proxyServer.getCommandManager().register(meta, new YoumuCommand(this));

        logger.info("YoumuChan 已启动");
    }

    // 重载插件：停止调度 -> 注销监听器 -> 重新读取配置并装配组件。
    public void reload() {
        logger.info("YoumuChan 正在重载");

        if (ghostInThePlugin != null) {
            ghostInThePlugin.youmuStop();
        }

        proxyServer.getEventManager().unregisterListeners(this);

        startPlugin();

        logger.info("YoumuChan 已重载");
    }

    // 读取配置并装配全部运行时组件。
    private void startPlugin() {
        ConfigurationNode config = loadConfig();

        // 读取运行参数；缺省值用于首次启动或配置缺失场景。
        String apiKey = config.node("api_key").getString("");
        String apiUrl = config.node("api_url")
                .getString("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");
        boolean debugMode = config.node("debug_mode").getBoolean(false);
        String borderModel = config.node("border_model").getString("qwen3.5-flash");
        float borderTemperature = (float) config.node("border_temperature").getDouble(0.0D);
        String youmuModel = config.node("youmu_model").getString("qwen3.5-plus");
        float youmuTemperature = (float) config.node("youmu_temperature").getDouble(0.7D);
        long timeWindowMs = config.node("time_window_ms").getLong(60000L);
        long baseIntervalMs = config.node("base_interval_ms").getLong(15000L);
        long cacheDurationMs = config.node("cache_duration_ms").getLong(300000L);
        int cacheMaxSize = config.node("cache_max_size").getInt(100);
        double halfLifeSeconds = config.node("half_life_seconds").getDouble(60.0);
        String youmuName = config.node("youmu_name").getString("YoumuChan");

        logger.info("YoumuChan 正在启动");

        // 采集层：记录游戏内事件并按时间窗口提供检索。
        this.collector = new InGameInfoCollector(cacheDurationMs, cacheMaxSize, proxyServer);

        // 热度层：根据玩家行为动态调整 AI 调度节奏。
        HeatController heatController = new HeatController(halfLifeSeconds);

        // 注册事件监听器：信息采集与热度更新。
        proxyServer.getEventManager().register(this, new InGameInfoListener(collector));
        proxyServer.getEventManager().register(this, new HeatControllerListener(heatController));
        proxyServer.getEventManager().register(this, new FocusControllerListener(focusController));

        // 上下文构建层：从采集信息生成模型输入上下文。
        AIContextBuilder contextBuilder = new AIContextBuilder(
                collector,
                heatController,
                youmuModel,
                youmuTemperature,
                timeWindowMs);

        ApiProcessor apiProcessor = new ApiProcessor(apiKey, apiUrl, debugMode, logger, proxyServer);
        // 推理通道：边界分析模型 + 主对话模型串联调用。
        KaianPassageway passageway = new KaianPassageway(
                contextBuilder,
                new AIBorderPromptFormatter(),
                new AIYoumuPromptFormatter(focusController, collector),
                apiProcessor,
                borderModel,
                borderTemperature,
                youmuModel,
                youmuTemperature);

        mentalStateController = new MentalStateController(proxyServer, debugMode);

        // 直接广播消息到各个子服，并使用配置名称作为消息前缀。
        MessageSender messageSender = new MessageSender(proxyServer, youmuName, collector);

        // 启动总调度器。
        ghostInThePlugin = new GhostInThePlugin(
                proxyServer,
                this,
                passageway,
                mentalStateController,
                focusController,
                heatController,
                messageSender,
                baseIntervalMs);

        debugInfo = new DebugInfo(
                proxyServer,
                this,
                heatController,
                focusController,
                ghostInThePlugin,
                collector);
        debugInfo.start();

        ghostInThePlugin.youmuStart();
    }

    // 切换 Debug 模式，修改配置并重载
    public boolean toggleDebug() {
        Path configFile = dataDirectory.resolve("config.yml");
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configFile)
                .build();
        try {
            ConfigurationNode config = loader.load();
            boolean currentMode = config.node("debug_mode").getBoolean(false);
            boolean newMode = !currentMode;

            // 使用文本替换来保留 YAML 文件中的注释和格式
            java.util.List<String> lines = Files.readAllLines(configFile);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.trim().startsWith("debug_mode:")) {
                    lines.set(i, line.replaceFirst("(?i)" + currentMode, String.valueOf(newMode)));
                    break;
                }
            }
            Files.write(configFile, lines);

            reload();
            return newMode;
        } catch (IOException e) {
            logger.error("配置文件修改失败", e);
            return false;
        }
    }

    // 设置边界分析模型，修改配置并重载
    public boolean setBorderModel(String modelName) {
        Path configFile = dataDirectory.resolve("config.yml");
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configFile)
                .build();
        try {
            loader.load();

            // 使用文本替换来保留 YAML 文件中的注释和格式
            java.util.List<String> lines = Files.readAllLines(configFile);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.trim().startsWith("border_model:")) {
                    lines.set(i, "border_model: " + modelName);
                    break;
                }
            }
            Files.write(configFile, lines);

            reload();
            return true;
        } catch (IOException e) {
            logger.error("配置文件修改失败", e);
            return false;
        }
    }

    // 设置妖梦对话模型，修改配置并重载
    public boolean setYoumuModel(String modelName) {
        Path configFile = dataDirectory.resolve("config.yml");
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configFile)
                .build();
        try {
            loader.load();

            // 使用文本替换来保留 YAML 文件中的注释和格式
            java.util.List<String> lines = Files.readAllLines(configFile);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.trim().startsWith("youmu_model:")) {
                    lines.set(i, "youmu_model: " + modelName);
                    break;
                }
            }
            Files.write(configFile, lines);

            reload();
            return true;
        } catch (IOException e) {
            logger.error("配置文件修改失败", e);
            return false;
        }
    }

    // 设置 API Key，修改配置并重载
    public boolean setApiKey(String apiKey) {
        Path configFile = dataDirectory.resolve("config.yml");
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configFile)
                .build();
        try {
            loader.load();

            // 使用文本替换来保留 YAML 文件中的注释和格式
            java.util.List<String> lines = Files.readAllLines(configFile);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.trim().startsWith("api_key:")) {
                    lines.set(i, "api_key: \"" + apiKey + "\"");
                    break;
                }
            }
            Files.write(configFile, lines);

            reload();
            return true;
        } catch (IOException e) {
            logger.error("配置文件修改失败", e);
            return false;
        }
    }

    // 设置 API URL，修改配置并重载
    public boolean setApiUrl(String apiUrl) {
        Path configFile = dataDirectory.resolve("config.yml");
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configFile)
                .build();
        try {
            loader.load();

            // 使用文本替换来保留 YAML 文件中的注释和格式
            java.util.List<String> lines = Files.readAllLines(configFile);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.trim().startsWith("api_url:")) {
                    lines.set(i, "api_url: \"" + apiUrl + "\"");
                    break;
                }
            }
            Files.write(configFile, lines);

            reload();
            return true;
        } catch (IOException e) {
            logger.error("配置文件修改失败", e);
            return false;
        }
    }

    // 返回心智状态控制器，供命令处理器直接调用。
    public MentalStateController getMentalStateController() {
        return mentalStateController;
    }

    public DebugInfo getDebugInfo() {
        return debugInfo;
    }

    public InGameInfoCollector getInGameInfoCollector() {
        return collector;
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
