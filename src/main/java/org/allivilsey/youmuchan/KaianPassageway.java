package org.allivilsey.youmuchan;

import java.io.IOException;

public class KaianPassageway {

    private final AIContextBuilder contextBuilder;

    private final PromptFormatter borderFormatter;
    private final PromptFormatter youmuFormatter;

    private final ApiProcessor apiProcessor;

    private final String borderModel;
    private final Float borderTemperature;

    private final String mainModel;
    private final Float mainTemperature;

    public KaianPassageway(
            AIContextBuilder contextBuilder,
            PromptFormatter borderFormatter,
            PromptFormatter youmuFormatter,
            ApiProcessor apiProcessor,
            String borderModel,
            Float borderTemperature,
            String mainModel,
            Float mainTemperature
    ) {
        this.contextBuilder = contextBuilder;
        this.borderFormatter = borderFormatter;
        this.youmuFormatter = youmuFormatter;
        this.apiProcessor = apiProcessor;
        this.borderModel = borderModel;
        this.borderTemperature = borderTemperature;
        this.mainModel = mainModel;
        this.mainTemperature = mainTemperature;
    }

    // 执行两阶段模型链路：
    // 1) 先构建上下文并调用边界分析模型，提取注入风险、情绪和 wiki 使用需求并回写到 context；
    // 2) 再基于更新后的 context 调用主对话模型，生成最终发言内容。
    public String pass(String targetPlayer, MentalState mentalState) throws IOException {
        AIContext context = contextBuilder.build(targetPlayer, mentalState);

        // 阶段一：配置边界分析模型并格式化提示词，用于生成结构化控制信息。
        context.setModel(borderModel);
        context.setTemperature(borderTemperature);
        borderFormatter.format(context);
        String borderReply = apiProcessor.sendToApi(context);
        AIBorderResultParser.applyResult(borderReply, context);

        // 阶段二：切换到主模型并重新格式化提示词，输出可直接发送到游戏内的回复文本。
        context.setModel(mainModel);
        context.setTemperature(mainTemperature);
        youmuFormatter.format(context);
        String youmuReply = apiProcessor.sendToApi(context);
        return AIYoumuResultParser.applyResult(youmuReply);
    }
}
