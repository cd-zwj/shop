<template>
  <div class="admin-dashboard">
    <el-page-header @back="goBack" content="平台数据概览" />
    
    <div class="dashboard-header">
      <h2>欢迎，{{ adminStore.username }}</h2>
      <el-button type="danger" @click="handleLogout">退出登录</el-button>
    </div>
    
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #409EFF;">
            <el-icon :size="40"><Shop /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalMerchants }}</div>
            <div class="stat-label">入驻商家总数</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #67C23A;">
            <el-icon :size="40"><CircleCheck /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.activeMerchants }}</div>
            <div class="stat-label">启用商家数</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #E6A23C;">
            <el-icon :size="40"><Money /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">¥{{ stats.totalSales }}</div>
            <div class="stat-label">平台总销售额</div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #F56C6C;">
            <el-icon :size="40"><DocumentChecked /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.pendingWithdrawals }}</div>
            <div class="stat-label">待审核提现</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>商家注册趋势</span>
            </div>
          </template>
          <div ref="merchantChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>平台销售趋势</span>
            </div>
          </template>
          <div ref="salesChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="quick-actions">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>快捷操作</span>
            </div>
          </template>
          <div class="action-buttons">
            <el-button type="primary" @click="goToMerchants">商家管理</el-button>
            <el-button type="warning" @click="goToWithdrawals">提现审核</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Shop, CircleCheck, Money, DocumentChecked } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import api from '../api'
import { useAdminStore } from '../stores/admin'

const router = useRouter()
const adminStore = useAdminStore()
const merchantChartRef = ref(null)
const salesChartRef = ref(null)

const stats = ref({
  totalMerchants: 0,
  activeMerchants: 0,
  totalSales: '0.00',
  pendingWithdrawals: 0
})

const goBack = () => {
  router.back()
}

const handleLogout = () => {
  adminStore.logout()
  ElMessage.success('已退出登录')
  router.push('/admin/login')
}

const goToMerchants = () => {
  router.push('/admin/merchants')
}

const goToWithdrawals = () => {
  router.push('/admin/withdrawals')
}

const fetchStats = async () => {
  try {
    const res = await api.get('/admin/dashboard/stats', {
      headers: {
        'Authorization': `Bearer ${adminStore.token}`
      }
    })
    if (res.data.code === 200) {
      stats.value = res.data.data
    }
  } catch (error) {
    console.error('获取统计数据失败', error)
  }
}

const initMerchantChart = async () => {
  try {
    const res = await api.get('/admin/dashboard/merchant-trend', {
      headers: {
        'Authorization': `Bearer ${adminStore.token}`
      }
    })
    
    if (res.data.code === 200 && merchantChartRef.value) {
      const chart = echarts.init(merchantChartRef.value)
      const data = res.data.data
      
      chart.setOption({
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          data: data.dates || []
        },
        yAxis: {
          type: 'value'
        },
        series: [{
          name: '新增商家',
          type: 'line',
          data: data.counts || [],
          smooth: true,
          itemStyle: {
            color: '#409EFF'
          }
        }]
      })
    }
  } catch (error) {
    console.error('加载商家趋势图失败', error)
  }
}

const initSalesChart = async () => {
  try {
    const res = await api.get('/admin/dashboard/sales-trend', {
      headers: {
        'Authorization': `Bearer ${adminStore.token}`
      }
    })
    
    if (res.data.code === 200 && salesChartRef.value) {
      const chart = echarts.init(salesChartRef.value)
      const data = res.data.data
      
      chart.setOption({
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          data: data.dates || []
        },
        yAxis: {
          type: 'value'
        },
        series: [{
          name: '销售额',
          type: 'bar',
          data: data.amounts || [],
          itemStyle: {
            color: '#67C23A'
          }
        }]
      })
    }
  } catch (error) {
    console.error('加载销售趋势图失败', error)
  }
}

onMounted(() => {
  fetchStats()
  initMerchantChart()
  initSalesChart()
})
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 20px 0;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
}

.stat-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-right: 20px;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.charts-row {
  margin-bottom: 20px;
}

.card-header {
  font-weight: bold;
  font-size: 16px;
}

.quick-actions {
  margin-bottom: 20px;
}

.action-buttons {
  display: flex;
  gap: 10px;
}
</style>
