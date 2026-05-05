package com.payment.util;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",
        streamingChatModel = "openAiStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        contentRetriever = "contentRetriever",
        tools = "aiTools")
public interface AiUtil {
    
    /**
     * 商家销售分析
     * 分析商家的销售数据，提供洞察和建议
     * 
     * @param merchantId 商家ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param analysisType 分析类型
     */
    @SystemMessage("你是一名专业的电商数据分析师。请分析商家的销售数据，提供关键洞察和具体建议。" +
            "你可以调用getSalesData工具获取销售数据。" +
            "请以JSON格式逐行返回分析结果，每行一个JSON对象，格式如下：\n" +
            "{\"type\":\"summary\",\"data\":{\"totalSales\":数值,\"orderCount\":数值,\"avgOrderValue\":数值,\"growthRate\":数值}}\n" +
            "{\"type\":\"trend\",\"data\":{\"labels\":[],\"sales\":[],\"orders\":[]}}\n" +
            "{\"type\":\"insights\",\"data\":[{\"type\":\"positive/warning\",\"title\":\"标题\",\"description\":\"描述\"}]}\n" +
            "{\"type\":\"recommendations\",\"data\":[\"建议1\",\"建议2\",\"建议3\"]}\n" +
            "注意：每个JSON对象后面加换行符\\n")
    Flux<String> analyzeSales(
            @UserMessage("请分析商家ID为{{merchantId}}的销售数据，时间范围：{{startDate}}到{{endDate}}，分析类型：{{analysisType}}。" +
                    "请先调用getSalesData工具获取数据，然后基于数据提供详细的分析、洞察和建议。") 
            @V("商家ID") Long merchantId,
            @V("开始日期 (yyyy-MM-dd)") String startDate,
            @V("结束日期 (yyyy-MM-dd)") String endDate,
            @V("分析类型: trend(趋势), category(品类), customer(客户)") String analysisType
    );
    
    /**
     * 用户商品推荐
     * 基于用户行为推荐商品
     * 
     * @param userId 用户ID（可选）
     * @param limit 推荐数量
     * @param scene 推荐场景
     */
    @SystemMessage("你是一名专业的商品推荐专家。请根据用户的购物历史和偏好推荐合适的商品。" +
            "你可以调用getRecommendProducts工具获取推荐商品列表。" +
            "请逐行返回推荐结果，每行一个JSON对象，格式如下：\n" +
            "{\"type\":\"recommendReason\",\"data\":\"推荐理由\"}\n" +
            "{\"type\":\"product\",\"data\":{\"productId\":数值,\"productName\":\"名称\",\"price\":数值,\"imageUrl\":\"URL\",\"merchantName\":\"商家\",\"score\":数值,\"reason\":\"理由\"}}\n" +
            "注意：每个JSON对象后面加换行符\\n")
    Flux<String> recommendProducts(
            @UserMessage("请为用户ID为{{userId}}推荐{{limit}}个商品，推荐场景：{{scene}}。" +
                    "请先调用getRecommendProducts工具获取商品列表，然后逐个返回推荐商品。")
            @V("用户ID") String userId,
            @V("推荐数量，默认10") Integer limit,
            @V("推荐场景: home(首页), detail(详情页), cart(购物车)") String scene
    );
    
    /**
     * 相似商品推荐
     * 推荐与指定商品相似的商品
     * 
     * @param productId 商品ID
     * @param limit 推荐数量
     */
    @SystemMessage("你是一名商品推荐专家。请推荐与指定商品相似的商品。" +
            "你可以调用getSimilarProducts工具获取相似商品列表。" +
            "请逐行返回商品，每行一个JSON对象，格式如下：\n" +
            "{\"type\":\"product\",\"data\":{\"productId\":数值,\"productName\":\"名称\",\"price\":数值,\"imageUrl\":\"URL\",\"merchantName\":\"商家\",\"similarity\":数值}}\n" +
            "注意：每个JSON对象后面加换行符\\n")
    Flux<String> recommendSimilarProducts(
            @UserMessage("请推荐与商品ID为{{productId}}相似的{{limit}}个商品。" +
                    "请先调用getSimilarProducts工具获取相似商品列表，然后逐个返回。")
            @V("商品ID") Long productId,
            @V("推荐数量，默认6") Integer limit
    );
    
    /**
     * AI对话助手
     * 与用户进行购物咨询对话
     * 
     * @param userId 用户ID（可选）
     * @param message 用户消息
     * @param context 上下文信息
     */
    @SystemMessage("你是一个专业、友好的购物助手。请根据用户的问题提供帮助和建议。" +
            "你可以调用工具搜索商品。" +
            "请以流式方式逐行返回回答，每行一个JSON对象，并在适当时候推荐商品和提供建议。" +
            "格式如下：\n" +
            "{\"type\":\"text\",\"data\":\"文本内容\"}\n" +
            "{\"type\":\"product\",\"data\":{\"productId\":数值,\"productName\":\"名称\",\"price\":数值,\"imageUrl\":\"URL\"}}\n" +
            "{\"type\":\"suggestion\",\"data\":\"建议内容\"}\n" +
            "注意：每个JSON对象后面加换行符\\n")
    Flux<String> chat(
            @UserMessage("用户ID：{{userId}}" +
                    "\n用户问题：{{message}}" +
                    "\n上下文信息：{{context}}\n\n" +
                    "请提供友好、专业的回答。如果涉及商品推荐，可以调用工具搜索相关商品。")
            @V("用户ID") String userId,
            @V("用户消息") String message,
            @V("上下文信息") String context
    );
}
