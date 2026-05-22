package com.payment.util;

import reactor.core.publisher.Flux;

public interface AiUtil {
    Flux<String> analyzeSales(
            Long merchantId,
            String startDate,
            String endDate,
            String analysisType
    );
    Flux<String> recommendProducts(
            String userId,
            Integer limit,
            String scene
    );

    Flux<String> recommendSimilarProducts(
            Long productId,
            Integer limit
    );

    Flux<String> chat(
            String userId,
            String message,
            String context
    );
}
