package org.allivilsey.youmuchan;

// 负责将 AIContext 转换为模型可消费的 system/user 提示词。
public interface PromptFormatter {

    // 就地修改 context 的 systemPrompt 与 userPrompt 字段。
    void format(AIContext context);

}
