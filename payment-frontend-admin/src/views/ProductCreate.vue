<template>
  <div class="product-create">
    <div class="page-header">
      <h2>上架商品</h2>
      <el-button @click="$router.back()">返回</el-button>
    </div>
    
    <el-card>
      <el-form 
        :model="form" 
        :rules="rules" 
        ref="formRef" 
        label-width="120px"
      >
        <el-form-item label="商品编码" prop="productCode">
          <el-input 
            v-model="form.productCode" 
            placeholder="请输入商品条码"
            maxlength="50"
          />
          <div class="form-tip">商品的唯一标识，用于扫码识别</div>
        </el-form-item>
        
        <el-form-item label="商品名称" prop="name">
          <el-input 
            v-model="form.name" 
            placeholder="请输入商品名称"
            maxlength="100"
          />
        </el-form-item>
        
        <el-form-item label="商品价格" prop="price">
          <el-input-number 
            v-model="form.price" 
            :min="0.01" 
            :precision="2"
            :step="0.1"
          />
          <span style="margin-left: 10px;">元</span>
        </el-form-item>
        
        <el-form-item label="商品分类" prop="category">
          <el-input 
            v-model="form.category" 
            placeholder="请输入商品分类"
            maxlength="50"
          />
        </el-form-item>
        
        <el-form-item label="商品单位" prop="unit">
          <el-input 
            v-model="form.unit" 
            placeholder="如：件、瓶、盒"
            maxlength="10"
          />
        </el-form-item>
        
        <el-form-item label="初始库存" prop="stock">
          <el-input-number 
            v-model="form.stock" 
            :min="0" 
            :step="1"
          />
        </el-form-item>
        
        <el-form-item label="商品描述" prop="description">
          <el-input 
            v-model="form.description" 
            type="textarea" 
            :rows="4"
            placeholder="请输入商品描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="商品图片" prop="imageUrl">
          <el-upload
            class="image-uploader"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :before-upload="beforeUpload"
            accept="image/*"
          >
            <img v-if="form.imageUrl" :src="form.imageUrl" class="uploaded-image" />
            <el-icon v-else class="uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="form-tip">支持jpg、png格式，大小不超过2MB</div>
        </el-form-item>
        
        <el-form-item label="商品状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">上架</el-radio>
            <el-radio :label="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="loading">
            提交
          </el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const loading = ref(false)
const formRef = ref()

// 表单数据
const form = reactive({
  productCode: '',
  name: '',
  price: null,
  category: '',
  unit: '件',
  stock: 0,
  description: '',
  imageUrl: '',
  status: 1
})

// 表单验证规则
const rules = {
  productCode: [
    { required: true, message: '请输入商品编码', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入商品价格', trigger: 'blur' }
  ]
}

// 上传地址
const uploadUrl = computed(() => 'http://localhost:8080/api/product/upload')

// 上传请求头
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))

// 上传前验证
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

// 上传成功
const handleUploadSuccess = (response) => {
  if (response.code === 200) {
    form.imageUrl = response.data
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error('图片上传失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  await formRef.value.validate()
  
  loading.value = true
  try {
    const res = await api.post('/product/create', form)
    if (res.data.code === 200) {
      ElMessage.success('商品创建成功')
      router.push('/product/list')
    } else {
      ElMessage.error(res.data.message || '创建失败')
    }
  } catch (error) {
    ElMessage.error('创建失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.product-create {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.image-uploader {
  width: 150px;
  height: 150px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.3s;
}

.image-uploader:hover {
  border-color: #409eff;
}

.uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 150px;
  height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.uploaded-image {
  width: 150px;
  height: 150px;
  object-fit: cover;
  display: block;
}
</style>
