package com.payment.rag.exception;

public class FileProcessingCancelledException extends RuntimeException {

    public FileProcessingCancelledException(String message) {
        super(message);
    }
}
