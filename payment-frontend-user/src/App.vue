<template>
  <div id="app">
    <el-container>
      <el-header>
        <div class="header-content">
          <h2>支付系统</h2>
          <div class="user-info" v-if="userStore.token">
            <span>{{ userStore.username }}</span>
            <el-button @click="logout">退出</el-button>
          </div>
          <div v-else>
            <el-button @click="$router.push('/login')">登录</el-button>
            <el-button @click="$router.push('/register')">注册</el-button>
          </div>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from './stores/user'

const router = useRouter()
const userStore = useUserStore()

const logout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>

