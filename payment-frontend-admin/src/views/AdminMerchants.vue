<template>
  <div class="admin-merchants">
    <el-page-header @back="goBack" content="商家管理" />
    
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="商家名称">
          <el-input v-model="searchForm.name" placeholder="请输入商家名称" clearable />
        </el-form-item>
        
        <el-form-item label="联系电话">
          <el-input v-model="searchForm.phone" placeholder="请输入联系电话" clearable />
        </el-form-item>
        
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="全部" :value="null" />
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-card class="table-card">
      <el-table :data="merchantList" v-loading="loading" border stripe>
        <el-table-column prop="id" label="商家ID" width="80" />
        <el-table-column prop="tenantCode" label="租户编码" width="150" />
        <el-table-column prop="name" label="商家名称" width="200" />
        <el-table-column prop="contactName" label="联系人" width="120" />
        <el-table-column prop="contactPhone" label="联系电话" width="150" />
        <el-table-column prop="createTime" label="入驻时间" width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewDetail(row.id)">
              详情
            </el-button>
            <el-button 
              v-if="row.status === 1" 
              type="warning" 
              size="small" 
              @click="handleDisable(row)"
            >
              禁用
            </el-button>
            <el-button 
              v-else 
              type="success" 
              size="small" 
              @click="handleEnable(row)"
            >
              启用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="pagination"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { useAdminStore } from '../stores/admin'

const router = useRouter()
const adminStore = useAdminStore()
const loading = ref(false)
const merchantList = ref([])

const searchForm = reactive({
  name: '',
  phone: '',
  status: null
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const goBack = () => {
  router.push('/admin/dashboard')
}

const fetchMerchants = async () => {
  loading.value = true
  try {
    const res = await api.get('/admin/merchants', {
      params: {
        current: pagination.current,
        size: pagination.size,
        name: searchForm.name,
        phone: searchForm.phone,
        status: searchForm.status
      },
      headers: {
        'Authorization': `Bearer ${adminStore.token}`
      }
    })
    
    if (res.data.code === 200) {
      merchantList.value = res.data.data.records
      pagination.total = res.data.data.total
    }
  } catch (error) {
    ElMessage.error('获取商家列表失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchMerchants()
}

const handleReset = () => {
  searchForm.name = ''
  searchForm.phone = ''
  searchForm.status = null
  pagination.current = 1
  fetchMerchants()
}

const handleSizeChange = () => {
  fetchMerchants()
}

const handleCurrentChange = () => {
  fetchMerchants()
}

const viewDetail = (id) => {
  router.push(`/admin/merchant/${id}`)
}

const handleEnable = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要启用商家"${row.name}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res = await api.put(`/admin/merchant/${row.id}/enable`, null, {
      headers: {
        'Authorization': `Bearer ${adminStore.token}`
      }
    })
    
    if (res.data.code === 200) {
      ElMessage.success('启用成功')
      fetchMerchants()
    } else {
      ElMessage.error(res.data.message || '启用失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('启用失败：' + (error.message || '未知错误'))
    }
  }
}

const handleDisable = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要禁用商家"${row.name}"吗？禁用后该商家将无法登录系统。`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res = await api.put(`/admin/merchant/${row.id}/disable`, null, {
      headers: {
        'Authorization': `Bearer ${adminStore.token}`
      }
    })
    
    if (res.data.code === 200) {
      ElMessage.success('禁用成功')
      fetchMerchants()
    } else {
      ElMessage.error(res.data.message || '禁用失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('禁用失败：' + (error.message || '未知错误'))
    }
  }
}

onMounted(() => {
  fetchMerchants()
})
</script>

<style scoped>
.admin-merchants {
  padding: 20px;
}

.search-card {
  margin: 20px 0;
}

.search-form {
  margin-bottom: 0;
}

.table-card {
  margin-top: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
