import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const merchantName = ref(localStorage.getItem('merchantName') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const tenantId = ref(localStorage.getItem('tenantId') || '')
  
  const login = async (loginData) => {
    const res = await api.post('/user/login', loginData)
    if (res.data.code === 200) {
      token.value = res.data.data
      localStorage.setItem('token', res.data.data)
      
      // 获取用户信息
      await fetchUserInfo()
      return true
    }
    return false
  }
  
  const fetchUserInfo = async () => {
    try {
      const res = await api.get('/user/info')
      if (res.data.code === 200) {
        const userInfo = res.data.data
        username.value = userInfo.username
        userId.value = userInfo.id
        tenantId.value = userInfo.tenantId
        merchantName.value = userInfo.tenantName || '商家管理系统'
        
        localStorage.setItem('username', username.value)
        localStorage.setItem('userId', userId.value)
        localStorage.setItem('tenantId', tenantId.value)
        localStorage.setItem('merchantName', merchantName.value)
      }
    } catch (error) {
      console.error('获取用户信息失败', error)
    }
  }
  
  const logout = () => {
    token.value = ''
    username.value = ''
    merchantName.value = ''
    userId.value = ''
    tenantId.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('merchantName')
    localStorage.removeItem('userId')
    localStorage.removeItem('tenantId')
  }
  
  return { 
    token, 
    username, 
    merchantName, 
    userId, 
    tenantId, 
    login, 
    logout,
    fetchUserInfo
  }
})

