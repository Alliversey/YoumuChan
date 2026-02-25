package org.allivilsey.youmuchan;

public class HeatController {

    private double fuel = 0.0;

    private long lastUpdateTime;

    private final double lambda;

    public HeatController(double halfLifeSeconds) {
        this.lambda = Math.log(2) / halfLifeSeconds;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public synchronized void addFuel(double f) {
        decay();
        fuel = fuel + f;
    }

    public synchronized double getHeat() {
        decay();
        return mapHeat();
    }

    private void decay() {
        long now = System.currentTimeMillis();
        double dt = (now - lastUpdateTime) / 1000.0;

        if (dt > 0) {
            fuel = fuel * Math.exp(-lambda * dt);
            lastUpdateTime = now;
        }
    }

    private double mapHeat() {
        return 2.0 / (1.0 + Math.exp(-0.5 * (fuel - 6.0)));
    }
}
