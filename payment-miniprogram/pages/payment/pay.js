// pages/payment/pay.js
const orderApi = require('../../api/order')
const paymentApi = require('../../api/payment')

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
      this.setData({ order: res })
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

  // 发起支付
  pay() {
    wx.showLoading({ title: '支付中...' })
    
    // 创建支付订单
    paymentApi.createPayment(this.data.orderNo).then(res => {
      wx.hideLoading()
      
      // 调起微信支付
      wx.requestPayment({
        timeStamp: res.timeStamp,
        nonceStr: res.nonceStr,
        package: res.package,
        signType: res.signType,
        paySign: res.paySign,
        success: () => {
          // 支付成功
          wx.redirectTo({
            url: `/pages/payment/result?orderNo=${this.data.orderNo}&success=true`
          })
        },
        fail: (err) => {
          // 支付失败
          if (err.errMsg === 'requestPayment:fail cancel') {
            wx.showToast({
              title: '支付已取消',
              icon: 'none'
            })
          } else {
            wx.redirectTo({
              url: `/pages/payment/result?orderNo=${this.data.orderNo}&success=false`
            })
          }
        }
      })
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({
        title: err.message || '支付失败',
        icon: 'none'
      })
      console.error('创建支付订单失败', err)
    })
  },

  // 取消支付
  cancel() {
    wx.showModal({
      title: '提示',
      content: '确定取消支付吗？',
      success: res => {
        if (res.confirm) {
          wx.navigateBack()
        }
      }
    })
  }
})
