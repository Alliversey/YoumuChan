package org.allivilsey.youmuchan.paper;

public class FuelInfo {

    private final double fuel;

    private final String playerName;

    private final long timestamp;

    public FuelInfo(double fuel, String playerName) {
        this.fuel = fuel;
        this.playerName = playerName;
        timestamp = System.currentTimeMillis();
    }

    public double getFuel() {
        return fuel;
    }

    public String getFuelPlayerName() {
        return playerName;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

