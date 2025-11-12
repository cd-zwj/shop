// api/product.js
const request = require('../utils/request')

/**
 * 获取商品列表
 */
function getProductList(params) {
  return request.get('/miniprogram/products', params)
}

/**
 * 获取商品详情
 */
function getProductDetail(id) {
  return request.get(`/miniprogram/products/${id}`)
}

/**
 * 搜索商品
 */
function searchProducts(keyword) {
  return request.get('/miniprogram/products/search', { keyword })
}

module.exports = {
  getProductList,
  getProductDetail,
  searchProducts
}
