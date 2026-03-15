package org.allivilsey.youmuchan.paper;

public class FuelInfo {

    private final double fuel;

    private final String playerName;

    public FuelInfo(double fuel, String playerName) {
        this.fuel = fuel;
        this.playerName = playerName;
    }

    public double getFuel() {
        return fuel;
    }

    public String getFuelPlayerName() {
        return playerName;
    }
}
