<template>
  <div class="product-list">
    <div class="page-header">
      <h2>商品管理</h2>
      <el-button type="primary" @click="$router.push('/product/create')">
        <el-icon><Plus /></el-icon>
        <span>上架商品</span>
      </el-button>
    </div>
    
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input 
            v-model="searchForm.keyword" 
            placeholder="商品名称或编码"
            clearable
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-input 
            v-model="searchForm.category" 
            placeholder="商品分类"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 商品列表 -->
    <el-card class="table-card">
      <el-table :data="productList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="商品图片" width="100">
          <template #default="{ row }">
            <el-image 
              :src="row.imageUrl || '/placeholder.png'" 
              fit="cover"
              style="width: 60px; height: 60px; border-radius: 4px;"
              :preview-src-list="[row.imageUrl]"
            />
          </template>
        </el-table-column>
        <el-table-column prop="productCode" label="商品编码" width="150" />
        <el-table-column prop="name" label="商品名称" min-width="200" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            ¥{{ row.price }}
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="editProduct(row.id)"
            >
              编辑
            </el-button>
            <el-button 
              :type="row.status === 1 ? 'warning' : 'success'" 
              size="small" 
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-popconfirm
              title="确定删除该商品吗？"
              @confirm="deleteProduct(row.id)"
            >
              <template #reference>
                <el-button type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchProducts"
          @current-change="fetchProducts"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const loading = ref(false)

// 搜索表单
const searchForm = reactive({
  keyword: '',
  category: '',
  status: null
})

// 商品列表
const productList = ref([])

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 获取商品列表
const fetchProducts = async () => {
  loading.value = true
  try {
    const res = await api.get('/product/list', {
      params: {
        keyword: searchForm.keyword,
        category: searchForm.category,
        status: searchForm.status,
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      }
    })
    if (res.data.code === 200) {
      const data = res.data.data
      productList.value = data.records || []
      pagination.total = data.total || 0
    }
  } catch (error) {
    ElMessage.error('获取商品列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  fetchProducts()
}

// 重置
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.category = ''
  searchForm.status = null
  pagination.pageNum = 1
  fetchProducts()
}

// 编辑商品
const editProduct = (id) => {
  router.push(`/product/edit/${id}`)
}

// 切换状态
const toggleStatus = async (product) => {
  try {
    const newStatus = product.status === 1 ? 0 : 1
    const res = await api.put(`/product/update/${product.id}`, {
      ...product,
      status: newStatus
    })
    if (res.data.code === 200) {
      ElMessage.success(newStatus === 1 ? '上架成功' : '下架成功')
      fetchProducts()
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 删除商品
const deleteProduct = async (id) => {
  try {
    const res = await api.delete(`/product/delete/${id}`)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      fetchProducts()
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  fetchProducts()
})
</script>

<style scoped>
.product-list {
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

.search-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
