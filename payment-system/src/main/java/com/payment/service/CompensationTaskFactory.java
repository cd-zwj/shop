package com.payment.service;

import com.payment.entity.CompensationTask;

public interface CompensationTaskFactory {

    CompensationTask createIfAbsent(String bizType, String bizNo, String remark);
}
