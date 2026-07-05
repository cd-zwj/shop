package com.payment.service;

import com.payment.common.BusinessException;
import com.payment.common.PageResult;
import com.payment.util.MinioUtil;
import com.payment.vo.FileAssetVO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileUploadApplicationServiceTest {

    private final MinioUtil minioUtil = mock(MinioUtil.class);
    private final FileAssetService fileAssetService = mock(FileAssetService.class);
    private final FileUploadApplicationService service = new FileUploadApplicationService(minioUtil, fileAssetService);

    @Test
    void uploadFileShouldRejectUnsupportedContentTypeBeforeStorage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "shell.exe", "application/octet-stream", "bad".getBytes());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.uploadFile(file, null, 10L, 20L));

        assertEquals("不支持的文件类型，仅支持 JPEG、PNG、GIF、WebP 图片和 PDF 文档", exception.getMessage());
        verify(minioUtil, never()).uploadFile(any(), any());
        verify(fileAssetService, never()).recordUpload(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void uploadFileShouldRejectUnsupportedExtensionBeforeStorage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "shell.exe", "image/png", "bad".getBytes());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.uploadFile(file, null, 10L, 20L));

        assertEquals("不支持的文件扩展名，仅支持 .jpg、.jpeg、.png、.gif、.webp、.pdf", exception.getMessage());
        verify(minioUtil, never()).uploadFile(any(), any());
        verify(fileAssetService, never()).recordUpload(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void uploadFileShouldRecordTenantUserAndMetadataForValidFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.pdf", "application/pdf", "content".getBytes());
        when(minioUtil.uploadFile(file, "md5")).thenReturn("http://local/file.pdf");

        String url = service.uploadFile(file, "md5", 10L, 20L);

        assertEquals("http://local/file.pdf", url);
        verify(fileAssetService).recordUpload(
                10L,
                20L,
                "receipt.pdf",
                "http://local/file.pdf",
                "md5",
                file.getSize(),
                "application/pdf");
    }

    @Test
    void uploadFileShouldCleanupObjectWhenAssetRecordFails() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.pdf", "application/pdf", "content".getBytes());
        when(minioUtil.uploadFile(file, null)).thenReturn("http://local/bucket/uploads/receipt.pdf");
        when(minioUtil.extractObjectNameFromUrl("http://local/bucket/uploads/receipt.pdf"))
                .thenReturn("uploads/receipt.pdf");
        when(fileAssetService.recordUpload(eq(10L), eq(20L), eq("receipt.pdf"), eq("http://local/bucket/uploads/receipt.pdf"),
                eq(null), eq(file.getSize()), eq("application/pdf")))
                .thenThrow(new RuntimeException("db failed"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.uploadFile(file, null, 10L, 20L));

        assertEquals("文件资产记录失败，请稍后重试", exception.getMessage());
        verify(minioUtil).deleteFile("uploads/receipt.pdf");
    }

    @Test
    void listFilesShouldReturnServicePageResultWithRealTotal() {
        PageResult<FileAssetVO> page = new PageResult<>(
                List.of(FileAssetVO.builder().id(1L).fileName("a.pdf").build()),
                15,
                2,
                10);
        when(fileAssetService.listByTenant(10L, 2, 10)).thenReturn(page);

        PageResult<FileAssetVO> result = service.listFiles(10L, 2, 10);

        assertEquals(15L, result.getTotal());
        assertEquals(2, result.getCurrent());
        assertEquals(2, result.getPages());
        assertEquals("a.pdf", result.getRecords().getFirst().getFileName());
    }
}
