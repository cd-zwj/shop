// pages/product/search.js
const productApi = require('../../api/product')
const { debounce } = require('../../utils/util')

Page({
  data: {
    keyword: '',
    products: [],
    searchHistory: [],
    searched: false
  },

  onLoad() {
    // 加载搜索历史
    const history = wx.getStorageSync('searchHistory') || []
    this.setData({ searchHistory: history })
  },

  // 输入事件
  onInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  // 搜索
  onSearch() {
    const keyword = this.data.keyword.trim()
    if (!keyword) {
      return
    }
    
    this.search(keyword)
  },

  // 执行搜索
  search(keyword) {
    wx.showLoading({ title: '搜索中...' })
    
    productApi.searchProducts(keyword).then(res => {
      this.setData({
        products: res || [],
        searched: true
      })
      
      // 保存搜索历史
      this.saveSearchHistory(keyword)
      
      wx.hideLoading()
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({
        title: '搜索失败',
        icon: 'none'
      })
      console.error('搜索失败', err)
    })
  },

  // 保存搜索历史
  saveSearchHistory(keyword) {
    let history = this.data.searchHistory
    
    // 移除重复项
    history = history.filter(item => item !== keyword)
    
    // 添加到开头
    history.unshift(keyword)
    
    // 最多保存10条
    if (history.length > 10) {
      history = history.slice(0, 10)
    }
    
    this.setData({ searchHistory: history })
    wx.setStorageSync('searchHistory', history)
  },

  // 清除关键词
  clearKeyword() {
    this.setData({
      keyword: '',
      products: [],
      searched: false
    })
  },

  // 选择历史记录
  selectHistory(e) {
    const keyword = e.currentTarget.dataset.keyword
    this.setData({ keyword })
    this.search(keyword)
  },

  // 清除搜索历史
  clearHistory() {
    wx.showModal({
      title: '提示',
      content: '确定清除搜索历史吗？',
      success: res => {
        if (res.confirm) {
          this.setData({ searchHistory: [] })
          wx.removeStorageSync('searchHistory')
        }
      }
    })
  },

  // 跳转到商品详情
  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/product/detail?id=${id}`
    })
  },

  // 返回
  goBack() {
    wx.navigateBack()
  }
})
