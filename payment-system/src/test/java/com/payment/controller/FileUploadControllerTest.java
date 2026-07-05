package com.payment.controller;

import com.payment.common.BusinessException;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.common.TenantContextHolder;
import com.payment.service.FileUploadApplicationService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.FileAssetVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileUploadControllerTest {

    private final FileUploadApplicationService fileUploadApplicationService = mock(FileUploadApplicationService.class);
    private final FileUploadController legacyController = new FileUploadController(fileUploadApplicationService);
    private final V1MerchantFileController v1Controller = new V1MerchantFileController(fileUploadApplicationService);

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void legacyUploadShouldUseCurrentTenantContext() {
        TenantContextHolder.setTenantId(10L);
        MockMultipartFile file = new MockMultipartFile("file", "receipt.pdf", "application/pdf", "ok".getBytes());

        try (MockedStatic<PlatformSessionHelper> platformSession = mockStatic(PlatformSessionHelper.class)) {
            platformSession.when(PlatformSessionHelper::getPlatformUserId).thenReturn(20L);
            when(fileUploadApplicationService.uploadFile(file, "md5", 10L, 20L)).thenReturn("http://local/file.pdf");

            Result<String> result = legacyController.uploadFile(file, "md5");

            assertEquals("http://local/file.pdf", result.getData());
            verify(fileUploadApplicationService).uploadFile(file, "md5", 10L, 20L);
        }
    }

    @Test
    void v1UploadShouldIgnorePathTenantAndUseCurrentTenantContext() {
        TenantContextHolder.setTenantId(10L);
        MockMultipartFile file = new MockMultipartFile("file", "receipt.pdf", "application/pdf", "ok".getBytes());

        try (MockedStatic<PlatformSessionHelper> platformSession = mockStatic(PlatformSessionHelper.class)) {
            platformSession.when(PlatformSessionHelper::getPlatformUserId).thenReturn(20L);
            when(fileUploadApplicationService.uploadFile(file, "md5", 10L, 20L)).thenReturn("http://local/file.pdf");

            Result<String> result = v1Controller.uploadFile(999L, file, "md5");

            assertEquals("http://local/file.pdf", result.getData());
            verify(fileUploadApplicationService).uploadFile(file, "md5", 10L, 20L);
        }
    }

    @Test
    void v1ListShouldIgnorePathTenantAndUseCurrentTenantContext() {
        TenantContextHolder.setTenantId(10L);
        PageResult<FileAssetVO> page = new PageResult<>(
                List.of(FileAssetVO.builder().id(1L).fileName("receipt.pdf").build()),
                1,
                1,
                10);
        when(fileUploadApplicationService.listFiles(10L, 1, 10)).thenReturn(page);

        Result<PageResult<FileAssetVO>> result = v1Controller.listFiles(999L, 1, 10);

        assertEquals(1L, result.getData().getTotal());
        verify(fileUploadApplicationService).listFiles(10L, 1, 10);
    }

    @Test
    void uploadShouldPropagateBusinessExceptionToGlobalHandler() {
        TenantContextHolder.setTenantId(10L);
        MockMultipartFile file = new MockMultipartFile("file", "shell.exe", "application/octet-stream", "bad".getBytes());

        try (MockedStatic<PlatformSessionHelper> platformSession = mockStatic(PlatformSessionHelper.class)) {
            platformSession.when(PlatformSessionHelper::getPlatformUserId).thenReturn(20L);
            when(fileUploadApplicationService.uploadFile(file, null, 10L, 20L))
                    .thenThrow(new BusinessException("不支持的文件类型"));

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> v1Controller.uploadFile(999L, file, null));

            assertEquals("不支持的文件类型", exception.getMessage());
        }
    }
}
