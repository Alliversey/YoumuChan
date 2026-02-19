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

    // Builder -> BorderFormatter -> 小模型 -> 更新Context -> YoumuFormatter -> 主模型
    public String pass(String targetPlayer, MentalState mentalState) throws IOException {
        AIContext context = contextBuilder.build(targetPlayer, mentalState);

        // 小模型：边界分析
        context.setModel(borderModel);
        context.setTemperature(borderTemperature);
        borderFormatter.format(context);
        String borderReply = apiProcessor.sendToApi(context);
        AIBorderResultParser.applyResult(borderReply, context);

        // 主模型：最终回复
        context.setModel(mainModel);
        context.setTemperature(mainTemperature);
        youmuFormatter.format(context);
        return apiProcessor.sendToApi(context);
    }
}
