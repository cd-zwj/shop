// pages/payment/result.js
const orderApi = require('../../api/order')

Page({
  data: {
    orderNo: '',
    success: false,
    order: {}
  },

  onLoad(options) {
    const success = options.success === 'true'
    this.setData({
      orderNo: options.orderNo,
      success
    })
    
    if (success && options.orderNo) {
      this.loadOrderDetail()
    }
  },

  // 加载订单详情
  loadOrderDetail() {
    orderApi.getOrderDetail(this.data.orderNo).then(res => {
      this.setData({ order: res })
    }).catch(err => {
      console.error('加载订单详情失败', err)
    })
  },

  // 返回首页
  goToHome() {
    wx.switchTab({
      url: '/pages/index/index'
    })
  },

  // 查看订单
  goToOrders() {
    wx.switchTab({
      url: '/pages/order/list'
    })
  }
})
