package org.allivilsey.youmuchan;

import java.util.stream.Collectors;

//处理聊天模型提示词
public class AIYoumuPromptFormatter implements PromptFormatter{

    @Override
    public void format(AIContext context) {

        //设置系统提示词
        String systemPrompt = """
                You are generating in game dialogue as Konpaku Youmu for Minecraft Tianji Server.
                Your Emotion: %s
                """.formatted(context.getEmotion());

        //格式化数据
        String chatContext = context.getFilteredInfos()
                .stream()
                .map(info -> info.getPlayerName() + ": " + info.getContent())
                .collect(Collectors.joining("\n"));

        //设置用户提示词
        String userPrompt = """
                In game log: 
                %s
                """.formatted(chatContext);

        //设置系统/用户提示词
        context.setSystemPrompt(systemPrompt);
        context.setUserPrompt(userPrompt);
    }
}
