// pages/index/index.js
const productApi = require('../../api/product')

Page({
  data: {
    banners: [
      '/images/banner1.jpg',
      '/images/banner2.jpg',
      '/images/banner3.jpg'
    ],
    categories: [
      { id: 1, name: '食品', icon: '/images/category-food.png' },
      { id: 2, name: '饮料', icon: '/images/category-drink.png' },
      { id: 3, name: '日用', icon: '/images/category-daily.png' },
      { id: 4, name: '其他', icon: '/images/category-other.png' }
    ],
    products: [],
    page: 1,
    pageSize: 10,
    loading: false,
    hasMore: true
  },

  onLoad() {
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
    
    return productApi.getProductList({
      page: this.data.page,
      pageSize: this.data.pageSize
    }).then(res => {
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

  // 跳转到搜索页面
  goToSearch() {
    wx.navigateTo({
      url: '/pages/product/search'
    })
  },

  // 跳转到分类页面
  goToCategory(e) {
    const category = e.currentTarget.dataset.category
    wx.navigateTo({
      url: `/pages/product/list?category=${category}`
    })
  },

  // 跳转到商品详情
  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/product/detail?id=${id}`
    })
  }
})
