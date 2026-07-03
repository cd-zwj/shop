package com.payment.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payment.rag.model.RagUnit;
import com.payment.rag.model.dto.RagDocumentInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RagUnitMapper extends BaseMapper<RagUnit>  {

    @Select("<script>" +
            "SELECT " +
            "  source_id as sourceId, " +
            "  MAX(file_hash) as fileHash, " +
            "  MAX(filename) as filename, " +
            "  MAX(source_type) as sourceType, " +
            "  MAX(minio_url) as minioUrl, " +
            "  MAX(minio_path) as minioPath, " +
            "  COUNT(*) as chunkCount, " +
            "  MAX(created_at) as createdAt, " +
            "  MAX(updated_at) as updatedAt " +
            "FROM rag_unit " +
            "<where>" +
            "  <if test='sourceType != null'> AND source_type = #{sourceType} </if>" +
            "  <if test='keyword != null and keyword != \"\"'> AND (filename LIKE CONCAT('%', #{keyword}, '%') OR minio_path LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "</where>" +
            "GROUP BY source_id " +
            "ORDER BY " +
            "<choose>" +
            "  <when test='sortBy == \"createdAt\" and sortOrder == \"ASC\"'>MAX(created_at) ASC</when>" +
            "  <when test='sortBy == \"createdAt\" and sortOrder == \"DESC\"'>MAX(created_at) DESC</when>" +
            "  <when test='sortBy == \"updatedAt\" and sortOrder == \"ASC\"'>MAX(updated_at) ASC</when>" +
            "  <when test='sortBy == \"updatedAt\" and sortOrder == \"DESC\"'>MAX(updated_at) DESC</when>" +
            "  <otherwise>MAX(created_at) DESC</otherwise>" +
            "</choose> " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<RagDocumentInfo> selectDocumentsPage(
            @Param("sourceType") String sourceType,
            @Param("keyword") String keyword,
            @Param("sortBy") String sortBy,
            @Param("sortOrder") String sortOrder,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    @Select("<script>" +
            "SELECT COUNT(DISTINCT source_id) FROM rag_unit " +
            "<where>" +
            "  <if test='sourceType != null'> AND source_type = #{sourceType} </if>" +
            "  <if test='keyword != null and keyword != \"\"'> AND (filename LIKE CONCAT('%', #{keyword}, '%') OR minio_path LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "</where>" +
            "</script>")
    Long countDocuments(
            @Param("sourceType") String sourceType,
            @Param("keyword") String keyword
    );
}
