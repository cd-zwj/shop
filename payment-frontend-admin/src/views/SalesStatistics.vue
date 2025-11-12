<template>
  <div class="sales-statistics">
    <h2 class="page-title">销售数据</h2>
    
    <!-- 数据概览 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #409eff;">
              <el-icon :size="30"><Money /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">今日销售额</div>
              <div class="stat-value">¥{{ overview.todaySales || '0.00' }}</div>
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
              <div class="stat-value">{{ overview.todayOrders || 0 }}</div>
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
              <div class="stat-value">¥{{ overview.monthSales || '0.00' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #f56c6c;">
              <el-icon :size="30"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">本月订单数</div>
              <div class="stat-value">{{ overview.monthOrders || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 时间范围选择 -->
    <el-card class="filter-card">
      <el-form :inline="true">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchChartData">查询</el-button>
          <el-button @click="handleExport" :loading="exportLoading">
            <el-icon><Download /></el-icon>
            <span>导出报表</span>
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 销售趋势图 -->
    <el-card class="chart-card">
      <template #header>
        <span>销售趋势</span>
      </template>
      <div ref="salesChartRef" style="width: 100%; height: 400px;"></div>
    </el-card>
    
    <!-- 商品销售排行 -->
    <el-card class="ranking-card">
      <template #header>
        <span>商品销售排行 TOP 10</span>
      </template>
      <el-table :data="topProducts" v-loading="rankingLoading" stripe>
        <el-table-column type="index" label="排名" width="80" />
        <el-table-column label="商品图片" width="100">
          <template #default="{ row }">
            <el-image 
              :src="row.productImage" 
              fit="cover"
              style="width: 60px; height: 60px; border-radius: 4px;"
            />
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名称" min-width="200" />
        <el-table-column prop="productCode" label="商品编码" width="150" />
        <el-table-column prop="salesQuantity" label="销售数量" width="120" />
        <el-table-column prop="salesAmount" label="销售金额" width="150">
          <template #default="{ row }">
            ¥{{ row.salesAmount }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import api from '../api'

const salesChartRef = ref()
let salesChart = null
const exportLoading = ref(false)
const rankingLoading = ref(false)

// 数据概览
const overview = ref({
  todaySales: '0.00',
  todayOrders: 0,
  monthSales: '0.00',
  monthOrders: 0
})

// 日期范围
const dateRange = ref([])

// 商品排行
const topProducts = ref([])

// 获取数据概览
const fetchOverview = async () => {
  try {
    const res = await api.get('/sales/statistics/overview')
    if (res.data.code === 200) {
      overview.value = res.data.data
    }
  } catch (error) {
    console.error('获取概览数据失败', error)
  }
}

// 获取图表数据
const fetchChartData = async () => {
  try {
    const params = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    
    const res = await api.get('/sales/statistics/trend', { params })
    if (res.data.code === 200) {
      const data = res.data.data
      renderSalesChart(data)
    }
  } catch (error) {
    ElMessage.error('获取图表数据失败')
  }
}

// 获取商品排行
const fetchTopProducts = async () => {
  rankingLoading.value = true
  try {
    const params = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    
    const res = await api.get('/sales/statistics/top-products', { params })
    if (res.data.code === 200) {
      topProducts.value = res.data.data || []
    }
  } catch (error) {
    ElMessage.error('获取商品排行失败')
  } finally {
    rankingLoading.value = false
  }
}

// 渲染销售趋势图
const renderSalesChart = (data) => {
  if (!salesChart) {
    salesChart = echarts.init(salesChartRef.value)
  }
  
  const dates = data.map(item => item.date)
  const salesAmounts = data.map(item => item.salesAmount)
  const orderCounts = data.map(item => item.orderCount)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['销售额', '订单数']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates
    },
    yAxis: [
      {
        type: 'value',
        name: '销售额（元）',
        position: 'left',
        axisLabel: {
          formatter: '¥{value}'
        }
      },
      {
        type: 'value',
        name: '订单数',
        position: 'right'
      }
    ],
    series: [
      {
        name: '销售额',
        type: 'line',
        smooth: true,
        data: salesAmounts,
        yAxisIndex: 0,
        itemStyle: {
          color: '#409eff'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ])
        }
      },
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        data: orderCounts,
        yAxisIndex: 1,
        itemStyle: {
          color: '#67c23a'
        }
      }
    ]
  }
  
  salesChart.setOption(option)
}

// 日期范围变化
const handleDateChange = () => {
  fetchChartData()
  fetchTopProducts()
}

// 导出报表
const handleExport = async () => {
  exportLoading.value = true
  try {
    const params = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    
    const res = await api.get('/sales/statistics/export', {
      params,
      responseType: 'blob'
    })
    
    // 创建下载链接
    const blob = new Blob([res.data], { 
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `销售报表_${new Date().getTime()}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('报表导出成功')
  } catch (error) {
    ElMessage.error('报表导出失败')
  } finally {
    exportLoading.value = false
  }
}

// 窗口大小变化时重新渲染图表
const handleResize = () => {
  if (salesChart) {
    salesChart.resize()
  }
}

onMounted(() => {
  fetchOverview()
  fetchChartData()
  fetchTopProducts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (salesChart) {
    salesChart.dispose()
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.sales-statistics {
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
  cursor: default;
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

.filter-card {
  margin-bottom: 20px;
}

.chart-card {
  margin-bottom: 20px;
}

.ranking-card {
  margin-bottom: 20px;
}
</style>
