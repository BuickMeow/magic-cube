package com.yuwen.magiccube.service.impl;

import com.yuwen.magiccube.config.DeepSeekConfig;
import com.yuwen.magiccube.service.DeepSeekService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
public class DeepSeekServiceImpl implements DeepSeekService {

    @Autowired
    private DeepSeekConfig deepSeekConfig;

    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build();

    private final Executor asyncExecutor = Executors.newFixedThreadPool(10);

    @Override
    public String generateSuggestion(Integer score, String interventionStatus, String userName) {
        try {
            // 构建提示词
            String prompt = buildPrompt(score, interventionStatus, userName);

            // 调用 DeepSeek API
            String response = callDeepSeekAPI(prompt);

            // 解析响应
            return extractContent(response);

        } catch (Exception e) {
            e.printStackTrace();
            return getFallbackSuggestion(score, interventionStatus, userName);
        }
    }

    @Override
    public CompletableFuture<String> generateSuggestionAsync(Integer score, String interventionStatus, String userName) {
        return CompletableFuture.supplyAsync(() -> {
            return generateSuggestion(score, interventionStatus, userName);
        }, asyncExecutor);
    }

    @Override
    public String analyzePsychologicalState(Integer score, String interventionStatus,
                                           String userName, String gender, Integer age) {
        try {
            // 构建更详细的分析提示词
            String prompt = buildDetailedPrompt(score, interventionStatus, userName, gender, age);

            // 调用 DeepSeek API
            String response = callDeepSeekAPI(prompt);

            return extractContent(response);

        } catch (Exception e) {
            e.printStackTrace();
            return "暂时无法提供详细分析，请稍后重试。";
        }
    }

    /**
     * 构建基础提示词
     */
    private String buildPrompt(Integer score, String interventionStatus, String userName) {
        String statusText = "PRE_INTERVENTION".equals(interventionStatus) ? "干预前" : "干预后";

        return "你是一位专业的心理咨询师。请根据以下信息，生成一段温暖、专业、鼓励性的心理建议：\n\n" +
               "学生姓名：" + (userName != null ? userName : "同学") + "\n" +
               "心理弹性得分：" + score + "分（满分 100 分）\n" +
               "测试阶段：" + statusText + "\n\n" +
               "评分标准：\n" +
               "- 低于 55 分：心理弹性较低，需要关注和支持\n" +
               "- 55-83 分：心理弹性中等，属于正常范围\n" +
               "- 高于 83 分：心理弹性良好，抗压能力强\n\n" +
               "要求：\n" +
               "1. 语气温暖、专业、富有同理心\n" +
               "2. 结合得分给出具体建议\n" +
               "3. 包含鼓励和肯定的话语\n" +
               "4. 字数控制在 100-200 字之间\n" +
               "5. 避免使用过于专业的术语，让学生容易理解";
    }

    /**
     * 构建详细分析提示词
     */
    private String buildDetailedPrompt(Integer score, String interventionStatus,
                                       String userName, String gender, Integer age) {
        String statusText = "PRE_INTERVENTION".equals(interventionStatus) ? "干预前" : "干预后";
        String genderText = "male".equals(gender) ? "男" : "女";

        return "你是一位资深的青少年心理咨询师。请根据以下信息，生成一份详细的心理状态分析报告：\n\n" +
               "基本信息：\n" +
               "- 姓名：" + (userName != null ? userName : "同学") + "\n" +
               "- 性别：" + genderText + "\n" +
               "- 年龄：" + (age != null ? age : "未知") + "岁\n" +
               "- 测试阶段：" + statusText + "\n" +
               "- 心理弹性得分：" + score + "分（满分 100 分）\n\n" +
               "请从以下角度分析：\n" +
               "1. 当前心理状态评估\n" +
               "2. 可能的压力源分析\n" +
               "3. 应对策略建议\n" +
               "4. 日常自我调节方法\n" +
               "5. 何时需要寻求专业帮助\n\n" +
               "要求：\n" +
               "- 专业但不失温暖\n" +
               "- 具体可操作的建议\n" +
               "- 字数 300-500 字";
    }

    /**
     * 调用 DeepSeek API（已适配百度千帆）
     */


    /**
     * 调用 DeepSeek API（已完美适配百度千帆 V2）
     */
    private String callDeepSeekAPI(String prompt) throws IOException {
        JSONObject requestBody = new JSONObject();

        // 🌟 修复 1：动态读取模型，彻底告别写死的 ERNIE
        requestBody.put("model", deepSeekConfig.getModel().trim());

        JSONArray messages = new JSONArray();
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一位专业的心理咨询师，擅长青少年心理健康辅导。");
        messages.put(systemMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.put(userMsg);

        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_output_tokens", 1000);

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                requestBody.toString()
        );

        // 🌟 修复 2：直接使用 API Key，并使用 trim() 去除配置文件可能带入的隐藏空格
        String apiKey = deepSeekConfig.getApiKey().trim();
        String apiUrl = deepSeekConfig.getApiUrl().trim();

        Request request = new Request.Builder()
                .url(apiUrl)
                // 标准的 Bearer 认证格式，确保中间只有一个空格
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // 🌟 修复 3：如果调用失败，打印出具体的服务器报错详情，方便排查
                String errorBody = response.body() != null ? response.body().string() : "无返回体";
                throw new IOException("API 调用失败，HTTP 状态码：" + response.code() + "，报错详情：" + errorBody);
            }

            return response.body().string();
        }
    }
    /**
     * 提取 API 响应内容
     */
    private String extractContent(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            JSONObject choices = json.getJSONArray("choices").getJSONObject(0);
            JSONObject message = choices.getJSONObject("message");
            return message.getString("content");
        } catch (Exception e) {
            e.printStackTrace();
            return "抱歉，暂时无法生成建议。";
        }
    }

    /**
     * 降级方案（API 失败时使用）
     */
    private String getFallbackSuggestion(Integer score, String interventionStatus, String userName) {
        StringBuilder suggestion = new StringBuilder();

        if (score >= 80) {
            suggestion.append("亲爱的").append(userName != null ? userName : "同学").append("，你的心理弹性得分非常优秀！");
            suggestion.append("这表明你具有很强的抗压能力和情绪调节能力。继续保持积极乐观的心态，相信你能应对各种挑战！");
        } else if (score >= 60) {
            suggestion.append("亲爱的").append(userName != null ? userName : "同学").append("，你的心理弹性处于良好水平。");
            suggestion.append("面对压力时，你可以尝试更多放松技巧，如深呼吸、冥想等。记住，适度的压力有助于成长，但也要学会适时放松。");
        } else {
            suggestion.append("亲爱的").append(userName != null ? userName : "同学").append("，最近可能遇到了一些挑战。");
            suggestion.append("请记住，寻求帮助是勇敢的表现。可以和信任的老师、家长或心理咨询师聊聊，他们会给你专业的支持和指导。");
        }

        return suggestion.toString();
    }
}
