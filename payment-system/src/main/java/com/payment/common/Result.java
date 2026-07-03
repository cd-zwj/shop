package com.payment.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一接口响应包装类
 * <p>
 * 所有 RESTful 接口的返回值统一使用此类包装，
 * 前端可根据 {@code code} 字段判断请求是否成功。
 * </p>
 *
 * <pre>
 * 成功响应示例：
 * { "code": 200, "message": "操作成功", "data": {...}, "timestamp": 1719206400000 }
 *
 * 失败响应示例：
 * { "code": 500, "message": "操作失败", "data": null, "timestamp": 1719206400000 }
 * </pre>
 *
 * @param <T> 响应数据类型
 * @author payment-system
 */
@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码（200 表示成功，其他表示失败）
     */
    private Integer code;

    /**
     * 响应提示信息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 无参构造，自动设置时间戳
     */
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 全参构造
     *
     * @param code    状态码
     * @param message 提示信息
     * @param data    响应数据
     */
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 构建成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /**
     * 构建成功响应（带数据）
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 构建成功响应（自定义提示信息和数据）
     *
     * @param message 提示信息
     * @param data    响应数据
     * @param <T>     数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 构建失败响应（默认 500 状态码）
     *
     * @param message 错误信息
     * @param <T>     数据类型
     * @return 失败响应
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    /**
     * 构建失败响应（自定义状态码）
     *
     * @param code    错误码
     * @param message 错误信息
     * @param <T>     数据类型
     * @return 失败响应
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
