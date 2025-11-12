// pages/product/list.js
const productApi = require('../../api/product')

Page({
  data: {
    category: '',
    sortType: 'default',
    priceOrder: 'asc',
    products: [],
    page: 1,
    pageSize: 10,
    loading: false,
    hasMore: true
  },

  onLoad(options) {
    if (options.category) {
      this.setData({ category: options.category })
      wx.setNavigationBarTitle({
        title: options.category
      })
    }
    this.loadProducts()
  },

  onReachBottom() {
    if (!this.data.loading && this.data.hasMore) {
      this.loadProducts()
    }
  },

  onPullDownRefresh() {
    this.setData({
      products: [],
      page: 1,
      hasMore: true
    })
    this.loadProducts().then(() => {
      wx.stopPullDownRefresh()
    })
  },

  // 加载商品列表
  loadProducts() {
    this.setData({ loading: true })
    
    const params = {
      page: this.data.page,
      pageSize: this.data.pageSize
    }
    
    if (this.data.category) {
      params.category = this.data.category
    }
    
    if (this.data.sortType === 'sales') {
      params.sortBy = 'sales'
      params.sortOrder = 'desc'
    } else if (this.data.sortType === 'price') {
      params.sortBy = 'price'
      params.sortOrder = this.data.priceOrder
    }
    
    return productApi.getProductList(params).then(res => {
      const products = this.data.products.concat(res.records || [])
      this.setData({
        products,
        page: this.data.page + 1,
        hasMore: res.records && res.records.length >= this.data.pageSize,
        loading: false
      })
    }).catch(err => {
      this.setData({ loading: false })
      console.error('加载商品失败', err)
    })
  },

  // 切换排序
  changeSort(e) {
    const type = e.currentTarget.dataset.type
    let priceOrder = this.data.priceOrder
    
    if (type === 'price') {
      // 切换价格排序
      priceOrder = priceOrder === 'asc' ? 'desc' : 'asc'
    }
    
    this.setData({
      sortType: type,
      priceOrder,
      products: [],
      page: 1,
      hasMore: true
    })
    
    this.loadProducts()
  },

  // 跳转到商品详情
  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/product/detail?id=${id}`
    })
  }
})
