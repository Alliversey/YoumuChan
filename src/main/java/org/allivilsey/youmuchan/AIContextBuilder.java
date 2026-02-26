package org.allivilsey.youmuchan;

import java.util.List;
import java.util.stream.Collectors;

public class AIContextBuilder {

    private final InGameInfoCollector collector;
    private final HeatController heatController;

    private final String defaultModel;
    private final float defaultTemperature;

    private final long timeWindowMillis;

    public AIContextBuilder(InGameInfoCollector collector, HeatController heatController ,String defaultModel, float defaultTemperature, long timeWindowMillis) {
        this.collector = collector;
        this.heatController = heatController;
        this.defaultModel = defaultModel;
        this.defaultTemperature = defaultTemperature;
        this.timeWindowMillis = timeWindowMillis;
    }

    //构建AIContext
    public AIContext build(String targetPlayer, MentalState state) {
        AIContext context = new AIContext(defaultModel, defaultTemperature);
        context.setTargetPlayer(targetPlayer);

        long dynamicWindow = (long) (timeWindowMillis * heatController.getHeat());

        List<InGameInfo> rawInfos = collector.getInfoByTime(dynamicWindow);

        context.setRawInfos(rawInfos);

        List<InGameInfo> filtered = filerByState(rawInfos, targetPlayer, state);

        context.setFilteredInfos(filtered);

        return context;
    }

    //根据心理状态过滤信息
    private List<InGameInfo> filerByState(List<InGameInfo> raw, String targetPlayer, MentalState state) {
        switch (state) {
            case SLEEP -> {
                return raw.stream()
                        .filter(info -> info.getInfoType() == InfoType.SERVER_EVENT || info.getInfoType() == InfoType.PLAYER_EVENT)
                        .collect(Collectors.toList());
            }
            case DREAM -> {
                if (targetPlayer != null) {
                    return raw.stream()
                            .filter(info -> targetPlayer.equalsIgnoreCase(info.getPlayerName()) || info.getInfoType() != InfoType.CHAT)
                            .collect(Collectors.toList());
                }
                return raw;
            }
            default -> {
                return raw;
            }
        }
    }
}
