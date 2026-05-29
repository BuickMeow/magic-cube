package com.yuwen.magiccube.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import okhttp3.OkHttpClient;

@Configuration
public class DeepSeekConfig {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    // 🌟 新增：动态读取配置文件中的模型名称
    @Value("${deepseek.api.model:deepseek-r1}")
    private String model;

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    // 🌟 新增 Getter 方法
    public String getModel() {
        return model;
    }
}