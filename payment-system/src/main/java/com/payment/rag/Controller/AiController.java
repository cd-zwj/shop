package com.payment.rag.Controller;


import com.payment.rag.model.dto.*;
import com.payment.rag.service.AiService;
import com.payment.rag.service.AuthContextService;
import com.payment.rag.service.AsrService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;
import java.io.InputStream;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/rag/ai")
@RequiredArgsConstructor
@Tag(name = "AI 对话", description = "单轮/多轮对话、会话管理、语音识别")
public class AiController {
    private final AiService aiService;
    private final AuthContextService authContextService;
    private final AsrService asrService;

    @GetMapping(value = "/chatmemory/chat", produces = "text/plain;charset=UTF-8")
    public CompletableFuture<String> chat(String msg, String userId) {
        if (msg == null || msg.isBlank()) {
            return CompletableFuture.completedFuture("请输入问题");
        }
        if (msg.length() > 4000) {
            return CompletableFuture.completedFuture("问题过长，请缩短后重试");
        }
        return aiService.chat(msg, authContextService.resolveUserId(userId));
    }

    /**
     * 获取用户会话列表接口
     * 用于获取用户的所有会话
     */
    @PostMapping("/session/list")
    public ApiResponse<SessionListResponse> getUserSessions(@Valid @RequestBody SessionListRequest request) {
        request.setUserId(authContextService.resolveUserId(request.getUserId()));
        return aiService.getUserSessions(request);
    }

    /**
     * 删除会话接口
     * 用于删除指定的会话
     */
    @PostMapping("/session/delete")
    public ApiResponse<SessionDeleteResponse> deleteSession(@Valid @RequestBody SessionDeleteRequest request) {
        request.setUserId(authContextService.resolveUserId(request.getUserId()));
        return aiService.deleteSession(request);
    }

    /**
     * 手动触发画像提炼接口
     * 用于前端在用户关闭浏览器或切换会话时，主动将这段对话记忆进行结转
     */
    @PostMapping("/session/extract-profile")
    public ApiResponse<String> extractProfile(@Valid @RequestBody SessionDeleteRequest request) {
        request.setUserId(authContextService.resolveUserId(request.getUserId()));
        return aiService.extractProfile(request);
    }

    /**
     * 获取会话历史记录
     * 切换会话时提取该会话的历史对话在前端显示
     */
    @GetMapping("/session/history")
    public ApiResponse<List<java.util.Map<String, Object>>> getHistory(@RequestParam String sessionId) {
        return aiService.getHistory(authContextService.getCurrentUserId(), sessionId);
    }


    /**
     * 创建新会话接口
     * 用于用户打开新的对话窗口时获取会话ID
     */
    @PostMapping("/session/create")
    public ApiResponse<SessionCreateResponse> createSession(@Valid @RequestBody SessionCreateRequest request) {
        request.setUserId(authContextService.resolveUserId(request.getUserId()));
        return aiService.createSession(request);
    }

    /**
     * 新增多轮对话接口（流式输出）
     * 支持更丰富的上下文管理和会话控制，以 SSE 流式返回 token
     */
    @PostMapping(value = "/multi-turn/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> multiTurnChat(@Valid @RequestBody MultiTurnChatRequest request) {
        try {
            request.setUserId(authContextService.resolveUserId(request.getUserId()));
            return aiService.multiTurnChat(request)
                    .onErrorResume(this::toErrorEventStream);
        } catch (Exception e) {
            return toErrorEventStream(e);
        }
    }

    /**
     * 统一场景化 AI 对话接口。
     * 场景枚举负责提示词注入，动态 tools 根据当前使用者角色和权限筛选。
     */
    @PostMapping(value = "/scenario/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> scenarioChat(@Valid @RequestBody MultiTurnChatRequest request) {
        try {
            request.setUserId(authContextService.resolveUserId(request.getUserId()));
            request.setScenario(AiScenario.fromNullable(request.getScenario() != null ? request.getScenario().name() : null));
            return aiService.multiTurnChat(request)
                    .onErrorResume(this::toErrorEventStream);
        } catch (Exception e) {
            return toErrorEventStream(e);
        }
    }

    /**
     * 语音 ASR 接口
     * 接收前端录制的 PCM 音频，并返回识别出的文本。
     */
    @PostMapping(value = "/asr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> transcribe(@RequestParam("file") MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            String text = asrService.transcribe(in);
            return ApiResponse.success("识别成功", text);
        } catch (com.payment.rag.exception.AsrUnavailableException e) {
            log.warn("ASR 服务不可用: {}", e.getMessage());
            return ApiResponse.error(503, "语音识别服务暂不可用，请稍后重试");
        } catch (Exception e) {
            log.error("语音识别异常", e);
            return ApiResponse.error("语音识别失败，请稍后重试");
        }
    }

    private Flux<ServerSentEvent<String>> toErrorEventStream(Throwable throwable) {
        log.error("AI SSE 接口异常", throwable);
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            message = "AI 服务暂时不可用，请稍后重试";
        }
        return Flux.just(ServerSentEvent.<String>builder()
                .event("error")
                .data(message)
                .build());
    }
}
