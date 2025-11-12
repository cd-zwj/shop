// pages/user/points.js
const userApi = require('../../api/user')

Page({
  data: {
    points: 0,
    logs: [],
    page: 1,
    pageSize: 20,
    loading: false,
    hasMore: true
  },

  onLoad() {
    this.loadPoints()
    this.loadLogs()
  },

  onReachBottom() {
    if (!this.data.loading && this.data.hasMore) {
      this.loadLogs()
    }
  },

  // 加载积分余额
  loadPoints() {
    userApi.getUserPoints().then(res => {
      this.setData({ points: res.points || 0 })
    }).catch(err => {
      console.error('加载积分失败', err)
    })
  },

  // 加载积分明细
  loadLogs() {
    this.setData({ loading: true })
    
    userApi.getPointsLogs({
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
      console.error('加载积分明细失败', err)
    })
  }
})
