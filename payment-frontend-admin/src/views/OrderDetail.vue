<template>
  <div class="order-detail">
    <div class="page-header">
      <h2>订单详情</h2>
      <el-button @click="$router.back()">返回</el-button>
    </div>
    
    <div v-loading="loading">
      <!-- 订单状态 -->
      <el-card class="status-card">
        <el-steps :active="getStepActive(order.orderStatus)" align-center>
          <el-step title="待支付" />
          <el-step title="待发货" />
          <el-step title="已发货" />
          <el-step title="已完成" />
        </el-steps>
      </el-card>
      
      <!-- 订单信息 -->
      <el-card class="info-card">
        <template #header>
          <span>订单信息</span>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getOrderStatusType(order.orderStatus)">
              {{ getOrderStatusText(order.orderStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="用户名">{{ order.userName }}</el-descriptions-item>
          <el-descriptions-item label="用户手机">{{ order.userPhone }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">¥{{ order.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">¥{{ order.paidAmount || order.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ getPayTypeText(order.payType) }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">
            <el-tag :type="order.payStatus === 1 ? 'success' : 'info'">
              {{ order.payStatus === 1 ? '已支付' : '未支付' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ order.createTime }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ order.payTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
      
      <!-- 商品信息 -->
      <el-card class="items-card">
        <template #header>
          <span>商品信息</span>
        </template>
        <el-table :data="order.items" border>
          <el-table-column label="商品图片" width="100">
            <template #default="{ row }">
              <el-image 
                :src="row.productImage" 
                fit="cover"
                style="width: 60px; height: 60px; border-radius: 4px;"
              />
            </template>
          </el-table-column>
          <el-table-column prop="productName" label="商品名称" />
          <el-table-column prop="productCode" label="商品编码" width="150" />
          <el-table-column prop="price" label="单价" width="100">
            <template #default="{ row }">
              ¥{{ row.price }}
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计" width="120">
            <template #default="{ row }">
              ¥{{ (row.price * row.quantity).toFixed(2) }}
            </template>
          </el-table-column>
        </el-table>
      </el-card>
      
      <!-- 物流信息 -->
      <el-card v-if="order.orderStatus >= 2" class="express-card">
        <template #header>
          <span>物流信息</span>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="物流公司">{{ order.expressCompany || '-' }}</el-descriptions-item>
          <el-descriptions-item label="物流单号">{{ order.expressNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发货时间">{{ order.shipTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货地址">{{ order.address || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
      
      <!-- 操作按钮 -->
      <el-card v-if="order.orderStatus === 1" class="action-card">
        <el-button type="primary" @click="showShipDialog">发货</el-button>
      </el-card>
    </div>
    
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const shipLoading = ref(false)
const shipDialogVisible = ref(false)
const shipFormRef = ref()

const orderNo = computed(() => route.params.orderNo)

// 订单信息
const order = ref({
  items: []
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

// 获取订单详情
const fetchOrderDetail = async () => {
  loading.value = true
  try {
    const res = await api.get(`/order/detail/${orderNo.value}`)
    if (res.data.code === 200) {
      order.value = res.data.data
    }
  } catch (error) {
    ElMessage.error('获取订单详情失败')
  } finally {
    loading.value = false
  }
}

// 显示发货对话框
const showShipDialog = () => {
  shipForm.orderNo = order.value.orderNo
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
      fetchOrderDetail()
    } else {
      ElMessage.error(res.data.message || '发货失败')
    }
  } catch (error) {
    ElMessage.error('发货失败')
  } finally {
    shipLoading.value = false
  }
}

// 获取步骤激活状态
const getStepActive = (status) => {
  const stepMap = {
    0: 0, // 待支付
    1: 1, // 待发货
    2: 2, // 已发货
    3: 3, // 已完成
    4: 0, // 已取消
    5: 1, // 退款中
    6: 0  // 已退款
  }
  return stepMap[status] || 0
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

// 获取支付方式文本
const getPayTypeText = (payType) => {
  const textMap = {
    'WECHAT': '微信支付',
    'ALIPAY': '支付宝',
    'BALANCE': '余额支付',
    'MIXED': '组合支付'
  }
  return textMap[payType] || '未知'
}

onMounted(() => {
  fetchOrderDetail()
})
</script>

<style scoped>
.order-detail {
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

.status-card,
.info-card,
.items-card,
.express-card,
.action-card {
  margin-bottom: 20px;
}

.action-card {
  text-align: center;
}
</style>
