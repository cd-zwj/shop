package com.payment.netty;

import com.alibaba.fastjson2.JSON;
import com.payment.dto.ScanRequestDTO;
import com.payment.dto.ScanResponseDTO;
import com.payment.entity.Tenant;
import com.payment.mapper.TenantMapper;
import com.payment.service.ScanService;
import com.payment.util.TenantContextHolder;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Netty服务器 - 处理扫码请求
 */
@Slf4j
@Component
public class NettyServer {
    
    @Value("${netty.server.port:8888}")
    private int port;
    
    @Value("${netty.server.boss-threads:1}")
    private int bossThreads;
    
    @Value("${netty.server.worker-threads:4}")
    private int workerThreads;
    
    @Autowired
    private ScanService scanService;
    
    @Autowired
    private TenantMapper tenantMapper;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    
    @PostConstruct
    public void start() {
        new Thread(() -> {
            try {
                bossGroup = new NioEventLoopGroup(bossThreads);
                workerGroup = new NioEventLoopGroup(workerThreads);
                
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                
                                // 添加分隔符解码器（使用换行符作为消息分隔符）
                                ByteBuf delimiter = Unpooled.copiedBuffer("\n".getBytes());
                                pipeline.addLast(new DelimiterBasedFrameDecoder(8192, delimiter));
                                
                                // 添加编解码器
                                pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));
                                pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));
                                
                                // 添加心跳检测（60秒读超时）
                                pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
                                
                                // 添加业务处理器
                                pipeline.addLast(new ScanRequestHandler());
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.TCP_NODELAY, true);
                
                ChannelFuture future = bootstrap.bind(port).sync();
                serverChannel = future.channel();
                log.info("Netty服务器启动成功，监听端口：{}", port);
                
                future.channel().closeFuture().sync();
            } catch (Exception e) {
                log.error("Netty服务器启动失败", e);
            } finally {
                shutdown();
            }
        }, "Netty-Server-Thread").start();
    }
    
    @PreDestroy
    public void shutdown() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        log.info("Netty服务器已关闭");
    }
    
    /**
     * 扫码请求处理器
     */
    private class ScanRequestHandler extends ChannelInboundHandlerAdapter {
        
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info("新客户端连接：{}", ctx.channel().remoteAddress());
        }
        
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("客户端断开连接：{}", ctx.channel().remoteAddress());
        }
        
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            try {
                String message = (String) msg;
                log.info("收到扫码请求：{}", message);
                
                // 解析请求
                ScanRequestDTO request = JSON.parseObject(message, ScanRequestDTO.class);
                
                // 根据tenantCode查询tenantId并设置上下文
                if (request.getTenantCode() != null) {
                    Tenant tenant = tenantMapper.selectOne(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Tenant>()
                                    .eq(Tenant::getTenantCode, request.getTenantCode())
                                    .eq(Tenant::getStatus, 1)
                    );
                    if (tenant != null) {
                        TenantContextHolder.setTenantId(tenant.getId());
                    } else {
                        sendErrorResponse(ctx, "租户不存在或已被禁用");
                        return;
                    }
                }
                
                // 发送到RabbitMQ队列进行异步处理（可选）
                // rabbitTemplate.convertAndSend("payment.scan.request", message);
                
                // 直接处理扫码请求（同步处理）
                ScanResponseDTO response = scanService.handleScan(request);
                
                // 返回响应（添加换行符作为消息分隔符）
                String responseJson = JSON.toJSONString(response) + "\n";
                ctx.writeAndFlush(responseJson);
                
            } catch (Exception e) {
                log.error("处理扫码请求失败", e);
                sendErrorResponse(ctx, "处理失败：" + e.getMessage());
            } finally {
                // 清除租户上下文
                TenantContextHolder.clear();
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("Netty连接异常：{}", ctx.channel().remoteAddress(), cause);
            ctx.close();
        }
        
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof IdleStateEvent event) {
                if (event.state() == io.netty.handler.timeout.IdleState.READER_IDLE) {
                    log.warn("连接超时（60秒无数据），关闭连接：{}", ctx.channel().remoteAddress());
                    ctx.close();
                }
            }
        }
        
        /**
         * 发送错误响应
         */
        private void sendErrorResponse(ChannelHandlerContext ctx, String errorMessage) {
            ScanResponseDTO errorResponse = new ScanResponseDTO();
            errorResponse.setStatus("ERROR");
            errorResponse.setMessage(errorMessage);
            String responseJson = JSON.toJSONString(errorResponse) + "\n";
            ctx.writeAndFlush(responseJson);
        }
    }
}

