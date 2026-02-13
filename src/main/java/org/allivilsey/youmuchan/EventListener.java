package org.allivilsey.youmuchan;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;

public class EventListener {
    private final ChatMessageCollector chatMessageCollector;

    public EventListener(ChatMessageCollector chatMessageCollector) {
        this.chatMessageCollector = chatMessageCollector;
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        String playerName = event.getPlayer().getUsername();
        String serverName = event.getPlayer()
                .getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse("Unknown");
        String content = event.getMessage();

        ChatMessage message = new ChatMessage(playerName, serverName, content);

        chatMessageCollector.addMessage(message);
    }
}
