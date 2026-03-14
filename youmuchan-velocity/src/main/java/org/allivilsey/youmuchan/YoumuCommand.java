package org.allivilsey.youmuchan;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

// 处理 /youmu 命令，支持子命令 reload / start / stop / debug / clear / mute。
public class YoumuCommand implements SimpleCommand {

    private final YoumuChan plugin;

    public YoumuCommand(YoumuChan plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            YoumuCommandUtil.sendHelpMessage(invocation);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!invocation.source().hasPermission("youmuchan.reload")) {
                    invocation.source().sendMessage(Component.text("没有权限执行此命令。", NamedTextColor.RED));
                    return;
                }
                plugin.reload();
                invocation.source().sendMessage(
                        Component.text("YoumuChan 配置已重载。", NamedTextColor.GREEN));
            }
            case "start" -> {
                if (!invocation.source().hasPermission("youmuchan.start")) {
                    invocation.source().sendMessage(Component.text("没有权限执行此命令。", NamedTextColor.RED));
                    return;
                }
                long duration = YoumuCommandUtil.parseDuration(args);
                if (duration == -1) {
                    invocation.source().sendMessage(
                            Component.text("错误: /youmu start 必须指定持续时间(如 1h, 30m, 10s 或毫秒数)，不能进入永久状态。",
                                    NamedTextColor.RED));
                    return;
                }
                plugin.getMentalStateController().setMentalState(MentalState.DREAM, duration);
                invocation.source().sendMessage(
                        Component.text("YoumuChan 已切换至 DREAM 状态，持续 " + duration + " ms。",
                                NamedTextColor.GREEN));
            }
            case "stop" -> {
                if (!invocation.source().hasPermission("youmuchan.stop")) {
                    invocation.source().sendMessage(Component.text("没有权限执行此命令。", NamedTextColor.RED));
                    return;
                }
                long duration = YoumuCommandUtil.parseDuration(args);
                plugin.getMentalStateController().setMentalState(MentalState.SLEEP, duration);
                invocation.source().sendMessage(
                        Component.text(duration == -1
                                ? "YoumuChan 已永久切换至 SLEEP 状态。"
                                : "YoumuChan 已切换至 SLEEP 状态，持续 " + duration + " ms。",
                                NamedTextColor.GREEN));
            }
            case "debug" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("info")) {
                    if (!invocation.source().hasPermission("youmuchan.debug")) {
                        invocation.source().sendMessage(Component.text("没有权限执行此命令。", NamedTextColor.RED));
                        return;
                    }
                    if (invocation.source() instanceof Player player) {
                        plugin.getDebugInfo().togglePlayer(player);
                    } else {
                        invocation.source().sendMessage(Component.text("只有玩家可以使用此命令。", NamedTextColor.RED));
                    }
                    return;
                }

                if (!invocation.source().hasPermission("youmuchan.debug")) {
                    invocation.source().sendMessage(Component.text("没有权限执行此命令。", NamedTextColor.RED));
                    return;
                }
                boolean newMode = plugin.toggleDebug();
                invocation.source().sendMessage(
                        Component.text("YoumuChan Debug 模式已" + (newMode ? "开启" : "关闭") + "。", NamedTextColor.GREEN));
            }
            case "clear" -> {
                if (!invocation.source().hasPermission("youmuchan.clear")) {
                    invocation.source().sendMessage(Component.text("没有权限执行此命令。", NamedTextColor.RED));
                    return;
                }
                plugin.getInGameInfoCollector().clearInfo();
                invocation.source().sendMessage(
                        Component.text("已清除所有聊天记录缓存。", NamedTextColor.GREEN));
            }
            case "mute" -> {
                YoumuCommandUtil.handleMute(invocation, plugin);
            }
            case "setmodel" -> {
                if (!invocation.source().hasPermission("youmuchan.setmodel")) {
                    invocation.source().sendMessage(Component.text("没有权限执行此命令。", NamedTextColor.RED));
                    return;
                }
                if (args.length < 3) {
                    invocation.source()
                            .sendMessage(Component.text("用法: /youmu setmodel <border|youmu> <model_name>",
                                    NamedTextColor.RED));
                    return;
                }
                if (args[1].equalsIgnoreCase("border")) {
                    String modelName = args[2];
                    if (plugin.setBorderModel(modelName)) {
                        invocation.source()
                                .sendMessage(Component.text("边界分析模型已更新为: " + modelName, NamedTextColor.GREEN));
                    } else {
                        invocation.source().sendMessage(Component.text("更新失败，请检查控制台。", NamedTextColor.RED));
                    }
                } else if (args[1].equalsIgnoreCase("youmu")) {
                    String modelName = args[2];
                    if (plugin.setYoumuModel(modelName)) {
                        invocation.source()
                                .sendMessage(Component.text("妖梦对话模型已更新为: " + modelName, NamedTextColor.GREEN));
                    } else {
                        invocation.source().sendMessage(Component.text("更新失败，请检查控制台。", NamedTextColor.RED));
                    }
                } else {
                    invocation.source().sendMessage(Component.text("未知子命令，目前支持: border, youmu", NamedTextColor.RED));
                }
            }
            case "setkey" -> {
                if (!invocation.source().hasPermission("youmuchan.setkey")) {
                    invocation.source().sendMessage(Component.text("没有权限执行此命令。", NamedTextColor.RED));
                    return;
                }
                if (args.length < 2) {
                    invocation.source().sendMessage(Component.text("用法: /youmu setkey <api_key>", NamedTextColor.RED));
                    return;
                }
                String apiKey = args[1];
                if (plugin.setApiKey(apiKey)) {
                    invocation.source().sendMessage(Component.text("API Key 已更新。", NamedTextColor.GREEN));
                } else {
                    invocation.source().sendMessage(Component.text("更新失败，请检查控制台。", NamedTextColor.RED));
                }
            }
            case "seturl" -> {
                if (!invocation.source().hasPermission("youmuchan.seturl")) {
                    invocation.source().sendMessage(Component.text("没有权限执行此命令。", NamedTextColor.RED));
                    return;
                }
                if (args.length < 2) {
                    invocation.source().sendMessage(Component.text("用法: /youmu seturl <api_url>", NamedTextColor.RED));
                    return;
                }
                String apiUrl = args[1];
                if (plugin.setApiUrl(apiUrl)) {
                    invocation.source().sendMessage(Component.text("API URL 已更新。", NamedTextColor.GREEN));
                } else {
                    invocation.source().sendMessage(Component.text("更新失败，请检查控制台。", NamedTextColor.RED));
                }
            }
            default -> {
                invocation.source().sendMessage(Component.text("未知子命令。", NamedTextColor.RED));
                YoumuCommandUtil.sendHelpMessage(invocation);
            }
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return YoumuCommandUtil.suggestAsync(invocation, plugin);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }
}
