<template>
  <div class="merchant-detail">
    <el-page-header @back="goBack" content="商家详情" />
    
    <el-card class="info-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>基本信息</span>
          <div>
            <el-tag :type="merchant.status === 1 ? 'success' : 'danger'" size="large">
              {{ merchant.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </div>
        </div>
      </template>
      
      <el-descriptions :column="2" border>
        <el-descriptions-item label="商家ID">{{ merchant.id }}</el-descriptions-item>
        <el-descriptions-item label="租户编码">{{ merchant.tenantCode }}</el-descriptions-item>
        <el-descriptions-item label="商家名称">{{ merchant.name }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ merchant.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ merchant.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="入驻时间">{{ merchant.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
    
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card shadow="hover">
          <el-statistic title="商品数量" :value="stats.productCount">
            <template #suffix>个</template>
          </el-statistic>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover">
          <el-statistic title="订单数量" :value="stats.orderCount">
            <template #suffix>笔</template>
          </el-statistic>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover">
          <el-statistic title="累计销售额" :value="stats.totalSales" :precision="2">
            <template #prefix>¥</template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>
    
    <el-card class="balance-card">
      <template #header>
        <div class="card-header">
          <span>账户余额</span>
        </div>
      </template>
      
      <el-descriptions :column="2" border>
        <el-descriptions-item label="可用余额">
          <span class="amount">¥{{ balance.balance }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="冻结余额">
          <span class="amount">¥{{ balance.frozenBalance }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="累计收入">
          <span class="amount">¥{{ balance.totalIncome }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="累计提现">
          <span class="amount">¥{{ balance.totalWithdrawal }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
    
    <el-card class="actions-card">
      <template #header>
        <div class="card-header">
          <span>操作</span>
        </div>
      </template>
      
      <div class="action-buttons">
        <el-button 
          v-if="merchant.status === 1" 
          type="warning" 
          @click="handleDisable"
        >
          禁用商家
        </el-button>
        <el-button 
          v-else 
          type="success" 
          @click="handleEnable"
        >
          启用商家
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { useAdminStore } from '../stores/admin'

const router = useRouter()
const route = useRoute()
const adminStore = useAdminStore()
const loading = ref(false)

const merchant = reactive({
  id: '',
  tenantCode: '',
  name: '',
  contactName: '',
  contactPhone: '',
  createTime: '',
  status: 1
})

const stats = reactive({
  productCount: 0,
  orderCount: 0,
  totalSales: 0
})

const balance = reactive({
  balance: '0.00',
  frozenBalance: '0.00',
  totalIncome: '0.00',
  totalWithdrawal: '0.00'
})

const goBack = () => {
  router.push('/admin/merchants')
}

const fetchMerchantDetail = async () => {
  loading.value = true
  try {
    const res = await api.get(`/admin/merchant/${route.params.id}`, {
      headers: {
        'Authorization': `Bearer ${adminStore.token}`
      }
    })
    
    if (res.data.code === 200) {
      const data = res.data.data
      Object.assign(merchant, data.merchant)
      Object.assign(stats, data.stats)
      Object.assign(balance, data.balance)
    }
  } catch (error) {
    ElMessage.error('获取商家详情失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const handleEnable = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要启用商家"${merchant.name}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res = await api.put(`/admin/merchant/${merchant.id}/enable`, null, {
      headers: {
        'Authorization': `Bearer ${adminStore.token}`
      }
    })
    
    if (res.data.code === 200) {
      ElMessage.success('启用成功')
      fetchMerchantDetail()
    } else {
      ElMessage.error(res.data.message || '启用失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('启用失败：' + (error.message || '未知错误'))
    }
  }
}

const handleDisable = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要禁用商家"${merchant.name}"吗？禁用后该商家将无法登录系统。`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res = await api.put(`/admin/merchant/${merchant.id}/disable`, null, {
      headers: {
        'Authorization': `Bearer ${adminStore.token}`
      }
    })
    
    if (res.data.code === 200) {
      ElMessage.success('禁用成功')
      fetchMerchantDetail()
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
  fetchMerchantDetail()
})
</script>

<style scoped>
.merchant-detail {
  padding: 20px;
}

.info-card {
  margin: 20px 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 16px;
}

.stats-row {
  margin: 20px 0;
}

.balance-card {
  margin: 20px 0;
}

.amount {
  font-size: 18px;
  font-weight: bold;
  color: #409EFF;
}

.actions-card {
  margin: 20px 0;
}

.action-buttons {
  display: flex;
  gap: 10px;
}
</style>
