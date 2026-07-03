package com.payment.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.rag.model.DocumentFile;
import com.payment.rag.model.dto.RagDocumentInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocumentFileMapper extends BaseMapper<DocumentFile> {

    @Select("<script>" +
            "SELECT " +
            "  source_id as sourceId, " +
            "  file_hash as fileHash, " +
            "  user_id as userId, " +
            "  filename as filename, " +
            "  source_type as sourceType, " +
            "  minio_url as minioUrl, " +
            "  minio_path as minioPath, " +
            "  file_size as fileSize, " +
            "  status as status, " +
            "  error_message as errorMessage, " +
            "  chunk_count as chunkCount, " +
            "  created_at as createdAt, " +
            "  updated_at as updatedAt " +
            "FROM document_file " +
            "<where>" +
            "  deleted = 0 " +
            "  <if test='userId != null and userId != \"\"'> AND user_id = #{userId} </if>" +
            "  <if test='sourceType != null and sourceType != \"\"'> AND source_type = #{sourceType} </if>" +
            "  <if test='keyword != null and keyword != \"\"'> AND filename LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "</where>" +
            "ORDER BY " +
            "<choose>" +
            "  <when test='sortBy == \"createdAt\" and sortOrder == \"ASC\"'>created_at ASC</when>" +
            "  <when test='sortBy == \"createdAt\" and sortOrder == \"DESC\"'>created_at DESC</when>" +
            "  <when test='sortBy == \"updatedAt\" and sortOrder == \"ASC\"'>updated_at ASC</when>" +
            "  <when test='sortBy == \"updatedAt\" and sortOrder == \"DESC\"'>updated_at DESC</when>" +
            "  <otherwise>created_at DESC</otherwise>" +
            "</choose> " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<RagDocumentInfo> selectDocumentsPage(@Param("sourceType") String sourceType,
                                              @Param("userId") String userId,
                                              @Param("keyword") String keyword,
                                              @Param("sortBy") String sortBy,
                                              @Param("sortOrder") String sortOrder,
                                              @Param("offset") Integer offset,
                                              @Param("pageSize") Integer pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM document_file " +
            "<where>" +
            "  deleted = 0 " +
            "  <if test='userId != null and userId != \"\"'> AND user_id = #{userId} </if>" +
            "  <if test='sourceType != null and sourceType != \"\"'> AND source_type = #{sourceType} </if>" +
            "  <if test='keyword != null and keyword != \"\"'> AND filename LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "</where>" +
            "</script>")
    Long countDocuments(@Param("sourceType") String sourceType,
                        @Param("userId") String userId,
                        @Param("keyword") String keyword);

    @Select("<script>" +
            "SELECT * FROM document_file WHERE deleted = 0 " +
            "<if test='userId != null and userId != \"\"'> AND user_id = #{userId} </if> " +
            "ORDER BY updated_at DESC" +
            "</script>")
    List<DocumentFile> selectAllActive(@Param("userId") String userId);
}
