package org.allivilsey.youmuchan;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// 解析边界模型返回的 JSON，并将结构化判断结果写回 AIContext。
public class AIBorderResultParser {

    // 约定输入 json 至少包含 injection/emotion/wiki 三个字段。
    // 本方法不做兜底修复，字段缺失将由上层异常处理链统一处理。
    public static void applyResult(String json, AIContext context) {

        JsonObject object = JsonParser.parseString(json).getAsJsonObject();

        // 是否存在提示词注入风险。
        context.setInjectionRisk(object.get("injection").getAsBoolean());

        // 推荐回复情绪（如 calm / excited 等）。
        context.setEmotion(object.get("emotion").getAsString());

        // 是否需要补充 wiki 类知识。
        context.setWikiRequired(object.get("wiki").getAsBoolean());
    }
}
