package com.payment.rag.service.processor;

import com.payment.rag.model.RagUnit;
import com.payment.rag.model.SourceType;
import com.payment.rag.service.TabularRowChunker;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class TabularProcessor implements MediaProcessor {

    private final TabularRowChunker tabularRowChunker;

    public TabularProcessor(TabularRowChunker tabularRowChunker) {
        this.tabularRowChunker = tabularRowChunker;
    }

    @Override
    public boolean supports(String mimeType) {
        return isCsvMimeType(mimeType) || isExcelMimeType(mimeType);
    }

    @Override
    public List<RagUnit> process(InputStream input, String filename, String mimeType) {
        if (isCsvMimeType(mimeType)) {
            return tabularRowChunker.chunkCsv(input, filename, SourceType.TEXT);
        }
        return tabularRowChunker.chunkWorkbook(input, filename, SourceType.TEXT);
    }

    private boolean isCsvMimeType(String mimeType) {
        return "text/csv".equals(mimeType) || "application/csv".equals(mimeType);
    }

    private boolean isExcelMimeType(String mimeType) {
        return "application/vnd.ms-excel".equals(mimeType)
                || "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(mimeType);
    }
}
