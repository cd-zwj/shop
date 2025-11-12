<template>
  <div class="home-container">
    <el-card>
      <h2>欢迎使用支付系统</h2>
      <el-button type="primary" @click="$router.push('/order/create')">创建订单</el-button>
      <el-button @click="$router.push('/order/list')">我的订单</el-button>
    </el-card>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home-container {
  padding: 20px;
}
</style>

