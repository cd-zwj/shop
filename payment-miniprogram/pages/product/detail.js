// pages/product/detail.js
const productApi = require('../../api/product')

Page({
  data: {
    id: null,
    product: {}
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ id: options.id })
      this.loadProductDetail()
    }
  },

  // 加载商品详情
  loadProductDetail() {
    wx.showLoading({ title: '加载中...' })
    
    productApi.getProductDetail(this.data.id).then(res => {
      this.setData({ product: res })
      wx.hideLoading()
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
      console.error('加载商品详情失败', err)
    })
  },

  // 立即购买
  buyNow() {
    const app = getApp()
    
    // 检查登录状态
    if (!app.checkLogin()) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      setTimeout(() => {
        wx.switchTab({
          url: '/pages/user/index'
        })
      }, 1500)
      return
    }
    
    // 跳转到订单创建页面
    wx.navigateTo({
      url: `/pages/order/create?productId=${this.data.id}&quantity=1`
    })
  },

  // 返回首页
  goToHome() {
    wx.switchTab({
      url: '/pages/index/index'
    })
  }
})
