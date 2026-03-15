package org.allivilsey.youmuchan;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.util.Tristate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class YoumuCommandUtil {

    private YoumuCommandUtil() {
    }

    public static void sendHelpMessage(SimpleCommand.Invocation invocation) {
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
        if (invocation.source().hasPermission("youmuchan.clear")) {
            invocation.source()
                    .sendMessage(Component.text(" - /youmu clear : 清除聊天记录缓存", NamedTextColor.YELLOW));
        }
        invocation.source()
                .sendMessage(Component.text(" - /youmu mute : 开关自己的 AI 发言屏蔽", NamedTextColor.YELLOW));
        if (invocation.source().hasPermission("youmu.muteplayer")) {
            invocation.source()
                    .sendMessage(Component.text(" - /youmu mute <玩家ID> : 开关指定玩家的 AI 发言屏蔽", NamedTextColor.YELLOW));
        }
        if (invocation.source().hasPermission("youmuchan.setmodel")) {
            invocation.source()
                    .sendMessage(
                            Component.text(" - /youmu setmodel border <model_name> : 设置边界分析模型", NamedTextColor.YELLOW));
            invocation.source()
                    .sendMessage(
                            Component.text(" - /youmu setmodel youmu <model_name> : 设置妖梦对话模型", NamedTextColor.YELLOW));
        }
        if (invocation.source().hasPermission("youmuchan.setkey")) {
            invocation.source()
                    .sendMessage(Component.text(" - /youmu setkey <api_key> : 设置 API Key", NamedTextColor.YELLOW));
        }
        if (invocation.source().hasPermission("youmuchan.seturl")) {
            invocation.source()
                    .sendMessage(Component.text(" - /youmu seturl <api_url> : 设置 API URL", NamedTextColor.YELLOW));
        }
    }

    // 解析可选的持续时间参数（args[1]），支持单位 h, m, s，默认认为是 ms。
    // 不存在或无法解析时返回 -1（表示无效或永久锁定）。
    public static long parseDuration(String[] args) {
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

    public static void handleMute(SimpleCommand.Invocation invocation, YoumuChan plugin) {
        String[] args = invocation.arguments();
        CommandSource source = invocation.source();
        LuckPerms luckPerms = plugin.getLuckPerms();

        if (luckPerms == null) {
            source.sendMessage(Component.text("LuckPerms 未就绪，无法切换权限。", NamedTextColor.RED));
            return;
        }

        if (args.length >= 2) {
            if (!source.hasPermission("youmu.muteplayer")) {
                source.sendMessage(Component.text("没有权限执行此命令。", NamedTextColor.RED));
                return;
            }
            String targetInput = args[1];
            resolveMuteTarget(plugin, luckPerms, targetInput).thenAccept(target -> {
                if (target == null) {
                    source.sendMessage(Component.text("未找到玩家: " + targetInput, NamedTextColor.RED));
                    return;
                }
                toggleVisibility(plugin, luckPerms, source, target.uniqueId(), target.displayName(), false);
            }).exceptionally(ex -> {
                plugin.getLogger().error("获取玩家信息失败", ex);
                source.sendMessage(Component.text("获取玩家信息失败，请查看控制台。", NamedTextColor.RED));
                return null;
            });
            return;
        }

        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("只有玩家可以使用此命令。", NamedTextColor.RED));
            return;
        }

        toggleVisibility(plugin, luckPerms, source, player.getUniqueId(), player.getUsername(), true);
    }

    private static CompletableFuture<MuteTarget> resolveMuteTarget(YoumuChan plugin, LuckPerms luckPerms, String input) {
        Optional<Player> online = plugin.getProxyServer().getPlayer(input);
        if (online.isPresent()) {
            Player player = online.get();
            return CompletableFuture.completedFuture(new MuteTarget(player.getUniqueId(), player.getUsername()));
        }
        try {
            UUID uuid = UUID.fromString(input);
            return CompletableFuture.completedFuture(new MuteTarget(uuid, input));
        } catch (IllegalArgumentException e) {
            return luckPerms.getUserManager().lookupUniqueId(input)
                    .thenApply(uuid -> uuid == null ? null : new MuteTarget(uuid, input));
        }
    }

    private static void toggleVisibility(YoumuChan plugin, LuckPerms luckPerms, CommandSource source, UUID uuid, String displayName, boolean self) {

        String VISIBILITY_PERMISSION = "youmu.visible";

        luckPerms.getUserManager().loadUser(uuid).thenAccept(user -> {
            if (user == null) {
                source.sendMessage(Component.text("玩家数据加载失败。", NamedTextColor.RED));
                return;
            }
            Tristate state = user.getCachedData().getPermissionData().checkPermission(VISIBILITY_PERMISSION);
            boolean visible = state != Tristate.FALSE;
            boolean nextVisible = !visible;
            PermissionNode allowNode = PermissionNode.builder(VISIBILITY_PERMISSION).value(true).build();
            PermissionNode denyNode = PermissionNode.builder(VISIBILITY_PERMISSION).value(false).build();
            user.data().remove(allowNode);
            user.data().remove(denyNode);
            user.data().add(nextVisible ? allowNode : denyNode);
            luckPerms.getUserManager().saveUser(user);
            if (self) {
                source.sendMessage(Component.text(
                        nextVisible ? "已开启你的 AI 发言显示。" : "已关闭你的 AI 发言显示。",
                        NamedTextColor.GREEN));
            } else {
                source.sendMessage(Component.text(
                        "已为玩家 " + displayName + (nextVisible ? " 开启" : " 关闭") + " AI 发言显示。",
                        NamedTextColor.GREEN));
            }
        }).exceptionally(ex -> {
            plugin.getLogger().error("切换 youmu.visible 失败", ex);
            source.sendMessage(Component.text("权限切换失败，请查看控制台。", NamedTextColor.RED));
            return null;
        });
    }

    public static CompletableFuture<List<String>> suggestAsync(SimpleCommand.Invocation invocation, YoumuChan plugin) {
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
            if (invocation.source().hasPermission("youmuchan.clear"))
                suggestions.add("clear");
            if (invocation.source().hasPermission("youmuchan.setmodel"))
                suggestions.add("setmodel");
            if (invocation.source().hasPermission("youmuchan.setkey"))
                suggestions.add("setkey");
            if (invocation.source().hasPermission("youmuchan.seturl"))
                suggestions.add("seturl");
            suggestions.add("mute");
            return CompletableFuture.completedFuture(suggestions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            if (invocation.source().hasPermission("youmuchan.debug")) {
                return CompletableFuture.completedFuture(List.of("info"));
            }

        } else if (args.length == 2 && args[0].equalsIgnoreCase("mute")) {
            if (invocation.source().hasPermission("youmu.muteplayer")) {
                java.util.List<String> suggestions = new java.util.ArrayList<>();
                for (Player player : plugin.getProxyServer().getAllPlayers()) {
                    suggestions.add(player.getUsername());
                }
                return CompletableFuture.completedFuture(suggestions);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setmodel")) {
            if (invocation.source().hasPermission("youmuchan.setmodel")) {
                return CompletableFuture.completedFuture(List.of("border", "youmu"));
            }
        }
        return CompletableFuture.completedFuture(List.of());
    }

    private record MuteTarget(UUID uniqueId, String displayName) {
    }
}

