// api/payment.js
const request = require('../utils/request')

/**
 * 创建支付订单
 */
function createPayment(orderNo) {
  return request.post('/miniprogram/payment/create', { orderNo })
}

/**
 * 查询支付状态
 */
function queryPaymentStatus(orderNo) {
  return request.get(`/miniprogram/payment/status/${orderNo}`)
}

module.exports = {
  createPayment,
  queryPaymentStatus
}
