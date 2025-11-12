import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api'

export const useAdminStore = defineStore('admin', () => {
  const token = ref(localStorage.getItem('adminToken') || '')
  const username = ref(localStorage.getItem('adminUsername') || '')
  const userId = ref(localStorage.getItem('adminUserId') || '')
  const isAdmin = ref(localStorage.getItem('isAdmin') === 'true')
  
  const login = async (loginData) => {
    const res = await api.post('/admin/login', loginData)
    if (res.data.code === 200) {
      token.value = res.data.data
      localStorage.setItem('adminToken', res.data.data)
      
      // 获取管理员信息
      await fetchAdminInfo()
      return true
    }
    return false
  }
  
  const fetchAdminInfo = async () => {
    try {
      const res = await api.get('/admin/info', {
        headers: {
          'Authorization': `Bearer ${token.value}`
        }
      })
      if (res.data.code === 200) {
        const adminInfo = res.data.data
        username.value = adminInfo.username
        userId.value = adminInfo.id
        isAdmin.value = true
        
        localStorage.setItem('adminUsername', username.value)
        localStorage.setItem('adminUserId', userId.value)
        localStorage.setItem('isAdmin', 'true')
      }
    } catch (error) {
      console.error('获取管理员信息失败', error)
    }
  }
  
  const logout = () => {
    token.value = ''
    username.value = ''
    userId.value = ''
    isAdmin.value = false
    localStorage.removeItem('adminToken')
    localStorage.removeItem('adminUsername')
    localStorage.removeItem('adminUserId')
    localStorage.removeItem('isAdmin')
  }
  
  return { 
    token, 
    username, 
    userId, 
    isAdmin,
    login, 
    logout,
    fetchAdminInfo
  }
})
