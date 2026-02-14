package org.allivilsey.youmuchan;

public class AIContext {

    private final String model;
    private final Float temperature;

    private String systemPrompt;
    private String userPrompt;

    public AIContext(String model, Float temperature) {
        this.model = model;
        this.temperature = temperature;
    }

    public String getModel() {
        return model;
    }

    public Float getTemperature() {
        return temperature;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }
}
