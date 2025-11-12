// pages/user/exchange.js
const userApi = require('../../api/user')

Page({
  data: {
    points: 0,
    products: [],
    loading: false
  },

  onLoad() {
    this.loadPoints()
    this.loadProducts()
  },

  // 加载积分余额
  loadPoints() {
    userApi.getUserPoints().then(res => {
      this.setData({ points: res.points || 0 })
    }).catch(err => {
      console.error('加载积分失败', err)
    })
  },

  // 加载兑换商品列表
  loadProducts() {
    this.setData({ loading: true })
    
    userApi.getExchangeProducts().then(res => {
      this.setData({
        products: res || [],
        loading: false
      })
    }).catch(err => {
      this.setData({ loading: false })
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
      console.error('加载兑换商品失败', err)
    })
  },

  // 兑换商品
  exchange(e) {
    const id = e.currentTarget.dataset.id
    const product = this.data.products.find(p => p.id === id)
    
    if (!product) return
    
    wx.showModal({
      title: '确认兑换',
      content: `确定使用${product.pointsRequired}积分兑换${product.productName}吗？`,
      success: res => {
        if (res.confirm) {
          wx.showLoading({ title: '兑换中...' })
          
          userApi.exchangeProduct(id).then(() => {
            wx.hideLoading()
            wx.showToast({
              title: '兑换成功',
              icon: 'success'
            })
            
            // 刷新数据
            this.loadPoints()
            this.loadProducts()
          }).catch(err => {
            wx.hideLoading()
            wx.showToast({
              title: err.message || '兑换失败',
              icon: 'none'
            })
          })
        }
      }
    })
  }
})
