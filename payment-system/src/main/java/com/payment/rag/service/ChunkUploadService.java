package com.payment.rag.service;

import com.payment.rag.model.dto.ChunkUploadStatus;
import com.payment.rag.model.dto.FileExistenceResponse;
import com.payment.rag.model.dto.UploadResponse;
import com.payment.rag.util.FileNameSanitizer;
import com.payment.rag.util.HashUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChunkUploadService {

    private final ChunkUploadRedisService chunkUploadRedisService;

    private final RagUnitService ragUnitService;

    @Value("${file.upload.temp-dir:./temp/chunks}")
    private String tempDirBase;

    public ChunkUploadStatus checkUploadStatus(String userId, String fileHash, String filename,
                                               Long fileSize, Integer totalChunks) {
        FileExistenceResponse existenceCheck = ragUnitService.checkFileExists(userId, fileHash);
        if (existenceCheck.getExists()) {
            return null;
        }

        ChunkUploadStatus status = chunkUploadRedisService.getUploadStatus(userId, fileHash);
        if (status == null) {
            String tempDir = tempDirBase + "/" + userId + "/" + fileHash;
            chunkUploadRedisService.initUploadStatus(userId, fileHash, filename, fileSize, totalChunks, tempDir);
            status = chunkUploadRedisService.getUploadStatus(userId, fileHash);
        }
        return status;
    }

    public void uploadChunk(String userId, String fileHash, Integer chunkNumber, MultipartFile chunkFile) throws IOException {
        if (chunkUploadRedisService.isChunkUploaded(userId, fileHash, chunkNumber)) {
            return;
        }

        ChunkUploadStatus status = chunkUploadRedisService.getUploadStatus(userId, fileHash);
        if (status == null) {
            throw new RuntimeException("上传状态不存在");
        }

        Path tempDirPath = Paths.get(status.getTempDir()).toAbsolutePath().normalize();
        Path allowedBase = Paths.get(tempDirBase).toAbsolutePath().normalize();
        if (!FileNameSanitizer.isInsideAllowedBase(tempDirPath, allowedBase)) {
            log.warn("检测到 uploadChunk tempDir 路径穿越: userId={}, fileHash={}, tempDir={}", userId, fileHash, status.getTempDir());
            throw new IllegalArgumentException("临时目录路径不合法");
        }
        if (!Files.exists(tempDirPath)) {
            Files.createDirectories(tempDirPath);
        }

        String chunkFilename = String.format("chunk_%05d", chunkNumber);
        Path chunkPath = tempDirPath.resolve(chunkFilename);

        try (InputStream inputStream = chunkFile.getInputStream()) {
            Files.copy(inputStream, chunkPath, StandardCopyOption.REPLACE_EXISTING);
        }

        chunkUploadRedisService.markChunkUploaded(userId, fileHash, chunkNumber);
    }

    public UploadResponse mergeChunks(String userId, String fileHash, String filename) throws Exception {
        ChunkUploadStatus status = chunkUploadRedisService.getUploadStatus(userId, fileHash);
        if (status == null) {
            throw new RuntimeException("上传状态不存在");
        }

        if (!status.isAllChunksUploaded()) {
            throw new RuntimeException(String.format(
                    "分片未上传完成: %d/%d", status.getUploadedChunks().size(), status.getTotalChunks()));
        }

        Path tempDirPath = Paths.get(status.getTempDir()).toAbsolutePath().normalize();
        Path allowedBase = Paths.get(tempDirBase).toAbsolutePath().normalize();
        if (!FileNameSanitizer.isInsideAllowedBase(tempDirPath, allowedBase)) {
            log.warn("检测到 mergeChunks tempDir 路径穿越: userId={}, fileHash={}, tempDir={}", userId, fileHash, status.getTempDir());
            throw new IllegalArgumentException("临时目录路径不合法");
        }
        Path mergedFile = tempDirPath.resolve("merged_" + filename).normalize();
        if (!FileNameSanitizer.isInsideAllowedBase(mergedFile, tempDirPath)) {
            log.warn("检测到 mergeChunks 文件名路径穿越: userId={}, fileHash={}, filename={}", userId, fileHash, filename);
            throw new IllegalArgumentException("文件名不合法");
        }

        try (FileOutputStream fos = new FileOutputStream(mergedFile.toFile())) {
            for (int i = 0; i < status.getTotalChunks(); i++) {
                String chunkFilename = String.format("chunk_%05d", i);
                Path chunkPath = tempDirPath.resolve(chunkFilename);
                if (!Files.exists(chunkPath)) {
                    throw new RuntimeException("分片文件不存在: " + chunkFilename);
                }

                try (FileInputStream fis = new FileInputStream(chunkPath.toFile())) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
            }
        }

        File file = mergedFile.toFile();
        MultipartFile multipartFile = new com.payment.rag.util.LocalMultipartFile(
                "file",
                filename,
                null,
                file
        );

        // 合并完成后，服务端重算 SHA-256
        String serverFileHash = HashUtils.hashFile(mergedFile);
        log.info("分片合并完成, 服务端 hash={}, 原始 fileHash={}", serverFileHash, fileHash);

        // 使用服务端重算的 hash 传给下游处理
        UploadResponse response = ragUnitService.processAndStoreAsync(multipartFile, serverFileHash, userId);

        cleanupTempFiles(tempDirPath);
        chunkUploadRedisService.deleteUploadStatus(userId, fileHash);
        return response;
    }

    private void cleanupTempFiles(Path tempDir) {
        try {
            if (Files.exists(tempDir)) {
                try (Stream<Path> paths = Files.walk(tempDir)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("删除临时文件失败: {}", path, e);
                        }
                    });
                }
            }
        } catch (IOException e) {
            log.error("清理临时文件失败: {}", tempDir, e);
        }
    }
}
