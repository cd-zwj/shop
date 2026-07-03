package com.payment.rag.service.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 媒体处理器注册表，根据 MIME 类型自动查找对应的 MediaProcessor。
 * <p>
 * 通过 Spring 注入所有 MediaProcessor 实现，替代在 Service 中手动持有 7 个处理器引用。
 */
@Slf4j
@Component
public class MediaProcessorRegistry {

    private final List<MediaProcessor> processors;

    public MediaProcessorRegistry(List<MediaProcessor> processors) {
        this.processors = processors;
        log.info("已注册 {} 个媒体处理器: {}", processors.size(),
                processors.stream().map(p -> p.getClass().getSimpleName()).toList());
    }

    /**
     * 根据 MIME 类型查找匹配的处理器。
     *
     * @param mimeType MIME 类型
     * @return 匹配的处理器，未找到返回 null
     */
    public MediaProcessor findByMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return null;
        }
        for (MediaProcessor processor : processors) {
            if (processor.supports(mimeType)) {
                return processor;
            }
        }
        return null;
    }
}
