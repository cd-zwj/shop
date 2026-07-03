package com.payment.rag.service.processor;

import com.payment.rag.model.RagUnit;
import java.io.InputStream;
import java.util.List;

public interface MediaProcessor {
    boolean supports(String mimeType);
    List<RagUnit> process(InputStream input, String filename, String mimeType);

    /**
     * Process with URL support for multimodal AI (images)
     */
    default List<RagUnit> process(InputStream input, String filename, String mimeType, String fileUrl) {
        return process(input, filename, mimeType);
    }
}
