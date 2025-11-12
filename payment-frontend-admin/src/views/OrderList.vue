<template>
  <div class="order-list">
    <h2 class="page-title">订单管理</h2>
    
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="订单号">
          <el-input 
            v-model="searchForm.orderNo" 
            placeholder="请输入订单号"
            clearable
          />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input 
            v-model="searchForm.userName" 
            placeholder="请输入用户名"
            clearable
          />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.orderStatus" placeholder="全部" clearable>
            <el-option label="待支付" :value="0" />
            <el-option label="待发货" :value="1" />
            <el-option label="已发货" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
            <el-option label="退款中" :value="5" />
            <el-option label="已退款" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 订单列表 -->
    <el-card class="table-card">
      <el-table :data="orderList" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column prop="userName" label="用户" width="120" />
        <el-table-column prop="totalAmount" label="订单金额" width="120">
          <template #default="{ row }">
            ¥{{ row.totalAmount }}
          </template>
        </el-table-column>
        <el-table-column prop="paidAmount" label="实付金额" width="120">
          <template #default="{ row }">
            ¥{{ row.paidAmount || row.totalAmount }}
          </template>
        </el-table-column>
        <el-table-column prop="orderStatus" label="订单状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getOrderStatusType(row.orderStatus)">
              {{ getOrderStatusText(row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payStatus" label="支付状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.payStatus === 1 ? 'success' : 'info'">
              {{ row.payStatus === 1 ? '已支付' : '未支付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="180" />
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="viewDetail(row.orderNo)"
            >
              查看详情
            </el-button>
            <el-button 
              v-if="row.orderStatus === 1"
              type="success" 
              size="small" 
              @click="showShipDialog(row)"
            >
              发货
            </el-button>
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
          @size-change="fetchOrders"
          @current-change="fetchOrders"
        />
      </div>
    </el-card>
    
    <!-- 发货对话框 -->
    <el-dialog v-model="shipDialogVisible" title="订单发货" width="500px">
      <el-form :model="shipForm" :rules="shipRules" ref="shipFormRef" label-width="100px">
        <el-form-item label="物流公司" prop="expressCompany">
          <el-input v-model="shipForm.expressCompany" placeholder="请输入物流公司" />
        </el-form-item>
        <el-form-item label="物流单号" prop="expressNo">
          <el-input v-model="shipForm.expressNo" placeholder="请输入物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleShip" :loading="shipLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const loading = ref(false)
const shipLoading = ref(false)
const shipDialogVisible = ref(false)
const shipFormRef = ref()

// 搜索表单
const searchForm = reactive({
  orderNo: '',
  userName: '',
  orderStatus: null
})

// 订单列表
const orderList = ref([])

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 发货表单
const shipForm = reactive({
  orderNo: '',
  expressCompany: '',
  expressNo: ''
})

// 发货表单验证
const shipRules = {
  expressCompany: [
    { required: true, message: '请输入物流公司', trigger: 'blur' }
  ],
  expressNo: [
    { required: true, message: '请输入物流单号', trigger: 'blur' }
  ]
}

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await api.get('/order/list', {
      params: {
        orderNo: searchForm.orderNo,
        userName: searchForm.userName,
        orderStatus: searchForm.orderStatus,
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      }
    })
    if (res.data.code === 200) {
      const data = res.data.data
      orderList.value = data.records || []
      pagination.total = data.total || 0
    }
  } catch (error) {
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  fetchOrders()
}

// 重置
const handleReset = () => {
  searchForm.orderNo = ''
  searchForm.userName = ''
  searchForm.orderStatus = null
  pagination.pageNum = 1
  fetchOrders()
}

// 查看详情
const viewDetail = (orderNo) => {
  router.push(`/order/detail/${orderNo}`)
}

// 显示发货对话框
const showShipDialog = (order) => {
  shipForm.orderNo = order.orderNo
  shipForm.expressCompany = ''
  shipForm.expressNo = ''
  shipDialogVisible.value = true
}

// 发货
const handleShip = async () => {
  await shipFormRef.value.validate()
  
  shipLoading.value = true
  try {
    const res = await api.post('/order/ship', shipForm)
    if (res.data.code === 200) {
      ElMessage.success('发货成功')
      shipDialogVisible.value = false
      fetchOrders()
    } else {
      ElMessage.error(res.data.message || '发货失败')
    }
  } catch (error) {
    ElMessage.error('发货失败')
  } finally {
    shipLoading.value = false
  }
}

// 获取订单状态类型
const getOrderStatusType = (status) => {
  const typeMap = {
    0: 'info',
    1: 'warning',
    2: 'primary',
    3: 'success',
    4: 'danger',
    5: 'warning',
    6: 'info'
  }
  return typeMap[status] || 'info'
}

// 获取订单状态文本
const getOrderStatusText = (status) => {
  const textMap = {
    0: '待支付',
    1: '待发货',
    2: '已发货',
    3: '已完成',
    4: '已取消',
    5: '退款中',
    6: '已退款'
  }
  return textMap[status] || '未知'
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.order-list {
  padding: 0;
}

.page-title {
  margin: 0 0 20px 0;
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
