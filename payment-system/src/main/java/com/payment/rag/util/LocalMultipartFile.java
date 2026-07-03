package com.payment.rag.util;

import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;

/**
 * 本地文件实现的MultipartFile
 * 避免将大文件完全加载到内存中
 */
public class LocalMultipartFile implements MultipartFile {

    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final File file;

    public LocalMultipartFile(String name, String originalFilename, String contentType, File file) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.file = file;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getOriginalFilename() {
        return this.originalFilename;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public boolean isEmpty() {
        return this.file.length() == 0;
    }

    @Override
    public long getSize() {
        return this.file.length();
    }

    @Override
    public byte[] getBytes() throws IOException {
        // 警告：如果文件过大，调用此方法可能会导致OOM
        return Files.readAllBytes(this.file.toPath());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(this.file);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        FileCopyUtils.copy(this.file, dest);
    }
}
