package com.payment.rag.service.processor;

import com.payment.rag.Config.VideoConfig;
import com.payment.rag.model.RagUnit;
import com.payment.rag.model.SourceType;
import com.payment.rag.service.AsrService;
import com.payment.rag.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.*;
import org.bytedeco.ffmpeg.global.avcodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoProcessor implements MediaProcessor {

    private final VideoConfig videoConfig;

    private final ImageProcessor imageProcessor;

    private final AsrService asrService;

    private final UploadService uploadService;

    @Qualifier("videoProcessorExecutor")
    private final ExecutorService executorService;

    @Override
    public boolean supports(String mimeType) {
        return mimeType != null && (
                mimeType.startsWith("video/") ||
                mimeType.equals("application/mp4")
        );
    }

    @Override
    public List<RagUnit> process(InputStream input, String filename, String mimeType) {
        // Fallback to default process if URL is not provided (though RagUnitService passes it now, we don't strictly need it here as we use stream)
        // But we implement the logic using the stream to create a temp file.
        return processInternal(input, filename);
    }

    @Override
    public List<RagUnit> process(InputStream input, String filename, String mimeType, String fileUrl) {
        // We use the input stream to create a local temp file for processing
        return processInternal(input, filename);
    }

    private List<RagUnit> processInternal(InputStream input, String filename) {
        log.info("正在处理视频文件: {}", filename);

        Path tempVideoFile = null;
        try {
            // 将输入流保存到临时文件（FFmpeg 需要文件访问）
            tempVideoFile = Files.createTempFile("video_", getExtension(filename));
            Files.copy(input, tempVideoFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String sourceId = UUID.randomUUID().toString();

            long durationMs = 0;
            double frameRate = 0;

            // 获取视频元数据
            // Disable logging from FFmpeg to console to keep logs clean
            FFmpegLogCallback.set();

            try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(tempVideoFile.toFile())) {
                grabber.start();
                long durationMicros = grabber.getLengthInTime();
                durationMs = durationMicros / 1000;
                frameRate = grabber.getFrameRate();
                grabber.stop();
            }

            log.info("视频时长: {}ms, 帧率: {}", durationMs, frameRate);

            int segmentDurationMs = videoConfig.getSegmentDuration() * 1000;
            int keyframeIntervalMs = videoConfig.getKeyframeInterval() * 1000;

            List<CompletableFuture<RagUnit>> futures = new ArrayList<>();
            int segmentIndex = 0;
            long currentTime = 0;

            // 为了在 lambda 中使用，必须是 effectively final
            final Path processingFile = tempVideoFile;

            while (currentTime < durationMs) {
                long segmentStart = currentTime;
                long segmentEnd = Math.min(currentTime + segmentDurationMs, durationMs);
                int currentSegmentIndex = segmentIndex;

                // 提交并行任务
                CompletableFuture<RagUnit> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        StringBuilder segmentText = new StringBuilder();

                        // 提取关键帧并调用 AI 生成描述 (传入文件路径，内部独立创建 grabber)
                        List<String> ocrTexts = extractKeyframeTexts(processingFile, segmentStart, segmentEnd, keyframeIntervalMs);
                        for (String ocrText : ocrTexts) {
                            if (!ocrText.isEmpty()) {
                                segmentText.append(ocrText).append(" ");
                            }
                        }

                        // 提取音频并进行语音识别
                        String asrText = extractAndTranscribeAudio(processingFile, segmentStart, segmentEnd);
                        if (asrText != null && !asrText.isEmpty()) {
                            segmentText.append(" [音频内容: ").append(asrText).append("] ");
                        }

                        // 如果有内容则创建 RagUnit
                        String content = segmentText.toString().trim();
                        if (!content.isEmpty()) {
                            RagUnit unit = new RagUnit();
                            unit.setSourceId(sourceId);
                            unit.setSourceType(SourceType.VIDEO);
                            unit.setContent(content);
                            unit.setChunkIndex(currentSegmentIndex);
                            unit.setStartTime(segmentStart);
                            unit.setEndTime(segmentEnd);
                            return unit;
                        }
                    } catch (Exception e) {
                        log.error("处理视频分片 {} 失败", currentSegmentIndex, e);
                    }
                    return null;
                }, executorService);

                futures.add(future);

                currentTime = segmentEnd;
                segmentIndex++;
            }

            // 等待所有任务完成并收集结果（带超时保护）
            List<RagUnit> units = new ArrayList<>();
            for (CompletableFuture<RagUnit> future : futures) {
                try {
                    RagUnit unit = future.get(5, TimeUnit.MINUTES);
                    if (unit != null) {
                        units.add(unit);
                    }
                } catch (java.util.concurrent.TimeoutException te) {
                    log.error("视频分片处理超时（5分钟），跳过该分片", te);
                    future.cancel(true);
                } catch (Exception e) {
                    log.error("等待视频分片结果失败", e);
                }
            }
            units.sort(Comparator.comparingInt(RagUnit::getChunkIndex));

            log.info("视频处理完成，共生成 {} 个分片", units.size());
            return units;

        } catch (Exception e) {
            log.error("处理视频文件时出错: {}", filename, e);
            throw new RuntimeException("视频文件处理失败", e);
        } finally {
            // 清理临时文件
            if (tempVideoFile != null) {
                try {
                    Files.deleteIfExists(tempVideoFile);
                } catch (IOException e) {
                    log.warn("删除临时视频文件失败", e);
                }
            }
        }
    }

    private List<String> extractKeyframeTexts(Path videoFile, long startMs, long endMs, int intervalMs) {
        List<String> texts = new ArrayList<>();
        Java2DFrameConverter converter = new Java2DFrameConverter();

        // 每个任务独立创建 grabber 以保证线程安全
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile.toFile())) {
            grabber.start();

            long currentMs = startMs;
            while (currentMs < endMs) {
                grabber.setTimestamp(currentMs * 1000); // 转换为微秒
                Frame frame = grabber.grabImage();

                if (frame != null && frame.image != null) {
                    BufferedImage image = converter.convert(frame);
                    if (image != null) {
                        // 将 BufferedImage 转换为字节数组
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(image, "png", baos);
                        byte[] imageBytes = baos.toByteArray();

                        // 上传关键帧到 MinIO
                        String keyframeName = "keyframe/" + UUID.randomUUID() + ".png";
                        uploadService.uploadFile(new ByteArrayInputStream(imageBytes), keyframeName, "image/png");
                        String keyframeUrl = uploadService.getFileUrl(keyframeName);

                        // 调用多模态 AI 生成图片描述
                        // 使用 URL 调用
                        String description = imageProcessor.describeImageByUrl(keyframeUrl, "image/png");
                        if (description != null && !description.trim().isEmpty()) {
                            texts.add(description.trim());
                        }

                        // 可选：删除关键帧图片以节省空间
                        // uploadService.deleteFile(keyframeName);
                    }
                }

                currentMs += intervalMs;
            }
            grabber.stop();
        } catch (Exception e) {
            log.warn("提取关键帧时出错", e);
        }

        return texts;
    }

    private String extractAndTranscribeAudio(Path videoFile, long startMs, long endMs) {
        Path tempAudioFile = null;
        try {
            tempAudioFile = Files.createTempFile("audio_", ".wav");

            // 使用 JavaCV (FFmpegFrameGrabber/Recorder) 替代命令行 FFmpeg
            try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile.toFile());
                 FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(tempAudioFile.toFile(), 1)) {

                grabber.start();

                // 配置录制器参数：16k采样率，单声道，PCM编码 (ASR要求)
                // 使用 s16le 格式生成原始 PCM 数据，不带 WAV 头
                recorder.setFormat("s16le");
                recorder.setSampleRate(16000);
                recorder.setAudioChannels(1);
                // avcodec.AV_CODEC_ID_PCM_S16LE 对应 PCM 16bit Little Endian
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_PCM_S16LE);

                recorder.start();

                // 定位到开始时间
                grabber.setTimestamp(startMs * 1000);

                Frame frame;
                while ((frame = grabber.grabSamples()) != null) {
                    // 检查是否超过结束时间
                    if (grabber.getTimestamp() > endMs * 1000) {
                        break;
                    }
                    recorder.record(frame);
                }

                recorder.stop();
                grabber.stop();
            }

            // 转录音频
            if (Files.exists(tempAudioFile) && Files.size(tempAudioFile) > 0) {
                try (InputStream audioStream = Files.newInputStream(tempAudioFile)) {
                    return asrService.transcribe(audioStream);
                }
            }

        } catch (Exception e) {
            log.warn("提取或转录音频片段时出错", e);
        } finally {
            if (tempAudioFile != null) {
                try {
                    Files.deleteIfExists(tempAudioFile);
                } catch (IOException e) {
                    log.warn("删除临时音频文件失败", e);
                }
            }
        }

        return "";
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : ".mp4";
    }
}
