package org.allivilsey.youmuchan;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// 解析边界模型返回的 JSON，并将结构化判断结果写回 AIContext。
public class AIBorderResultParser {

    // 约定输入 json 至少包含 injection/emotion_params/wiki 三个字段。
    // 本方法不做兜底修复，字段缺失将由上层异常处理链统一处理。
    public static void applyResult(String json, AIContext context) {
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();

        // 是否存在提示词注入风险，默认 false
        context.setInjectionRisk(getBooleanSafe(object, "injection"));

        // 解析情绪参数
        AIContext.EmotionParams params = new AIContext.EmotionParams();
        if (object.has("emotion_params") && object.get("emotion_params").isJsonObject()) {
            JsonObject emotionParams = object.getAsJsonObject("emotion_params");
            params.valence = getFloatSafe(emotionParams, "valence");
            params.arousal = getFloatSafe(emotionParams, "arousal");
            params.dominance = getFloatSafe(emotionParams, "dominance");
            params.sarcasm = getFloatSafe(emotionParams, "sarcasm");
            params.politeness = getFloatSafe(emotionParams, "politeness");
            params.verbosity = getFloatSafe(emotionParams, "verbosity");
        }
        context.setEmotionParams(params);

        // 是否需要补充 wiki 类知识，默认 false
        context.setWikiRequired(getBooleanSafe(object, "wiki"));
    }

    private static boolean getBooleanSafe(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsBoolean();
        }
        return false;
    }

    private static float getFloatSafe(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsFloat();
        }
        return 0.0f;
    }
}
