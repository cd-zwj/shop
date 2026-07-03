package com.payment.rag.service;

import com.payment.rag.exception.AsrUnavailableException;
import com.payment.rag.service.asr.AsrFallbackProperties;
import com.payment.rag.service.asr.AsrProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AsrService {

    private final List<AsrProvider> providers;

    public AsrService(List<AsrProvider> providers, AsrFallbackProperties properties) {
        Map<String, AsrProvider> providerByName = providers.stream()
                .collect(Collectors.toMap(AsrProvider::name, Function.identity(), (left, right) -> left));
        this.providers = providers.stream()
                .filter(provider -> properties.getProviderOrder().contains(provider.name()))
                .sorted(Comparator.comparingInt(provider -> properties.getProviderOrder().indexOf(provider.name())))
                .toList();

        List<String> missingProviders = properties.getProviderOrder().stream()
                .filter(name -> !providerByName.containsKey(name))
                .toList();
        if (!missingProviders.isEmpty()) {
            log.warn("ASR provider 配置不存在，将跳过: {}", missingProviders);
        }
    }

    public String transcribe(InputStream audioStream) {
        byte[] audioData;
        try {
            audioData = audioStream.readAllBytes();
        } catch (Exception e) {
            log.error("读取音频流失败", e);
            throw new AsrUnavailableException("读取音频流失败", e);
        }

        AsrUnavailableException lastFailure = null;
        for (AsrProvider provider : providers) {
            if (!provider.isAvailable()) {
                log.debug("ASR provider 不可用，跳过: {}", provider.name());
                continue;
            }

            try {
                log.info("正在使用 ASR provider: {}", provider.name());
                return provider.transcribe(audioData);
            } catch (AsrUnavailableException e) {
                lastFailure = e;
                log.warn("ASR provider 调用失败，将尝试下一个: {}, 原因: {}", provider.name(), e.getMessage());
            } catch (Exception e) {
                lastFailure = new AsrUnavailableException(provider.name() + " ASR 调用失败", e);
                log.warn("ASR provider 调用异常，将尝试下一个: {}, 原因: {}", provider.name(), e.getMessage());
            }
        }

        if (lastFailure != null) {
            throw new AsrUnavailableException("ASR 语音识别服务暂不可用", lastFailure);
        }
        throw new AsrUnavailableException("ASR 语音识别服务未配置或初始化失败");
    }

    /**
     * 判断至少一个 ASR provider 是否可用。
     */
    public boolean isAvailable() {
        return providers.stream().anyMatch(AsrProvider::isAvailable);
    }
}
