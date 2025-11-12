<template>
  <router-view v-if="$route.path === '/login' || $route.path === '/admin/login'" />
  <router-view v-else-if="$route.path.startsWith('/admin')" />
  <el-container v-else class="main-container">
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <h3>商家管理系统</h3>
      </div>
      <el-menu router :default-active="$route.path" class="menu">
        <el-menu-item index="/dashboard">
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/product/list">
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/order/list">
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/withdrawal/list">
          <span>提现管理</span>
        </el-menu-item>
        <el-menu-item index="/sales/statistics">
          <span>销售数据</span>
        </el-menu-item>
        <el-menu-item index="/marketing/manage">
          <span>营销管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-content">
          <h3>{{ merchantName }}</h3>
          <div class="user-info">
            <span>{{ username }}</span>
            <el-button @click="logout" type="primary" size="small">退出</el-button>
          </div>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from './stores/user'

const router = useRouter()
const userStore = useUserStore()

const username = computed(() => userStore.username || '商家用户')
const merchantName = computed(() => userStore.merchantName || '商家管理系统')

const logout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.main-container {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  color: #fff;
}

.logo {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid #4a5568;
}

.logo h3 {
  color: #fff;
  margin: 0;
  font-size: 18px;
}

.menu {
  border: none;
  background-color: #304156;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.header-content h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-info span {
  color: #606266;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>

