<template>
  <div class="user-manage-container">
    <el-card>
      <h2>用户管理</h2>
      <el-table :data="userList" v-loading="loading">
        <el-table-column prop="id" label="ID" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="userType" label="用户类型">
          <template #default="{ row }">
            <el-tag v-if="row.userType === 1">普通用户</el-tag>
            <el-tag type="warning" v-else>管理员</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success">启用</el-tag>
            <el-tag v-else type="danger">禁用</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const loading = ref(false)
const userList = ref([])

onMounted(() => {
  // 加载用户列表
})
</script>

<style scoped>
.user-manage-container {
  padding: 20px;
}
</style>

