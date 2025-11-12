// pages/order/create.js
const productApi = require('../../api/product')
const orderApi = require('../../api/order')
const userApi = require('../../api/user')
const { formatMoney } = require('../../utils/util')

Page({
  data: {
    productId: null,
    product: {},
    quantity: 1,
    payMethod: 'wechat',
    balance: 0,
    useBalance: false,
    totalAmount: 0,
    balanceDeduction: 0,
    actualAmount: 0
  },

  onLoad(options) {
    if (options.productId) {
      this.setData({
        productId: options.productId,
        quantity: parseInt(options.quantity) || 1
      })
      this.loadProductDetail()
      this.loadUserBalance()
    }
  },

  // 加载商品详情
  loadProductDetail() {
    productApi.getProductDetail(this.data.productId).then(res => {
      this.setData({ product: res })
      this.calculateAmount()
    }).catch(err => {
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
      console.error('加载商品详情失败', err)
    })
  },

  // 加载用户余额
  loadUserBalance() {
    userApi.getUserBalance().then(res => {
      this.setData({ balance: res.balance || 0 })
      this.calculateAmount()
    }).catch(err => {
      console.error('加载余额失败', err)
    })
  },

  // 增加数量
  increaseQuantity() {
    const quantity = this.data.quantity + 1
    if (quantity > this.data.product.stock) {
      wx.showToast({
        title: '库存不足',
        icon: 'none'
      })
      return
    }
    this.setData({ quantity })
    this.calculateAmount()
  },

  // 减少数量
  decreaseQuantity() {
    const quantity = this.data.quantity - 1
    if (quantity < 1) {
      return
    }
    this.setData({ quantity })
    this.calculateAmount()
  },

  // 选择支付方式
  selectPayMethod(e) {
    const method = e.currentTarget.dataset.method
    this.setData({
      payMethod: method,
      useBalance: method === 'balance'
    })
    this.calculateAmount()
  },

  // 计算金额
  calculateAmount() {
    const totalAmount = parseFloat((this.data.product.price * this.data.quantity).toFixed(2))
    let balanceDeduction = 0
    let actualAmount = totalAmount
    
    if (this.data.useBalance && this.data.balance > 0) {
      balanceDeduction = Math.min(this.data.balance, totalAmount)
      actualAmount = totalAmount - balanceDeduction
    }
    
    this.setData({
      totalAmount: formatMoney(totalAmount),
      balanceDeduction: formatMoney(balanceDeduction),
      actualAmount: formatMoney(actualAmount)
    })
  },

  // 提交订单
  submitOrder() {
    wx.showLoading({ title: '提交中...' })
    
    const orderData = {
      productId: this.data.productId,
      quantity: this.data.quantity,
      payMethod: this.data.payMethod,
      useBalance: this.data.useBalance
    }
    
    orderApi.createOrder(orderData).then(res => {
      wx.hideLoading()
      
      // 跳转到支付页面
      wx.redirectTo({
        url: `/pages/payment/pay?orderNo=${res.orderNo}`
      })
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({
        title: err.message || '提交失败',
        icon: 'none'
      })
      console.error('提交订单失败', err)
    })
  }
})
