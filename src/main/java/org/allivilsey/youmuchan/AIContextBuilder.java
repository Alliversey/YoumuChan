package org.allivilsey.youmuchan;

import java.util.List;
import java.util.stream.Collectors;

// 按当前热度、目标玩家与心智状态构建一次完整的 AI 请求上下文。
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

    // 构建上下文：
    // 1) 按热度动态扩大/缩小时间窗口；
    // 2) 先保留原始信息，再根据状态过滤为模型输入信息。
    public AIContext build(String targetPlayer, MentalState state) {
        AIContext context = new AIContext(defaultModel, defaultTemperature);
        context.setTargetPlayer(targetPlayer);

        // 热度越高，窗口越大，可见上下文越多。
        long dynamicWindow = (long) (timeWindowMillis / 2.0 + timeWindowMillis * heatController.getHeat());

        List<InGameInfo> rawInfos = collector.getInfoByTime(dynamicWindow);

        context.setRawInfos(rawInfos);

        List<InGameInfo> filtered = filerByState(rawInfos, state);

        context.setFilteredInfos(filtered);

        return context;
    }

    // 根据心智状态裁剪信息范围，控制模型注意力与 token 消耗。
    private List<InGameInfo> filerByState(List<InGameInfo> raw, MentalState state) {
        if (state == MentalState.DREAM) {
            return raw.stream().filter(info -> info.getInfoType() == InfoType.SERVER_EVENT || info.getInfoType() == InfoType.PLAYER_EVENT).collect(Collectors.toList());
        }
        return raw;
    }
}
