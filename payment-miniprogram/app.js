// app.js
App({
  globalData: {
    userInfo: null,
    token: null,
    tenantId: null,
    apiBase: 'http://localhost:8080/api'
  },

  onLaunch() {
    // 检查登录状态
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')
    
    if (token && userInfo) {
      this.globalData.token = token
      this.globalData.userInfo = userInfo
      this.globalData.tenantId = userInfo.tenantId
    }
  },

  // 用户登录
  login() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: res => {
          if (res.code) {
            // 发送 res.code 到后台换取 openId, sessionKey, unionId
            wx.request({
              url: `${this.globalData.apiBase}/miniprogram/login`,
              method: 'POST',
              data: {
                code: res.code
              },
              success: response => {
                if (response.data.code === 200) {
                  const { token, userInfo } = response.data.data
                  
                  // 保存登录信息
                  this.globalData.token = token
                  this.globalData.userInfo = userInfo
                  this.globalData.tenantId = userInfo.tenantId
                  
                  wx.setStorageSync('token', token)
                  wx.setStorageSync('userInfo', userInfo)
                  
                  resolve(response.data.data)
                } else {
                  reject(response.data.message)
                }
              },
              fail: err => {
                reject(err)
              }
            })
          } else {
            reject('登录失败：' + res.errMsg)
          }
        },
        fail: err => {
          reject(err)
        }
      })
    })
  },

  // 获取用户信息
  getUserInfo() {
    return new Promise((resolve, reject) => {
      wx.getUserProfile({
        desc: '用于完善用户资料',
        success: res => {
          this.globalData.userInfo = res.userInfo
          wx.setStorageSync('userInfo', res.userInfo)
          resolve(res.userInfo)
        },
        fail: err => {
          reject(err)
        }
      })
    })
  },

  // 退出登录
  logout() {
    this.globalData.token = null
    this.globalData.userInfo = null
    this.globalData.tenantId = null
    
    wx.removeStorageSync('token')
    wx.removeStorageSync('userInfo')
  },

  // 检查登录状态
  checkLogin() {
    return !!this.globalData.token
  }
})
