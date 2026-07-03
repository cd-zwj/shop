package com.payment.rag.service.processor;

import com.payment.rag.model.RagUnit;
import com.payment.rag.model.SourceType;
import com.payment.rag.service.TextSplitterService;
import com.payment.rag.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PowerPointProcessor implements MediaProcessor {

    private final ImageProcessor imageProcessor;

    private final UploadService uploadService;

    private final TextSplitterService textSplitterService;

    @Value("${chunking.token.ppt-chunk-size:1000}")
    private int pptChunkSize;

    @Override
    public boolean supports(String mimeType) {
        return mimeType != null && (
                mimeType.equals("application/vnd.ms-powerpoint") ||
                mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")
        );
    }

    @Override
    public List<RagUnit> process(InputStream input, String filename, String mimeType) {
        log.warn("PowerPointProcessor.process 被调用，但未提供 URL");
        return new ArrayList<>();
    }

    @Override
    public List<RagUnit> process(InputStream input, String filename, String mimeType, String fileUrl) {
        log.info("正在处理PowerPoint文件: {}", filename);
        List<RagUnit> units = new ArrayList<>();

        try {
            XMLSlideShow ppt = new XMLSlideShow(input);
            List<XSLFSlide> slides = ppt.getSlides();
            int chunkIndex = 0;

            for (int slideNum = 0; slideNum < slides.size(); slideNum++) {
                XSLFSlide slide = slides.get(slideNum);
                StringBuilder slideContent = new StringBuilder();
                slideContent.append("===== 幻灯片 ").append(slideNum + 1).append(" =====\n\n");

                // 1. 提取文本内容
                String slideText = extractSlideText(slide);
                if (!slideText.trim().isEmpty()) {
                    slideContent.append("【文本内容】\n").append(slideText).append("\n\n");
                }

                // 2. 提取并处理图片
                List<XSLFPictureShape> pictures = extractPictures(slide);
                if (!pictures.isEmpty()) {
                    slideContent.append("【图片描述】\n");

                    for (int picIdx = 0; picIdx < pictures.size(); picIdx++) {
                        XSLFPictureShape picture = pictures.get(picIdx);
                        try {
                            // 上传图片到MinIO
                            XSLFPictureData pictureData = picture.getPictureData();
                            byte[] imageBytes = pictureData.getData();
                            String imageType = pictureData.getContentType();

                            String imagePath = UUID.randomUUID().toString() + "/slide" + (slideNum + 1) + "_img" + (picIdx + 1) + getImageExtension(imageType);
                            uploadService.uploadFile(new ByteArrayInputStream(imageBytes), imagePath, imageType);
                            String imageUrl = uploadService.getFileUrl(imagePath);

                            // 使用AI生成图片描述
                            String imageDesc = imageProcessor.describeImageByUrl(imageUrl, imageType);

                            if (imageDesc != null && !imageDesc.trim().isEmpty()) {
                                slideContent.append("图片 ").append(picIdx + 1).append(": ").append(imageDesc).append("\n");
                            }

                            // 添加延迟避免API限流
                            if (picIdx < pictures.size() - 1) {
                                Thread.sleep(1000);
                            }

                        } catch (Exception e) {
                            log.warn("处理幻灯片 {} 的图片 {} 失败: {}", slideNum + 1, picIdx + 1, e.getMessage());
                            slideContent.append("图片 ").append(picIdx + 1).append(": [处理失败]\n");
                        }
                    }
                }

                // 3. 创建RagUnit
                String content = slideContent.toString().trim();
                if (!content.isEmpty()) {
                    // 使用 TokenTextSplitter 进行智能分块
                    List<String> chunks = textSplitterService.splitText(content, pptChunkSize);

                    for (String chunk : chunks) {
                        RagUnit unit = new RagUnit();
                        unit.setSourceType(SourceType.TEXT);
                        unit.setContent(chunk);
                        unit.setChunkIndex(chunkIndex++);
                        units.add(unit);
                    }
                }

                log.info("已处理幻灯片 {}/{}", slideNum + 1, slides.size());
            }

            ppt.close();
            log.info("PowerPoint文件处理完成，共生成 {} 个分块", units.size());

        } catch (Exception e) {
            log.error("处理PowerPoint文件失败: {}", filename, e);
            throw new RuntimeException("Failed to process PowerPoint file", e);
        }

        return units;
    }

    /**
     * 提取幻灯片中的所有文本
     */
    private String extractSlideText(XSLFSlide slide) {
        StringBuilder text = new StringBuilder();
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape) {
                XSLFTextShape textShape = (XSLFTextShape) shape;
                String shapeText = textShape.getText();
                if (shapeText != null && !shapeText.trim().isEmpty()) {
                    text.append(shapeText.trim()).append("\n");
                }
            }
        }
        return text.toString();
    }

    /**
     * 提取幻灯片中的所有图片
     */
    private List<XSLFPictureShape> extractPictures(XSLFSlide slide) {
        List<XSLFPictureShape> pictures = new ArrayList<>();
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFPictureShape) {
                pictures.add((XSLFPictureShape) shape);
            }
        }
        return pictures;
    }

    /**
     * 根据MIME类型获取图片扩展名
     */
    private String getImageExtension(String mimeType) {
        if (mimeType == null) return ".jpg";
        if (mimeType.contains("png")) return ".png";
        if (mimeType.contains("jpeg") || mimeType.contains("jpg")) return ".jpg";
        if (mimeType.contains("gif")) return ".gif";
        return ".jpg";
    }
}
