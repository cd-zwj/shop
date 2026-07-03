package com.payment.rag.service.agent;

import com.payment.rag.model.dto.AgentMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentSessionModeService {

    private static final String MODE_KEY_PREFIX = "agent:session:mode:";
    private static final String ROUTE_KEY_PREFIX = "agent:session:route:";
    private static final Duration SESSION_MODE_TTL = Duration.ofHours(12);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final StringRedisTemplate redisTemplate;

    public Optional<AgentMode> getMode(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        try {
            String value = redisTemplate.opsForValue().get(modeKey(sessionId));
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(AgentMode.valueOf(value));
        } catch (Exception e) {
            log.warn("读取 agent 会话模式失败: sessionId={}, error={}", sessionId, e.getMessage());
            return Optional.empty();
        }
    }

    public void saveMode(String sessionId, AgentMode mode, AgentRouteDecision decision) {
        if (sessionId == null || sessionId.isBlank() || mode == null || mode == AgentMode.AUTO) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(modeKey(sessionId), mode.name(), SESSION_MODE_TTL);
            if (decision != null) {
                redisTemplate.opsForValue().set(routeKey(sessionId), MAPPER.writeValueAsString(decision), SESSION_MODE_TTL);
            }
        } catch (Exception e) {
            log.warn("保存 agent 会话模式失败: sessionId={}, mode={}, error={}", sessionId, mode, e.getMessage());
        }
    }

    private String modeKey(String sessionId) {
        return MODE_KEY_PREFIX + sessionId;
    }

    private String routeKey(String sessionId) {
        return ROUTE_KEY_PREFIX + sessionId;
    }
}
