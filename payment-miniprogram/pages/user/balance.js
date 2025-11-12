// pages/user/balance.js
const userApi = require('../../api/user')

Page({
  data: {
    balance: 0,
    logs: [],
    page: 1,
    pageSize: 20,
    loading: false,
    hasMore: true
  },

  onLoad() {
    this.loadBalance()
    this.loadLogs()
  },

  onShow() {
    // 从充值页面返回时刷新余额
    if (this.data.balance > 0) {
      this.loadBalance()
    }
  },

  onReachBottom() {
    if (!this.data.loading && this.data.hasMore) {
      this.loadLogs()
    }
  },

  // 加载余额
  loadBalance() {
    userApi.getUserBalance().then(res => {
      this.setData({ balance: res.balance || 0 })
    }).catch(err => {
      console.error('加载余额失败', err)
    })
  },

  // 加载余额明细
  loadLogs() {
    this.setData({ loading: true })
    
    userApi.getBalanceLogs({
      page: this.data.page,
      pageSize: this.data.pageSize
    }).then(res => {
      const logs = this.data.logs.concat(res.records || [])
      this.setData({
        logs,
        page: this.data.page + 1,
        hasMore: res.records && res.records.length >= this.data.pageSize,
        loading: false
      })
    }).catch(err => {
      this.setData({ loading: false })
      console.error('加载余额明细失败', err)
    })
  },

  // 跳转到充值页面
  goToRecharge() {
    wx.navigateTo({
      url: '/pages/user/recharge'
    })
  }
})
