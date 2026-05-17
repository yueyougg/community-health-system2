package com.community.health.controller;

import com.community.health.common.ApiResponse;
import com.community.health.dto.AiChatRequest;
import com.community.health.service.AiService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('RESIDENT', 'DOCTOR', 'ADMIN', 'PUBLIC_HEALTH_MANAGER')")
    public ApiResponse<String> chat(@RequestBody AiChatRequest request) {
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            return ApiResponse.fail("消息不能为空");
        }
        String reply = aiService.chat(request.getMessages());
        return ApiResponse.ok("success", reply);
    }
}
