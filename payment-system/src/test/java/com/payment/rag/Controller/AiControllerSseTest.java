package com.payment.rag.Controller;

import com.payment.rag.model.dto.AiScenario;
import com.payment.rag.model.dto.MultiTurnChatRequest;
import com.payment.rag.service.AiService;
import com.payment.rag.service.AsrService;
import com.payment.rag.service.AuthContextService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiController SSE 异常处理")
class AiControllerSseTest {

    @Mock
    private AiService aiService;

    @Mock
    private AuthContextService authContextService;

    @Mock
    private AsrService asrService;

    @Test
    @DisplayName("scenario chat 前置异常时应返回 error 事件")
    void scenarioChatShouldReturnErrorEventWhenResolveUserFails() {
        AiController controller = new AiController(aiService, authContextService, asrService);
        when(authContextService.resolveUserId("")).thenThrow(new IllegalStateException("mock ai failure"));

        MultiTurnChatRequest request = new MultiTurnChatRequest();
        request.setUserId("");
        request.setSessionId("session-1");
        request.setMessage("你好");
        request.setScenario(AiScenario.USER_SHOPPING_ASSISTANT);

        List<ServerSentEvent<String>> events = controller.scenarioChat(request).collectList().block();

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().event()).isEqualTo("error");
        assertThat(events.getFirst().data()).isEqualTo("mock ai failure");
        verifyNoInteractions(aiService);
    }

    @Test
    @DisplayName("multi-turn chat service 异常时应转成 error 事件")
    void multiTurnChatShouldReturnErrorEventWhenServiceFails() {
        AiController controller = new AiController(aiService, authContextService, asrService);
        when(authContextService.resolveUserId("")).thenReturn("user-1");
        when(aiService.multiTurnChat(org.mockito.ArgumentMatchers.any(MultiTurnChatRequest.class)))
                .thenReturn(Flux.error(new IllegalStateException("service stream failed")));

        MultiTurnChatRequest request = new MultiTurnChatRequest();
        request.setUserId("");
        request.setSessionId("session-2");
        request.setMessage("怎么了");

        List<ServerSentEvent<String>> events = controller.multiTurnChat(request).collectList().block();

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().event()).isEqualTo("error");
        assertThat(events.getFirst().data()).isEqualTo("service stream failed");
    }
}
