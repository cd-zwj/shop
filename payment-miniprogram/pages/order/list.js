// pages/order/list.js
const orderApi = require('../../api/order')
const { getOrderStatusText, getOrderStatusColor } = require('../../utils/util')

Page({
  data: {
    statusFilter: '',
    orders: [],
    page: 1,
    pageSize: 10,
    loading: false,
    hasMore: true
  },

  onLoad(options) {
    if (options.status) {
      this.setData({ statusFilter: options.status })
    }
    this.loadOrders()
  },

  onShow() {
    // 从详情页返回时刷新列表
    if (this.data.orders.length > 0) {
      this.refreshOrders()
    }
  },

  onReachBottom() {
    if (!this.data.loading && this.data.hasMore) {
      this.loadOrders()
    }
  },

  onPullDownRefresh() {
    this.refreshOrders().then(() => {
      wx.stopPullDownRefresh()
    })
  },

  // 加载订单列表
  loadOrders() {
    this.setData({ loading: true })
    
    const params = {
      page: this.data.page,
      pageSize: this.data.pageSize
    }
    
    if (this.data.statusFilter) {
      params.orderStatus = this.data.statusFilter
    }
    
    return orderApi.getOrderList(params).then(res => {
      const orders = (res.records || []).map(order => ({
        ...order,
        statusText: getOrderStatusText(order.orderStatus),
        statusColor: getOrderStatusColor(order.orderStatus)
      }))
      
      this.setData({
        orders: this.data.orders.concat(orders),
        page: this.data.page + 1,
        hasMore: orders.length >= this.data.pageSize,
        loading: false
      })
    }).catch(err => {
      this.setData({ loading: false })
      console.error('加载订单失败', err)
    })
  },

  // 刷新订单列表
  refreshOrders() {
    this.setData({
      orders: [],
      page: 1,
      hasMore: true
    })
    return this.loadOrders()
  },

  // 切换状态
  changeStatus(e) {
    const status = e.currentTarget.dataset.status
    this.setData({
      statusFilter: status,
      orders: [],
      page: 1,
      hasMore: true
    })
    this.loadOrders()
  },

  // 取消订单
  cancelOrder(e) {
    const orderNo = e.currentTarget.dataset.orderNo
    
    wx.showModal({
      title: '提示',
      content: '确定取消订单吗？',
      success: res => {
        if (res.confirm) {
          wx.showLoading({ title: '取消中...' })
          
          orderApi.cancelOrder(orderNo).then(() => {
            wx.hideLoading()
            wx.showToast({
              title: '取消成功',
              icon: 'success'
            })
            this.refreshOrders()
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
  payOrder(e) {
    const orderNo = e.currentTarget.dataset.orderNo
    wx.navigateTo({
      url: `/pages/payment/pay?orderNo=${orderNo}`
    })
  },

  // 确认收货
  confirmOrder(e) {
    const orderNo = e.currentTarget.dataset.orderNo
    
    wx.showModal({
      title: '提示',
      content: '确认收货吗？',
      success: res => {
        if (res.confirm) {
          wx.showLoading({ title: '确认中...' })
          
          orderApi.confirmOrder(orderNo).then(() => {
            wx.hideLoading()
            wx.showToast({
              title: '确认成功',
              icon: 'success'
            })
            this.refreshOrders()
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
  },

  // 查看详情
  goToDetail(e) {
    const orderNo = e.currentTarget.dataset.orderNo
    wx.navigateTo({
      url: `/pages/order/detail?orderNo=${orderNo}`
    })
  }
})
