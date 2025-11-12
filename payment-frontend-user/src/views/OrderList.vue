<template>
  <div class="order-list-container">
    <el-card>
      <h2>我的订单</h2>
      <el-table :data="orderList" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" />
        <el-table-column prop="amount" label="金额" />
        <el-table-column prop="payType" label="支付方式" />
        <el-table-column prop="orderStatus" label="订单状态">
          <template #default="{ row }">
            <el-tag v-if="row.orderStatus === 'PENDING'">待支付</el-tag>
            <el-tag type="success" v-else-if="row.orderStatus === 'PAID'">已支付</el-tag>
            <el-tag type="info" v-else-if="row.orderStatus === 'CANCELLED'">已取消</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button size="small" @click="viewOrder(row.orderNo)">查看</el-button>
            <el-button size="small" v-if="row.orderStatus === 'PENDING'" @click="cancelOrder(row.orderNo)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const orderList = ref([])

onMounted(() => {
  loadOrders()
})

const loadOrders = async () => {
  // 这里应该调用获取订单列表的接口
  // 由于后端没有提供，这里只是示例
  loading.value = true
  try {
    // const res = await api.get('/order/list')
    // orderList.value = res.data.data
  } catch (error) {
    ElMessage.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

const viewOrder = (orderNo) => {
  // 查看订单详情
}

const cancelOrder = async (orderNo) => {
  try {
    await api.post('/order/cancel', null, { params: { orderNo } })
    ElMessage.success('订单已取消')
    loadOrders()
  } catch (error) {
    ElMessage.error('取消订单失败')
  }
}
</script>

<style scoped>
.order-list-container {
  padding: 20px;
}
</style>

