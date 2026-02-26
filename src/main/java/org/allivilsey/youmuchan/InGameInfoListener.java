package org.allivilsey.youmuchan;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;

public class InGameInfoListener {
    private final InGameInfoCollector inGameInfoCollector;
    private final HeatController heatController;

    public InGameInfoListener(
            InGameInfoCollector inGameInfoCollector,
            HeatController heatController) {
        this.inGameInfoCollector = inGameInfoCollector;
        this.heatController = heatController;
    }

    //监听聊天事件
    @Subscribe
    public void onChat(PlayerChatEvent event) {
        String playerName = event.getPlayer().getUsername();
        String serverName = event.getPlayer()
                .getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse("Unknown");
        String content = event.getMessage();

        InGameInfo info = new InGameInfo(InfoType.CHAT, playerName, serverName, content);

        inGameInfoCollector.addInfo(info);
    }

    //监听玩家登录
    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        String playerName = event.getPlayer().getUsername();

        InGameInfo info = new InGameInfo(
                InfoType.PLAYER_EVENT,
                playerName,
                null,
                "加入服务器"
        );

        inGameInfoCollector.addInfo(info);
    }

    //监听玩家登出
    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
        String playerName = event.getPlayer().getUsername();

        InGameInfo info = new InGameInfo(
                InfoType.PLAYER_EVENT,
                playerName,
                null,
                "离开服务器"
        );

        inGameInfoCollector.addInfo(info);
    }
}
