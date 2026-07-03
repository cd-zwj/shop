package com.payment.rag.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * MinerU 官方云端 API 客户端 (mineru.net)
 * <p>
 * 调用流程:
 * 1. POST /api/v4/extract/task  提交文件 URL → 得到 task_id
 * 2. GET  /api/v4/extract/task/{task_id} 轮询状态 (state: pending / running / done / failed)
 * 3. 从 done 结果中取出 markdown 字段作为最终提取文本
 */
@Component
@Slf4j
public class MinerUClient {

    private static final String BASE_URL = "https://mineru.net";
    private static final int POLL_INTERVAL_SECONDS = 5;
    private static final int MAX_POLL_MINUTES = 10;

    @Value("${mineru.api-key:}")
    private String apiKey;

    @Value("${mineru.model-version:vlm}")
    private String modelVersion;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MinerUClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 通过 MinerU API 从公开可访问的文件 URL 提取文本（Markdown 格式）。
     *
     * @param fileUrl  MinIO 或其他公开可访问的文件下载地址
     * @param filename 文件名（仅用于日志记录）
     * @return 解析后的 Markdown 文本
     * @throws RuntimeException 解析失败时抛出
     */
    public String extractText(String fileUrl, String filename) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("MinerU API Key 未配置，请在 application.yaml 中设置 mineru.api-key");
        }

        log.info("[MinerU] 开始解析文件: {}, URL: {}", filename, fileUrl);

        // Step 1: 提交任务
        String taskId = submitTask(fileUrl, filename);
        log.info("[MinerU] 任务已提交, taskId: {}", taskId);

        // Step 2: 轮询状态
        JsonNode result = pollUntilDone(taskId, filename);
        log.info("[MinerU] 任务完成, taskId: {}", taskId);

        // Step 3: 提取 Markdown 内容
        return extractMarkdown(result, filename);
    }

    // ---------------------------------------------------------------- private helpers

    private String submitTask(String fileUrl, String filename) {
        try {
            String body = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
                put("url", fileUrl);
                put("model_version", modelVersion);
                // 根据官方文档，参数名是 enable_formula 和 enable_table
                put("enable_formula", true);
                put("enable_table", true);
            }});

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/v4/extract/task"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "*/*")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                throw new RuntimeException("[MinerU] 提交任务失败, status=" + response.statusCode()
                        + ", body=" + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            if (!isSuccess(json)) {
                throw new RuntimeException("[MinerU] 提交任务接口返回错误: " + response.body());
            }

            String taskId = json.path("data").path("task_id").asText(null);
            if (taskId == null || taskId.isBlank()) {
                throw new RuntimeException("[MinerU] 接口未返回 task_id: " + response.body());
            }
            return taskId;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("[MinerU] 提交任务时发生异常: " + filename, e);
        }
    }

    private JsonNode pollUntilDone(String taskId, String filename) {
        int maxPolls = (MAX_POLL_MINUTES * 60) / POLL_INTERVAL_SECONDS;
        String pollUrl = BASE_URL + "/api/v4/extract/task/" + taskId;

        for (int i = 0; i < maxPolls; i++) {
            try {
                TimeUnit.SECONDS.sleep(POLL_INTERVAL_SECONDS);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(pollUrl))
                        .header("Authorization", "Bearer " + apiKey)
                        .GET()
                        .timeout(Duration.ofSeconds(30))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    log.warn("[MinerU] 轮询状态返回异常 status={}, 继续等待...", response.statusCode());
                    continue;
                }

                JsonNode json = objectMapper.readTree(response.body());
                if (!isSuccess(json)) {
                    throw new RuntimeException("[MinerU] 轮询接口返回错误: " + response.body());
                }

                JsonNode data = json.path("data");
                String state = data.path("state").asText("");

                log.debug("[MinerU] taskId={}, state={}, 已等待约{}秒",
                        taskId, state, (long) (i + 1) * POLL_INTERVAL_SECONDS);

                switch (state) {
                    case "done":
                        return data;
                    case "failed":
                        String errMsg = data.path("err_msg").asText("未知错误");
                        throw new RuntimeException("[MinerU] 任务解析失败: " + errMsg + ", file=" + filename);
                    case "pending":
                    case "running":
                    case "waiting-file":
                    case "converting":
                        // 这些都是官方处于进行中的状态
                        break;
                    default:
                        log.warn("[MinerU] 未知状态: {}, 继续轮询...", state);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("[MinerU] 轮询被中断: " + filename, e);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                log.warn("[MinerU] 轮询请求异常, 将在下一轮重试: {}", e.getMessage());
            }
        }

        throw new RuntimeException("[MinerU] 任务超时(" + MAX_POLL_MINUTES + "分钟), taskId=" + taskId
                + ", file=" + filename);
    }

    private String extractMarkdown(JsonNode data, String filename) {
        // v4 精准解析接口返回的是全量 ZIP 文件包链接：full_zip_url
        // 包里包含 md 文件、json 数据格式等。必须下载该 ZIP 并在内存里提取 Markdown 文本。
        String zipUrl = data.path("full_zip_url").asText(null);

        if (zipUrl == null || zipUrl.isBlank()) {
            throw new RuntimeException("[MinerU] 解析完成但未返回 full_zip_url, data=" + data.toString());
        }

        log.info("[MinerU] 开始下载结果 ZIP: {}", zipUrl);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(zipUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(60))
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new RuntimeException("下载 ZIP 失败, status=" + response.statusCode());
            }

            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(response.body())) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    // 通常包含 main.md、auto.md 或其他.md后缀的最终文件
                    if (entry.getName().endsWith(".md")) {
                        byte[] bytes = zis.readAllBytes();
                        String mdText = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                        log.info("[MinerU] 成功从 ZIP 提取文本 ({}), 文件: {}, 字符数: {}",
                                entry.getName(), filename, mdText.length());
                        return mdText;
                    }
                }
            }
            throw new RuntimeException("[MinerU] ZIP 包内未找到 .md 格式的 Markdown 文件");
        } catch (Exception e) {
            log.error("[MinerU] 解压下载 ZIP 的过程中出错: {}", filename, e);
            throw new RuntimeException("[MinerU] 提取 Markdown 异常: " + filename, e);
        }
    }

    /**
     * 判断 MinerU API 返回是否成功（code == 0 或不含 code 字段时视为成功）
     */
    private boolean isSuccess(JsonNode json) {
        JsonNode code = json.path("code");
        if (code.isMissingNode()) return true;   // 部分接口不含 code
        return code.asInt(-1) == 0;
    }
}
