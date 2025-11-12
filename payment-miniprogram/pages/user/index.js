// pages/user/index.js
const userApi = require('../../api/user')

Page({
  data: {
    isLogin: false,
    userInfo: {},
    points: 0,
    balance: 0
  },

  onShow() {
    this.checkLogin()
    if (this.data.isLogin) {
      this.loadUserData()
    }
  },

  // 检查登录状态
  checkLogin() {
    const app = getApp()
    const isLogin = app.checkLogin()
    this.setData({
      isLogin,
      userInfo: app.globalData.userInfo || {}
    })
  },

  // 加载用户数据
  loadUserData() {
    // 加载积分
    userApi.getUserPoints().then(res => {
      this.setData({ points: res.points || 0 })
    }).catch(err => {
      console.error('加载积分失败', err)
    })

    // 加载余额
    userApi.getUserBalance().then(res => {
      this.setData({ balance: res.balance || 0 })
    }).catch(err => {
      console.error('加载余额失败', err)
    })
  },

  // 登录
  login() {
    const app = getApp()
    wx.showLoading({ title: '登录中...' })
    
    app.login().then(() => {
      wx.hideLoading()
      wx.showToast({
        title: '登录成功',
        icon: 'success'
      })
      this.checkLogin()
      this.loadUserData()
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({
        title: err.message || '登录失败',
        icon: 'none'
      })
    })
  },

  // 退出登录
  logout() {
    wx.showModal({
      title: '提示',
      content: '确定退出登录吗？',
      success: res => {
        if (res.confirm) {
          const app = getApp()
          app.logout()
          this.setData({
            isLogin: false,
            userInfo: {},
            points: 0,
            balance: 0
          })
          wx.showToast({
            title: '已退出登录',
            icon: 'success'
          })
        }
      }
    })
  },

  // 跳转到订单列表
  goToOrders(e) {
    const status = e.currentTarget.dataset.status || ''
    wx.switchTab({
      url: '/pages/order/list'
    })
  },

  // 跳转到积分页面
  goToPoints() {
    wx.navigateTo({
      url: '/pages/user/points'
    })
  },

  // 跳转到积分兑换页面
  goToExchange() {
    wx.navigateTo({
      url: '/pages/user/exchange'
    })
  },

  // 跳转到充值页面
  goToRecharge() {
    wx.navigateTo({
      url: '/pages/user/recharge'
    })
  },

  // 跳转到余额页面
  goToBalance() {
    wx.navigateTo({
      url: '/pages/user/balance'
    })
  }
})
