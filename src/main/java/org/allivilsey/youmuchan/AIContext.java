package org.allivilsey.youmuchan;

import java.util.ArrayList;
import java.util.List;

public class AIContext {

    public static class EmotionParams {
        public float valence = 0.0f;
        public float arousal = 0.0f;
        public float dominance = 0.0f;
        public float sarcasm = 0.0f;
        public float politeness = 0.0f;
        public float verbosity = 0.0f;
    }

    //模型名称
    private String model;
    //模型温度
    private Float temperature;
    //本次请求创建时间
    private long requestTime = System.currentTimeMillis();

    //系统|用户提示词
    private String systemPrompt;
    private String userPrompt;

    //未处理原始游戏数据
    private List<InGameInfo> rawInfos = new ArrayList<>();
    //已过滤游戏数据
    private List<InGameInfo> filteredInfos = new ArrayList<>();

    //模型检测是否为注入攻击（默认否）
    private boolean injectionRisk = false;
    //模型判断应对情绪参数
    private EmotionParams emotionParams = new EmotionParams();
    //模型判断wiki使用需求
    private boolean wikiRequired = false;

    //目标玩家
    private String targetPlayer;

    public AIContext(String model, Float temperature) {
        this.model = model;
        this.temperature = temperature;
    }

    //GETTER
    public String getModel() {
        return model;
    }

    public Float getTemperature() {
        return temperature;
    }

    public long getRequestTime() {
        return requestTime;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public List<InGameInfo> getRawInfos() {
        return rawInfos;
    }

    public List<InGameInfo> getFilteredInfos() {
        return filteredInfos;
    }

    public boolean isInjectionRisk() {
        return injectionRisk;
    }

    public EmotionParams getEmotionParams() {
        return emotionParams;
    }

    public boolean isWikiRequired() {
        return wikiRequired;
    }

    public String getTargetPlayer() {
        return targetPlayer;
    }

    //SETTER
    public void setRawInfos(List<InGameInfo> rawInfos) {
        this.rawInfos = rawInfos;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }

    public void setFilteredInfos(List<InGameInfo> filteredInfos) {
        this.filteredInfos = filteredInfos;
    }

    public void setInjectionRisk(boolean injectionRisk) {
        this.injectionRisk = injectionRisk;
    }

    public void setEmotionParams(EmotionParams emotionParams) {
        this.emotionParams = emotionParams;
    }

    public void setWikiRequired(boolean wikiRequired) {
        this.wikiRequired = wikiRequired;
    }

    public void setTargetPlayer(String targetPlayer) {
        this.targetPlayer = targetPlayer;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setTemperature(Float temperature) {
        this.temperature = temperature;
    }
}
