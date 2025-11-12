<template>
  <div class="dashboard">
    <h2 class="page-title">首页概览</h2>
    
    <!-- 数据卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #409eff;">
              <el-icon :size="30"><Money /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">今日销售额</div>
              <div class="stat-value">¥{{ todaySales }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #67c23a;">
              <el-icon :size="30"><ShoppingCart /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">今日订单数</div>
              <div class="stat-value">{{ todayOrders }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #e6a23c;">
              <el-icon :size="30"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">本月销售额</div>
              <div class="stat-value">¥{{ monthSales }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #f56c6c;">
              <el-icon :size="30"><Wallet /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">账户余额</div>
              <div class="stat-value">¥{{ balance }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 快捷操作 -->
    <el-card class="quick-actions">
      <template #header>
        <span>快捷操作</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="6">
          <el-button type="primary" @click="$router.push('/product/create')" style="width: 100%">
            <el-icon><Plus /></el-icon>
            <span>上架商品</span>
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="success" @click="$router.push('/order/list')" style="width: 100%">
            <el-icon><List /></el-icon>
            <span>订单管理</span>
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="warning" @click="$router.push('/withdrawal/list')" style="width: 100%">
            <el-icon><CreditCard /></el-icon>
            <span>申请提现</span>
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="info" @click="$router.push('/sales/statistics')" style="width: 100%">
            <el-icon><DataAnalysis /></el-icon>
            <span>销售数据</span>
          </el-button>
        </el-col>
      </el-row>
    </el-card>
    
    <!-- 待处理订单 -->
    <el-card class="pending-orders">
      <template #header>
        <div class="card-header">
          <span>待处理订单</span>
          <el-button text type="primary" @click="$router.push('/order/list')">查看全部</el-button>
        </div>
      </template>
      <el-table :data="pendingOrders" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column prop="userName" label="用户" width="120" />
        <el-table-column prop="totalAmount" label="订单金额" width="120">
          <template #default="{ row }">
            ¥{{ row.totalAmount }}
          </template>
        </el-table-column>
        <el-table-column prop="orderStatus" label="订单状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getOrderStatusType(row.orderStatus)">
              {{ getOrderStatusText(row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="180" />
        <el-table-column label="操作" fixed="right" width="150">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="viewOrder(row.orderNo)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const loading = ref(false)

// 统计数据
const todaySales = ref('0.00')
const todayOrders = ref(0)
const monthSales = ref('0.00')
const balance = ref('0.00')

// 待处理订单
const pendingOrders = ref([])

// 获取销售统计数据
const fetchStatistics = async () => {
  try {
    const res = await api.get('/sales/statistics/overview')
    if (res.data.code === 200) {
      const data = res.data.data
      todaySales.value = data.todaySales || '0.00'
      todayOrders.value = data.todayOrders || 0
      monthSales.value = data.monthSales || '0.00'
    }
  } catch (error) {
    console.error('获取统计数据失败', error)
  }
}

// 获取账户余额
const fetchBalance = async () => {
  try {
    const res = await api.get('/withdrawal/balance')
    if (res.data.code === 200) {
      balance.value = res.data.data.balance || '0.00'
    }
  } catch (error) {
    console.error('获取余额失败', error)
  }
}

// 获取待处理订单
const fetchPendingOrders = async () => {
  loading.value = true
  try {
    const res = await api.get('/order/list', {
      params: {
        orderStatus: 1, // 待发货
        pageNum: 1,
        pageSize: 10
      }
    })
    if (res.data.code === 200) {
      pendingOrders.value = res.data.data.records || []
    }
  } catch (error) {
    console.error('获取订单列表失败', error)
  } finally {
    loading.value = false
  }
}

// 查看订单详情
const viewOrder = (orderNo) => {
  router.push(`/order/detail/${orderNo}`)
}

// 获取订单状态类型
const getOrderStatusType = (status) => {
  const typeMap = {
    0: 'info',    // 待支付
    1: 'warning', // 待发货
    2: 'primary', // 已发货
    3: 'success', // 已完成
    4: 'danger',  // 已取消
    5: 'warning'  // 退款中
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
  fetchStatistics()
  fetchBalance()
  fetchPendingOrders()
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.page-title {
  margin: 0 0 20px 0;
  font-size: 24px;
  color: #303133;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.quick-actions {
  margin-bottom: 20px;
}

.pending-orders {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
