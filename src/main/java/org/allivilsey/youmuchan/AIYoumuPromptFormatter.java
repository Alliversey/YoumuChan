package org.allivilsey.youmuchan;

import java.util.stream.Collectors;

//处理聊天模型提示词
public class AIYoumuPromptFormatter implements PromptFormatter{

    @Override
    public void format(AIContext context) {

        String systemPrompt = """
                You are generating in game dialogue as Konpaku Youmu for Minecraft Tianji Server.
                Your Emotion: %s
                """.formatted(context.getEmotion());

        String chatContext = context.getFilteredInfos()
                .stream()
                .map(info -> info.getPlayerName() + ": " + info.getContent())
                .collect(Collectors.joining("\n"));

        String userPrompt = """
                In game log: 
                %s
                """.formatted(chatContext);

        context.setSystemPrompt(systemPrompt);
        context.setUserPrompt(userPrompt);
    }
}
