package org.allivilsey.youmuchan;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;

public class FocusControllerListener {
    private final FocusController focusController;

    public FocusControllerListener(FocusController focusController) {
        this.focusController = focusController;
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        focusController.addFocus(event.getPlayer().getUsername(), 0.5);
    }

    @Subscribe
    public void playerLogin(PostLoginEvent event) {
        focusController.addFocus(event.getPlayer().getUsername(), 1.0);
    }

    @Subscribe
    public void mentionedName(PlayerChatEvent event) {
        if (event.getMessage().contains("妖梦")) {
            focusController.addFocus(event.getPlayer().getUsername(), 3.0);
        }
    }
}
