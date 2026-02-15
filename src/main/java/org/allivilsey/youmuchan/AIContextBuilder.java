package org.allivilsey.youmuchan;

public class AIContextBuilder {

    private final InGameInfoCollector collector;

    private final String defaultModel;
    private final float defaultTemperature;

    private final long timeWindowMillis;

    public AIContextBuilder(InGameInfoCollector collector,
                            String defaultModel,
                            float defaultTemperature,
                            long timeWindowMillis) {
        this.collector = collector;
        this.defaultModel = defaultModel;
        this.defaultTemperature = defaultTemperature;
        this.timeWindowMillis = timeWindowMillis;
    }


}
