package com.payment.rag.model;

public enum DocumentFileStatus {
    UPLOADING,
    UPLOAD_SUCCESS,
    REINDEXING,
    CHUNKING,
    VECTORIZING,
    SUCCESS,
    FAILED;

    public boolean isProcessing() {
        return this == UPLOADING
                || this == UPLOAD_SUCCESS
                || this == REINDEXING
                || this == CHUNKING
                || this == VECTORIZING;
    }

    public boolean canInstantUpload() {
        return this == SUCCESS;
    }
}
