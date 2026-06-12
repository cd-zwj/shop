package com.payment.consumer;

import com.payment.config.RabbitMQConfig;
import com.payment.util.JsonUtils;
import com.payment.dto.ScanRequestDTO;
import com.payment.dto.ScanResponseDTO;
import com.payment.entity.Tenant;
import com.payment.mapper.TenantMapper;
import com.payment.service.ScanService;
import com.payment.util.TenantContextHolder;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ消费者 - 处理扫码请求
 */
/**
 * @deprecated POS 功能已下线，代码保留用于参考。
 */
@Deprecated
@Slf4j
@Component
public class ScanConsumer {
    
    @Autowired
    private ScanService scanService;
    
    @Autowired
    private TenantMapper tenantMapper;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    /**
     * 消费扫码请求
     * 从RabbitMQ队列中获取扫码请求，处理后将结果发送到结果队列
     */
    @RabbitListener(queues = RabbitMQConfig.SCAN_REQUEST_QUEUE)
    public void handleScanRequest(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            String messageBody = new String(message.getBody());
            log.info("RabbitMQ收到扫码请求消息：{}", messageBody);
            
            ScanRequestDTO request = JsonUtils.fromJson(messageBody, ScanRequestDTO.class);
            
            // 根据tenantCode查询tenantId并设置上下文
            if (request.getTenantCode() != null) {
                Tenant tenant = tenantMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Tenant>()
                                .eq(Tenant::getTenantCode, request.getTenantCode())
                                .eq(Tenant::getStatus, 1)
                );
                if (tenant != null) {
                    TenantContextHolder.setTenantId(tenant.getId());
                    log.info("设置租户上下文，tenantId: {}, tenantCode: {}", tenant.getId(), tenant.getTenantCode());
                } else {
                    log.warn("租户不存在或已被禁用，tenantCode: {}", request.getTenantCode());
                    // 发送错误响应到结果队列
                    ScanResponseDTO errorResponse = new ScanResponseDTO();
                    errorResponse.setStatus("ERROR");
                    errorResponse.setMessage("租户不存在或已被禁用");
                    rabbitTemplate.convertAndSend(RabbitMQConfig.SCAN_RESULT_QUEUE, JsonUtils.toJson(errorResponse));
                    
                    // 确认消息
                    channel.basicAck(deliveryTag, false);
                    return;
                }
            }
            
            // 调用ScanService处理扫码请求
            ScanResponseDTO response = scanService.handleScan(request);
            log.info("扫码请求处理完成，响应状态: {}", response.getStatus());
            
            // 发送处理结果到结果队列（供Netty服务器或其他消费者使用）
            rabbitTemplate.convertAndSend(RabbitMQConfig.SCAN_RESULT_QUEUE, JsonUtils.toJson(response));
            log.info("扫码处理结果已发送到结果队列");
            
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            log.error("处理扫码请求失败", e);
            try {
                // 发送错误响应到结果队列
                ScanResponseDTO errorResponse = new ScanResponseDTO();
                errorResponse.setStatus("ERROR");
                errorResponse.setMessage("处理失败，请稍后重试");
                rabbitTemplate.convertAndSend("payment.scan.result", JsonUtils.toJson(errorResponse));
                
                // 拒绝消息，不重新入队（避免死循环）
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("拒绝消息失败", ex);
            }
        } finally {
            // 清除租户上下文
            TenantContextHolder.clear();
        }
    }
}



