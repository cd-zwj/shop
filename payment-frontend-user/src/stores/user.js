import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  
  const login = async (loginData) => {
    const res = await api.post('/user/login', loginData)
    if (res.data.code === 200) {
      token.value = res.data.data
      localStorage.setItem('token', res.data.data)
      return true
    }
    return false
  }
  
  const logout = () => {
    token.value = ''
    username.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
  }
  
  return { token, username, login, logout }
})

