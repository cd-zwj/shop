package com.payment.rag.service.asr;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.payment.rag.exception.AsrUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;

@Component
@Slf4j
@RequiredArgsConstructor
public class DashScopeParaformerAsrProvider implements AsrProvider {

    private static final String PROVIDER_NAME = "dashscope-paraformer";

    private final DashScopeAsrProperties properties;

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled() && hasText(properties.getApiKey());
    }

    @Override
    public String transcribe(byte[] audioData) {
        if (!isAvailable()) {
            throw new AsrUnavailableException("DashScope Paraformer ASR 未配置");
        }

        File tempFile = null;
        Recognition recognizer = new Recognition();
        try {
            tempFile = File.createTempFile("dashscope-asr-", "." + properties.getFormat());
            Files.write(tempFile.toPath(), audioData);

            RecognitionParam param = RecognitionParam.builder()
                    .apiKey(properties.getApiKey())
                    .model(properties.getModel())
                    .format(properties.getFormat())
                    .sampleRate(properties.getSampleRate())
                    .parameter("language_hints", new String[]{"zh", "en"})
                    .build();

            String result = recognizer.call(param, tempFile);
            return result == null ? "" : result;
        } catch (Exception e) {
            log.warn("DashScope Paraformer ASR 识别失败: {}", e.getMessage());
            throw new AsrUnavailableException("DashScope Paraformer ASR 识别失败", e);
        } finally {
            closeRecognizer(recognizer);
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile.toPath());
                } catch (Exception e) {
                    log.debug("删除 DashScope ASR 临时音频文件失败: {}", tempFile.getAbsolutePath(), e);
                }
            }
        }
    }

    private void closeRecognizer(Recognition recognizer) {
        try {
            if (recognizer.getDuplexApi() != null) {
                recognizer.getDuplexApi().close(1000, "bye");
            }
        } catch (Exception e) {
            log.debug("关闭 DashScope ASR WebSocket 连接失败", e);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank() && !value.startsWith("your-");
    }
}
