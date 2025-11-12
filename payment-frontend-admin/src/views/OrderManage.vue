<template>
  <div class="order-manage-container">
    <el-card>
      <h2>订单管理</h2>
      <el-table :data="orderList" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" />
        <el-table-column prop="userId" label="用户ID" />
        <el-table-column prop="amount" label="金额" />
        <el-table-column prop="payType" label="支付方式" />
        <el-table-column prop="orderStatus" label="订单状态" />
        <el-table-column prop="createTime" label="创建时间" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const loading = ref(false)
const orderList = ref([])

onMounted(() => {
  // 加载订单列表
})
</script>

<style scoped>
.order-manage-container {
  padding: 20px;
}
</style>

