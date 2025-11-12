<template>
  <div class="withdrawal-list">
    <h2 class="page-title">提现管理</h2>
    
    <!-- 账户余额 -->
    <el-row :gutter="20" class="balance-row">
      <el-col :span="8">
        <el-card class="balance-card">
          <div class="balance-content">
            <div class="balance-icon" style="background-color: #67c23a;">
              <el-icon :size="30"><Wallet /></el-icon>
            </div>
            <div class="balance-info">
              <div class="balance-label">可用余额</div>
              <div class="balance-value">¥{{ balance.balance || '0.00' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="balance-card">
          <div class="balance-content">
            <div class="balance-icon" style="background-color: #e6a23c;">
              <el-icon :size="30"><Lock /></el-icon>
            </div>
            <div class="balance-info">
              <div class="balance-label">冻结余额</div>
              <div class="balance-value">¥{{ balance.frozenBalance || '0.00' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="balance-card">
          <div class="balance-content">
            <div class="balance-icon" style="background-color: #409eff;">
              <el-icon :size="30"><Money /></el-icon>
            </div>
            <div class="balance-info">
              <div class="balance-label">累计收入</div>
              <div class="balance-value">¥{{ balance.totalIncome || '0.00' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 申请提现按钮 -->
    <el-card class="action-card">
      <el-button type="primary" size="large" @click="showApplyDialog">
        <el-icon><CreditCard /></el-icon>
        <span>申请提现</span>
      </el-button>
    </el-card>
    
    <!-- 提现记录 -->
    <el-card class="table-card">
      <template #header>
        <span>提现记录</span>
      </template>
      <el-table :data="withdrawalList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="amount" label="提现金额" width="120">
          <template #default="{ row }">
            ¥{{ row.amount }}
          </template>
        </el-table-column>
        <el-table-column prop="bankName" label="银行名称" width="150" />
        <el-table-column prop="bankAccount" label="银行账号" width="180" />
        <el-table-column prop="accountName" label="账户名" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="申请时间" width="180" />
        <el-table-column prop="approveTime" label="审核时间" width="180">
          <template #default="{ row }">
            {{ row.approveTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="拒绝原因" min-width="150">
          <template #default="{ row }">
            {{ row.rejectReason || '-' }}
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchWithdrawals"
          @current-change="fetchWithdrawals"
        />
      </div>
    </el-card>
    
    <!-- 申请提现对话框 -->
    <el-dialog v-model="applyDialogVisible" title="申请提现" width="500px">
      <el-form :model="applyForm" :rules="applyRules" ref="applyFormRef" label-width="100px">
        <el-form-item label="提现金额" prop="amount">
          <el-input-number 
            v-model="applyForm.amount" 
            :min="0.01" 
            :max="parseFloat(balance.balance || 0)"
            :precision="2"
            :step="100"
          />
          <span style="margin-left: 10px;">元</span>
          <div class="form-tip">可用余额：¥{{ balance.balance || '0.00' }}</div>
        </el-form-item>
        <el-form-item label="银行名称" prop="bankName">
          <el-input v-model="applyForm.bankName" placeholder="请输入银行名称" />
        </el-form-item>
        <el-form-item label="银行账号" prop="bankAccount">
          <el-input v-model="applyForm.bankAccount" placeholder="请输入银行账号" />
        </el-form-item>
        <el-form-item label="账户名" prop="accountName">
          <el-input v-model="applyForm.accountName" placeholder="请输入账户名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleApply" :loading="applyLoading">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const loading = ref(false)
const applyLoading = ref(false)
const applyDialogVisible = ref(false)
const applyFormRef = ref()

// 账户余额
const balance = ref({
  balance: '0.00',
  frozenBalance: '0.00',
  totalIncome: '0.00'
})

// 提现列表
const withdrawalList = ref([])

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 申请表单
const applyForm = reactive({
  amount: null,
  bankName: '',
  bankAccount: '',
  accountName: ''
})

// 申请表单验证
const applyRules = {
  amount: [
    { required: true, message: '请输入提现金额', trigger: 'blur' }
  ],
  bankName: [
    { required: true, message: '请输入银行名称', trigger: 'blur' }
  ],
  bankAccount: [
    { required: true, message: '请输入银行账号', trigger: 'blur' }
  ],
  accountName: [
    { required: true, message: '请输入账户名', trigger: 'blur' }
  ]
}

// 获取账户余额
const fetchBalance = async () => {
  try {
    const res = await api.get('/withdrawal/balance')
    if (res.data.code === 200) {
      balance.value = res.data.data
    }
  } catch (error) {
    console.error('获取余额失败', error)
  }
}

// 获取提现记录
const fetchWithdrawals = async () => {
  loading.value = true
  try {
    const res = await api.get('/withdrawal/list', {
      params: {
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      }
    })
    if (res.data.code === 200) {
      const data = res.data.data
      withdrawalList.value = data.records || []
      pagination.total = data.total || 0
    }
  } catch (error) {
    ElMessage.error('获取提现记录失败')
  } finally {
    loading.value = false
  }
}

// 显示申请对话框
const showApplyDialog = () => {
  if (parseFloat(balance.value.balance || 0) <= 0) {
    ElMessage.warning('可用余额不足')
    return
  }
  applyForm.amount = null
  applyForm.bankName = ''
  applyForm.bankAccount = ''
  applyForm.accountName = ''
  applyDialogVisible.value = true
}

// 提交申请
const handleApply = async () => {
  await applyFormRef.value.validate()
  
  if (applyForm.amount > parseFloat(balance.value.balance || 0)) {
    ElMessage.error('提现金额不能超过可用余额')
    return
  }
  
  applyLoading.value = true
  try {
    const res = await api.post('/withdrawal/apply', applyForm)
    if (res.data.code === 200) {
      ElMessage.success('提现申请提交成功，等待审核')
      applyDialogVisible.value = false
      fetchBalance()
      fetchWithdrawals()
    } else {
      ElMessage.error(res.data.message || '申请失败')
    }
  } catch (error) {
    ElMessage.error('申请失败')
  } finally {
    applyLoading.value = false
  }
}

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    0: 'warning', // 待审核
    1: 'success', // 已通过
    2: 'danger'   // 已拒绝
  }
  return typeMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const textMap = {
    0: '待审核',
    1: '已通过',
    2: '已拒绝'
  }
  return textMap[status] || '未知'
}

onMounted(() => {
  fetchBalance()
  fetchWithdrawals()
})
</script>

<style scoped>
.withdrawal-list {
  padding: 0;
}

.page-title {
  margin: 0 0 20px 0;
  font-size: 24px;
  color: #303133;
}

.balance-row {
  margin-bottom: 20px;
}

.balance-card {
  cursor: default;
}

.balance-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.balance-icon {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.balance-info {
  flex: 1;
}

.balance-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.balance-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.action-card {
  margin-bottom: 20px;
  text-align: center;
}

.table-card {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>
