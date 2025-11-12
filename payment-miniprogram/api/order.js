// api/order.js
const request = require('../utils/request')

/**
 * 创建订单
 */
function createOrder(data) {
  return request.post('/miniprogram/orders', data)
}

/**
 * 获取订单列表
 */
function getOrderList(params) {
  return request.get('/miniprogram/orders', params)
}

/**
 * 获取订单详情
 */
function getOrderDetail(orderNo) {
  return request.get(`/miniprogram/orders/${orderNo}`)
}

/**
 * 取消订单
 */
function cancelOrder(orderNo) {
  return request.post(`/miniprogram/orders/${orderNo}/cancel`)
}

/**
 * 确认收货
 */
function confirmOrder(orderNo) {
  return request.post(`/miniprogram/orders/${orderNo}/confirm`)
}

module.exports = {
  createOrder,
  getOrderList,
  getOrderDetail,
  cancelOrder,
  confirmOrder
}
