package com.community.health.service;

import com.community.health.dto.AiMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    @Value("${ai.api-url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:gpt-3.5-turbo}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public String chat(List<AiMessage> messages) {
        if (apiKey == null || apiKey.isEmpty() || "your_api_key_here".equals(apiKey)) {
            return "AI助手未配置API Key，请联系管理员在后端 application.yml 中配置 ai.api-key";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // 确保有 system prompt 来设定角色
        boolean hasSystem = messages.stream().anyMatch(m -> "system".equals(m.getRole()));
        if (!hasSystem) {
            messages.add(0, new AiMessage("system", "你是一个专业的社区健康档案管理系统的AI健康助手。请主要为社区居民提供健康咨询、日常护理建议、慢性病管理指导等服务。请使用友善、专业的语气回答。如果用户的问题与健康无关，请委婉地引导回健康话题。"));
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            return "AI助手暂时不可用，请检查网络或配置。(" + e.getMessage() + ")";
        }
        return "抱歉，我没有理解您的问题，请换种方式提问。";
    }
}
