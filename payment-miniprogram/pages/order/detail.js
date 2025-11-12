// pages/order/detail.js
const orderApi = require('../../api/order')
const { getOrderStatusText, getOrderStatusColor } = require('../../utils/util')

Page({
  data: {
    orderNo: '',
    order: {}
  },

  onLoad(options) {
    if (options.orderNo) {
      this.setData({ orderNo: options.orderNo })
      this.loadOrderDetail()
    }
  },

  // 加载订单详情
  loadOrderDetail() {
    wx.showLoading({ title: '加载中...' })
    
    orderApi.getOrderDetail(this.data.orderNo).then(res => {
      this.setData({
        order: {
          ...res,
          statusText: getOrderStatusText(res.orderStatus),
          statusColor: getOrderStatusColor(res.orderStatus)
        }
      })
      wx.hideLoading()
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
      console.error('加载订单详情失败', err)
    })
  },

  // 取消订单
  cancelOrder() {
    wx.showModal({
      title: '提示',
      content: '确定取消订单吗？',
      success: res => {
        if (res.confirm) {
          wx.showLoading({ title: '取消中...' })
          
          orderApi.cancelOrder(this.data.orderNo).then(() => {
            wx.hideLoading()
            wx.showToast({
              title: '取消成功',
              icon: 'success'
            })
            this.loadOrderDetail()
          }).catch(err => {
            wx.hideLoading()
            wx.showToast({
              title: err.message || '取消失败',
              icon: 'none'
            })
          })
        }
      }
    })
  },

  // 去支付
  payOrder() {
    wx.navigateTo({
      url: `/pages/payment/pay?orderNo=${this.data.orderNo}`
    })
  },

  // 确认收货
  confirmOrder() {
    wx.showModal({
      title: '提示',
      content: '确认收货吗？',
      success: res => {
        if (res.confirm) {
          wx.showLoading({ title: '确认中...' })
          
          orderApi.confirmOrder(this.data.orderNo).then(() => {
            wx.hideLoading()
            wx.showToast({
              title: '确认成功',
              icon: 'success'
            })
            this.loadOrderDetail()
          }).catch(err => {
            wx.hideLoading()
            wx.showToast({
              title: err.message || '确认失败',
              icon: 'none'
            })
          })
        }
      }
    })
  }
})
