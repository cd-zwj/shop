package com.payment.rag.service.asr;

public interface AsrProvider {

    String name();

    boolean isAvailable();

    String transcribe(byte[] audioData);
}
