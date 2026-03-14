package org.allivilsey.youmuchan.paper;

public class FocusInfo {

    private final String playerName;

    private final double focus;

    public FocusInfo(String playerName, double focus) {
        this.playerName = playerName;
        this.focus = focus;
    }

    public String getFocusedPlayerName() {
        return playerName;
    }

    public double getFocus() {
        return focus;
    }
}
