package com.payment.rag.Controller;

import com.payment.rag.model.dto.ApiResponse;
import com.payment.rag.model.dto.FileExistenceResponse;
import com.payment.rag.model.dto.UploadResponse;
import com.payment.rag.service.AuthContextService;
import com.payment.rag.service.UploadApplicationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传控制器
 * 负责单文件、批量文件上传及文件存在性检查。
 */
@RestController
@RequestMapping("/rag/api/upload")
@RequiredArgsConstructor
@Tag(name = "文件上传", description = "单文件/批量上传及文件存在性检查")
public class UploadController {

    private final UploadApplicationService uploadApplicationService;
    private final AuthContextService authContextService;

    /**
     * 检查文件是否已存在。
     *
     * @param fileHash 文件 SHA-256 哈希值
     * @param userId 用户 ID
     */    @GetMapping("/check")
    public ApiResponse<FileExistenceResponse> checkFileExists(@RequestParam("fileHash") String fileHash,
                                                              @RequestParam(value = "userId", required = false) String userId) {
        return uploadApplicationService.checkFileExists(fileHash, authContextService.resolveUserId(userId));
    }

    /**
     * 单文件上传。
     *
     * @param file 上传文件
     * @param fileHash 文件 SHA-256 哈希值
     * @param userId 用户 ID
     */    @PostMapping
    public ApiResponse<UploadResponse> uploadFile(@RequestParam("file") MultipartFile file,
                                                  @RequestParam("fileHash") String fileHash,
                                                  @RequestParam(value = "userId", required = false) String userId) {
        return uploadApplicationService.uploadFile(file, fileHash, authContextService.resolveUserId(userId));
    }

    /**
     * 批量文件上传。
     *
     * @param files 文件列表
     * @param fileHashes 文件哈希列表
     * @param userId 用户 ID
     */    @PostMapping("/batch")
    public ApiResponse<List<UploadResponse>> uploadFiles(@RequestParam("files") MultipartFile[] files,
                                                         @RequestParam("fileHashes") String[] fileHashes,
                                                         @RequestParam(value = "userId", required = false) String userId) {
        return uploadApplicationService.uploadFiles(files, fileHashes, authContextService.resolveUserId(userId));
    }
}
