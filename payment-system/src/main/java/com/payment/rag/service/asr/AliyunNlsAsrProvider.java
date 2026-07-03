package com.payment.rag.service.asr;

import com.alibaba.nls.client.AccessToken;
import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriber;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberListener;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberResponse;
import com.payment.rag.Config.AliyunAsrConfig;
import com.payment.rag.exception.AsrUnavailableException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class AliyunNlsAsrProvider implements AsrProvider {

    private static final String PROVIDER_NAME = "aliyun-nls";

    private final AliyunAsrConfig asrConfig;

    private volatile NlsClient nlsClient;
    private volatile boolean configured = false;

    @PostConstruct
    public void init() {
        try {
            if (hasText(asrConfig.getAccessKeyId())
                    && hasText(asrConfig.getAccessKeySecret())
                    && hasText(asrConfig.getAppKey())) {
                log.info("正在初始化阿里云 NLS 客户端...");
                AccessToken token = new AccessToken(asrConfig.getAccessKeyId(), asrConfig.getAccessKeySecret());
                token.apply();
                this.nlsClient = new NlsClient(token.getToken());
                this.configured = true;
                log.info("阿里云 ASR 客户端初始化成功");
            } else {
                log.warn("未配置阿里云 ASR 凭证，阿里云 NLS ASR provider 不可用");
            }
        } catch (Exception e) {
            log.error("初始化阿里云 ASR 客户端失败", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (nlsClient != null) {
            nlsClient.shutdown();
        }
    }

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        return configured && nlsClient != null;
    }

    @Override
    public String transcribe(byte[] audioData) {
        if (!isAvailable()) {
            throw new AsrUnavailableException("阿里云 NLS ASR 未配置或初始化失败");
        }

        for (int attempt = 0; attempt <= 2; attempt++) {
            if (attempt > 0) {
                log.info("阿里云 NLS ASR 重试第 {} 次", attempt);
            }

            String result = doTranscribe(new ByteArrayInputStream(audioData));
            if (result != null) {
                return result;
            }
        }

        throw new AsrUnavailableException("阿里云 NLS ASR 识别失败，已重试 2 次");
    }

    private String doTranscribe(InputStream audioStream) {
        StringBuilder result = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        SpeechTranscriber transcriber = null;

        try {
            transcriber = new SpeechTranscriber(nlsClient, getTranscriberListener(result, latch));
            transcriber.setAppKey(asrConfig.getAppKey());
            transcriber.setFormat(InputFormatEnum.PCM);
            transcriber.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
            transcriber.setEnableIntermediateResult(false);
            transcriber.setEnablePunctuation(true);
            transcriber.setEnableITN(true);

            transcriber.start();

            byte[] buffer = new byte[3200];
            int bytesRead;
            while ((bytesRead = audioStream.read(buffer)) != -1) {
                transcriber.send(buffer, bytesRead);
            }

            transcriber.stop();
            if (!latch.await(5, TimeUnit.SECONDS)) {
                log.warn("阿里云 NLS ASR 回调超时");
                return null;
            }

            return result.toString();
        } catch (Exception e) {
            log.error("阿里云 NLS ASR 识别过程中出现异常", e);
            return null;
        } finally {
            if (transcriber != null) {
                try {
                    transcriber.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private SpeechTranscriberListener getTranscriberListener(StringBuilder result, CountDownLatch latch) {
        return new SpeechTranscriberListener() {
            @Override
            public void onTranscriberStart(SpeechTranscriberResponse response) {
                log.debug("阿里云 NLS ASR 已开始，任务 ID: {}", response.getTaskId());
            }

            @Override
            public void onSentenceBegin(SpeechTranscriberResponse response) {
                log.debug("句子开始，序号: {}", response.getTransSentenceIndex());
            }

            @Override
            public void onSentenceEnd(SpeechTranscriberResponse response) {
                String text = response.getTransSentenceText();
                if (hasText(text)) {
                    result.append(text).append(" ");
                }
                log.debug("句子结束，内容: {}", text);
            }

            @Override
            public void onTranscriptionResultChange(SpeechTranscriberResponse response) {
                // Intermediate results are disabled.
            }

            @Override
            public void onTranscriptionComplete(SpeechTranscriberResponse response) {
                latch.countDown();
            }

            @Override
            public void onFail(SpeechTranscriberResponse response) {
                log.error("阿里云 NLS ASR 识别失败: {}", response.getStatusText());
                latch.countDown();
            }
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank() && !value.startsWith("your-");
    }
}
