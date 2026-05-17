package com.community.health.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiChatRequest {
    private List<AiMessage> messages;
}
