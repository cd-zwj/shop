# MinIO配置说明

## 配置文件 (application.yml)

将以下配置添加到 `application.yml` 文件中：

```yaml
# MinIO配置
minio:
  endpoint: http://localhost:9000  # MinIO服务地址
  access-key: ${MINIO_ROOT_USER}   # 访问密钥
  secret-key: ${MINIO_ROOT_PASSWORD} # 密钥
  bucket-name: payment-system      # 存储桶名称
  domain: http://localhost:9000/payment-system  # 文件访问域名
  path-prefix: uploads/            # 文件路径前缀
```

## 替换说明

已将阿里云OSS替换为MinIO，主要变更：

1. **配置类**
   - 新增：`MinioConfig.java` - MinIO配置类
   - 保留：`OssConfig.java` - 如需兼容可保留

2. **工具类**
   - 新增：`MinioUtil.java` - MinIO工具类，支持分片上传
   - 保留：`OssUtil.java` - 如需兼容可保留

3. **服务层**
   - `ProductServiceImpl.java` - 已更新使用 `MinioUtil`

4. **控制器**
   - 新增：`FileUploadController.java` - 文件上传API，支持分片上传

## API接口

### 1. 简单文件上传（小文件）
```
POST /api/file/upload
参数：file (MultipartFile)
返回：文件URL
```

### 2. 分片上传
```
POST /api/file/upload-chunk
参数：
  - file: 文件分片 (MultipartFile)
  - fileId: 文件唯一ID（前端生成，可以是UUID或其他唯一标识）
  - chunkNumber: 分片编号（从1开始）
  - totalChunks: 总分片数
  - fileMd5: 完整文件的MD5值（前端计算，用于合并后校验）
返回：上传结果和进度信息
```

### 3. 获取上传进度
```
GET /api/file/upload-progress?fileId={fileId}
返回：上传进度百分比和详细信息
```

## 分片上传流程

1. **前端计算文件MD5** 作为 `fileMd5` 参数
2. **前端生成唯一fileId**（可以使用MD5或UUID）
3. **将文件分片**（建议每片5MB）
4. **循环上传每个分片**
   - 调用 `/api/file/upload-chunk`
   - 传入：file, fileId, chunkNumber, totalChunks, fileMd5
5. **后端自动合并并校验**
   - 当所有分片上传完成后，MinIO自动合并
   - 合并后计算文件MD5并与前端传递的fileMd5对比
   - ✅ 校验成功：删除临时分片文件，上传完成
   - ❌ 校验失败：删除合并文件，抛出异常
6. **获取最终文件URL**
   - 格式：`{domain}/{pathPrefix}{fileId}`
   - 例如：`http://localhost:9000/payment-system/uploads/abc123def456...`

## 前端示例代码

```javascript
// 1. 计算文件MD5（使用spark-md5库）
async function calculateMD5(file) {
  return new Promise((resolve, reject) => {
    const blobSlice = File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice;
    const chunkSize = 2097152; // 2MB
    const chunks = Math.ceil(file.size / chunkSize);
    let currentChunk = 0;
    const spark = new SparkMD5.ArrayBuffer();
    const fileReader = new FileReader();

    fileReader.onload = function (e) {
      spark.append(e.target.result);
      currentChunk++;

      if (currentChunk < chunks) {
        loadNext();
      } else {
        resolve(spark.end());
      }
    };

    fileReader.onerror = function () {
      reject('MD5计算失败');
    };

    function loadNext() {
      const start = currentChunk * chunkSize;
      const end = Math.min(start + chunkSize, file.size);
      fileReader.readAsArrayBuffer(blobSlice.call(file, start, end));
    }

    loadNext();
  });
}

// 2. 分片上传
async function uploadFileInChunks(file) {
  const chunkSize = 5 * 1024 * 1024; // 5MB per chunk
  const totalChunks = Math.ceil(file.size / chunkSize);
  
  // 计算文件MD5
  const fileMd5 = await calculateMD5(file);
  
  // 生成唯一fileId（可以使用MD5或UUID）
  const fileId = fileMd5; // 或者使用 UUID.v4()
  
  for (let i = 0; i < totalChunks; i++) {
    const start = i * chunkSize;
    const end = Math.min(start + chunkSize, file.size);
    const chunk = file.slice(start, end);
    
    const formData = new FormData();
    formData.append('file', chunk);
    formData.append('fileId', fileId);
    formData.append('chunkNumber', i + 1);
    formData.append('totalChunks', totalChunks);
    formData.append('fileMd5', fileMd5); // 传递完整文件的MD5
    
    const response = await fetch('/api/file/upload-chunk', {
      method: 'POST',
      body: formData
    });
    
    const result = await response.json();
    
    if (!result.success) {
      throw new Error(result.message);
    }
    
    console.log(`上传进度: ${result.data.uploadedChunks.length}/${totalChunks}`);
  }
  
  // 返回最终文件URL
  return `http://localhost:9000/payment-system/uploads/${fileId}`;
}

// 3. 查询上传进度
async function checkProgress(fileId) {
  const response = await fetch(`/api/file/upload-progress?fileId=${fileId}`);
  const result = await response.json();
  console.log(`上传进度: ${result.data.progress}%`);
  return result.data;
}

// 4. 使用示例
const fileInput = document.getElementById('fileInput');
fileInput.addEventListener('change', async (e) => {
  const file = e.target.files[0];
  try {
    const fileUrl = await uploadFileInChunks(file);
    console.log('文件上传成功，URL:', fileUrl);
  } catch (error) {
    console.error('文件上传失败:', error.message);
  }
});
```

## MinIO部署

### Docker部署
```bash
docker run -d \
  -p 9000:9000 \
  -p 9001:9001 \
  --name minio \
  -e "MINIO_ROOT_USER=<your-minio-user>" \
  -e "MINIO_ROOT_PASSWORD=<your-minio-password>" \
  -v /data/minio:/data \
  minio/minio server /data --console-address ":9001"
```

### 访问MinIO控制台
- 地址：http://localhost:9001
- 用户名：使用 `MINIO_ROOT_USER` 中配置的值
- 密码：使用 `MINIO_ROOT_PASSWORD` 中配置的值

## 注意事项

1. **文件MD5计算**：前端使用spark-md5库计算MD5，后端在合并后校验
2. **MD5校验流程**：
   - 前端计算完整文件MD5并传递给后端
   - 后端保存MD5到Redis
   - 所有分片上传完成后，后端合并文件
   - 后端下载合并后的文件并计算MD5
   - 对比前端传递的MD5和后端计算的MD5
   - 校验失败则删除文件并抛出异常
3. **分片大小**：建议5MB-10MB，根据网络情况调整
4. **Redis依赖**：分片上传使用Redis记录进度和MD5，确保Redis服务正常
5. **Bucket权限**：确保MinIO bucket设置为public或配置正确的访问策略
6. **文件过期**：Redis中的上传进度记录1天后自动过期
7. **并发上传**：支持多个分片并发上传，提高上传速度
8. **前端库依赖**：需要安装spark-md5库用于MD5计算
   ```bash
   npm install spark-md5
   ```
