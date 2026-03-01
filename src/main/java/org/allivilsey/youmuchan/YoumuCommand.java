package org.allivilsey.youmuchan;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

// 处理 /youmu 命令，目前支持子命令 reload。
public class YoumuCommand implements SimpleCommand {

    private final YoumuChan plugin;

    public YoumuCommand(YoumuChan plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            invocation.source().sendMessage(
                    Component.text("用法: /youmu reload", NamedTextColor.YELLOW));
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            invocation.source().sendMessage(
                    Component.text("YoumuChan 配置已重载。", NamedTextColor.GREEN));
        } else {
            invocation.source().sendMessage(
                    Component.text("未知子命令。用法: /youmu reload", NamedTextColor.RED));
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        if (invocation.arguments().length <= 1) {
            return CompletableFuture.completedFuture(List.of("reload"));
        }
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("youmuchan.reload");
    }
}
