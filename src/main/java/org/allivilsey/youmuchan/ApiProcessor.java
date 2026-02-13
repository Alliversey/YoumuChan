package org.allivilsey.youmuchan;

import com.google.gson.*;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ApiProcessor {

    //声明JSON类型
    private static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    //OkHttp客户端
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    //接口地址
    private static final String apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    //从配置文件导入APIKEY和模型名称
    private final String apiKey;
    public ApiProcessor(String apiKey) {
            this.apiKey = apiKey;
    }

    //发送用户消息，返回AI回复文本
    public String sendToApi(String model, String systemMessage, String userMessage, Float temperature) throws IOException {
        JsonObject root = new JsonObject();

        //指定模型
        root.addProperty("model", model);

        //构造消息组
        JsonArray messages = new JsonArray();

        //构造system message
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", systemMessage);
        messages.add(system);

        //构造user message
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userMessage);
        messages.add(user);

        //message加入根对象
        root.add("messages", messages);

        //温度加入根对象
        root.addProperty("temperature", temperature);

        //将JSON字符串包装为请求
        RequestBody body = RequestBody.create(root.toString(), JSON);

        //构造HTTP POST请求
        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        //发送并接收相应
        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException(
                        "API错误："
                                + response.code()
                                + " / "
                                + response.body().string()
                );
            }

            String responseBody = response.body().string();

            //解析并返回文本
            return parseReply(responseBody);
        }
    }

    //提取返回内容
    private String parseReply(String json) {

        JsonObject root = JsonParser
                .parseString(json)
                .getAsJsonObject();

        JsonArray choices = root.getAsJsonArray("choices");

        //防异常返回
        if (choices == null || choices.isEmpty()) {
            return "";
        }

        //读取第一个choice的message
        JsonObject message = choices
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("message");

        //返回AI回复文本
        return message.get("content").getAsString();
    }
}
