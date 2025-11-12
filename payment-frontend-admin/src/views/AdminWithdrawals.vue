<template>
  <div class="admin-withdrawals">
    <el-page-header @back="goBack" content="提现审核" />
    
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="商家名称">
          <el-input v-model="searchForm.merchantName" placeholder="请输入商家名称" clearable />
        </el-form-item>
        
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="全部" :value="null" />
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="申请时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-card class="table-card">
      <el-table :data="withdrawalList" v-loading="loading" border stripe>
        <el-table-column prop="id" label="申请ID" width="80" />
        <el-table-column prop="merchantName" label="商家名称" width="200" />
        <el-table-column label="提现金额" width="150">
          <template #default="{ row }">
            <span class="amount">¥{{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="bankName" label="银行名称" width="150" />
        <el-table-column prop="bankAccount" label="银行账号" width="180" />
        <el-table-column prop="accountName" label="账户名" width="120" />
        <el-table-column prop="applyTime" label="申请时间" width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button type="success" size="small" @click="handleApprove(row)">
                通过
              </el-button>
              <el-button type="danger" size="small" @click="handleReject(row)">
                拒绝
              </el-button>
            </template>
            <template v-else>
              <el-button type="info" size="small" @click="viewDetail(row)">
                查看
              </el-button>
            </template>
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
    
    <!-- 拒绝对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝提现申请" width="500px">
      <el-form :model="rejectForm" :rules="rejectRules" ref="rejectFormRef" label-width="100px">
        <el-form-item label="拒绝原因" prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject" :loading="rejecting">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="提现详情" width="600px">
      <el-descriptions :column="1" border v-if="currentWithdrawal">
        <el-descriptions-item label="申请ID">{{ currentWithdrawal.id }}</el-descriptions-item>
        <el-descriptions-item label="商家名称">{{ currentWithdrawal.merchantName }}</el-descriptions-item>
        <el-descriptions-item label="提现金额">
          <span class="amount">¥{{ currentWithdrawal.amount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="银行名称">{{ currentWithdrawal.bankName }}</el-descriptions-item>
        <el-descriptions-item label="银行账号">{{ currentWithdrawal.bankAccount }}</el-descriptions-item>
        <el-descriptions-item label="账户名">{{ currentWithdrawal.accountName }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ currentWithdrawal.applyTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentWithdrawal.status)">
            {{ getStatusText(currentWithdrawal.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审核时间" v-if="currentWithdrawal.approveTime">
          {{ currentWithdrawal.approveTime }}
        </el-descriptions-item>
        <el-descriptions-item label="审核人" v-if="currentWithdrawal.approverName">
          {{ currentWithdrawal.approverName }}
        </el-descriptions-item>
        <el-descriptions-item label="拒绝原因" v-if="currentWithdrawal.rejectReason">
          {{ currentWithdrawal.rejectReason }}
        </el-descriptions-item>
      </el-descriptions>
      
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
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
const withdrawalList = ref([])
const rejectDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const rejectFormRef = ref(null)
const rejecting = ref(false)
const currentWithdrawal = ref(null)

const searchForm = reactive({
  merchantName: '',
  status: null,
  dateRange: null
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const rejectForm = reactive({
  id: null,
  reason: ''
})

const rejectRules = {
  reason: [
    { required: true, message: '请输入拒绝原因', trigger: 'blur' },
    { min: 5, message: '拒绝原因至少5个字符', trigger: 'blur' }
  ]
}

const goBack = () => {
  router.push('/admin/dashboard')
}

const getStatusType = (status) => {
  const types = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    0: '待审核',
    1: '已通过',
    2: '已拒绝'
  }
  return texts[status] || '未知'
}

const fetchWithdrawals = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      merchantName: searchForm.merchantName,
      status: searchForm.status
    }
    
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    
    const res = await api.get('/admin/withdrawals', {
      params,
      headers: {
        'Authorization': `Bearer ${adminStore.token}`
      }
    })
    
    if (res.data.code === 200) {
      withdrawalList.value = res.data.data.records
      pagination.total = res.data.data.total
    }
  } catch (error) {
    ElMessage.error('获取提现列表失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchWithdrawals()
}

const handleReset = () => {
  searchForm.merchantName = ''
  searchForm.status = null
  searchForm.dateRange = null
  pagination.current = 1
  fetchWithdrawals()
}

const handleSizeChange = () => {
  fetchWithdrawals()
}

const handleCurrentChange = () => {
  fetchWithdrawals()
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要通过商家"${row.merchantName}"的提现申请吗？提现金额：¥${row.amount}`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res = await api.put(`/admin/withdrawal/${row.id}/approve`, null, {
      headers: {
        'Authorization': `Bearer ${adminStore.token}`
      }
    })
    
    if (res.data.code === 200) {
      ElMessage.success('审核通过')
      fetchWithdrawals()
    } else {
      ElMessage.error(res.data.message || '审核失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('审核失败：' + (error.message || '未知错误'))
    }
  }
}

const handleReject = (row) => {
  rejectForm.id = row.id
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (!rejectFormRef.value) return
  
  await rejectFormRef.value.validate(async (valid) => {
    if (valid) {
      rejecting.value = true
      try {
        const res = await api.put(`/admin/withdrawal/${rejectForm.id}/reject`, {
          reason: rejectForm.reason
        }, {
          headers: {
            'Authorization': `Bearer ${adminStore.token}`
          }
        })
        
        if (res.data.code === 200) {
          ElMessage.success('已拒绝')
          rejectDialogVisible.value = false
          fetchWithdrawals()
        } else {
          ElMessage.error(res.data.message || '操作失败')
        }
      } catch (error) {
        ElMessage.error('操作失败：' + (error.message || '未知错误'))
      } finally {
        rejecting.value = false
      }
    }
  })
}

const viewDetail = (row) => {
  currentWithdrawal.value = row
  detailDialogVisible.value = true
}

onMounted(() => {
  fetchWithdrawals()
})
</script>

<style scoped>
.admin-withdrawals {
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

.amount {
  font-size: 16px;
  font-weight: bold;
  color: #E6A23C;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
