package org.allivilsey.youmuchan;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

// 处理 /youmu 命令，支持子命令 reload / start / stop。
public class YoumuCommand implements SimpleCommand {

    private final YoumuChan plugin;

    public YoumuCommand(YoumuChan plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            sendHelpMessage(invocation);
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
                long duration = parseDuration(args);
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
                long duration = parseDuration(args);
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
                    if (invocation.source() instanceof com.velocitypowered.api.proxy.Player player) {
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
            default -> {
                invocation.source().sendMessage(Component.text("未知子命令。", NamedTextColor.RED));
                sendHelpMessage(invocation);
            }
        }
    }

    private void sendHelpMessage(Invocation invocation) {
        invocation.source().sendMessage(Component.text("YoumuChan 命令列表:", NamedTextColor.YELLOW));
        if (invocation.source().hasPermission("youmuchan.reload")) {
            invocation.source().sendMessage(Component.text(" - /youmu reload : 重载配置", NamedTextColor.YELLOW));
        }
        if (invocation.source().hasPermission("youmuchan.start")) {
            invocation.source()
                    .sendMessage(Component.text(" - /youmu start [时间] : 切换至 DREAM 状态", NamedTextColor.YELLOW));
        }
        if (invocation.source().hasPermission("youmuchan.stop")) {
            invocation.source()
                    .sendMessage(Component.text(" - /youmu stop [时间] : 切换至 SLEEP 状态", NamedTextColor.YELLOW));
        }
        if (invocation.source().hasPermission("youmuchan.debug")) {
            invocation.source()
                    .sendMessage(Component.text(" - /youmu debug : 切换 Debug 模式", NamedTextColor.YELLOW));
        }
    }

    // 解析可选的持续时间参数（args[1]），支持单位 h, m, s，默认认为是 ms。
    // 不存在或无法解析时返回 -1（表示无效或永久锁定）。
    private long parseDuration(String[] args) {
        if (args.length < 2) {
            return -1L;
        }

        String input = args[1].toLowerCase();
        try {
            if (input.endsWith("h")) {
                long val = Long.parseLong(input.substring(0, input.length() - 1));
                return val > 0 ? val * 60 * 60 * 1000 : -1L;
            } else if (input.endsWith("m")) {
                long val = Long.parseLong(input.substring(0, input.length() - 1));
                return val > 0 ? val * 60 * 1000 : -1L;
            } else if (input.endsWith("s")) {
                long val = Long.parseLong(input.substring(0, input.length() - 1));
                return val > 0 ? val * 1000 : -1L;
            } else {
                long val = Long.parseLong(input);
                return val > 0 ? val : -1L;
            }
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            java.util.List<String> suggestions = new java.util.ArrayList<>();
            if (invocation.source().hasPermission("youmuchan.reload"))
                suggestions.add("reload");
            if (invocation.source().hasPermission("youmuchan.start"))
                suggestions.add("start");
            if (invocation.source().hasPermission("youmuchan.stop"))
                suggestions.add("stop");
            if (invocation.source().hasPermission("youmuchan.debug"))
                suggestions.add("debug");
            return CompletableFuture.completedFuture(suggestions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            if (invocation.source().hasPermission("youmuchan.debug")) {
                return CompletableFuture.completedFuture(List.of("info"));
            }
        }
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("youmuchan.reload") ||
                invocation.source().hasPermission("youmuchan.start") ||
                invocation.source().hasPermission("youmuchan.stop") ||
                invocation.source().hasPermission("youmuchan.debug");
    }
}
