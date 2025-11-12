// api/user.js
const request = require('../utils/request')

/**
 * 获取用户信息
 */
function getUserInfo() {
  return request.get('/miniprogram/user/info')
}

/**
 * 获取用户积分
 */
function getUserPoints() {
  return request.get('/miniprogram/points/balance')
}

/**
 * 获取积分明细
 */
function getPointsLogs(params) {
  return request.get('/miniprogram/points/logs', params)
}

/**
 * 获取积分兑换商品列表
 */
function getExchangeProducts() {
  return request.get('/miniprogram/points/exchange-products')
}

/**
 * 积分兑换商品
 */
function exchangeProduct(exchangeProductId) {
  return request.post('/miniprogram/points/exchange', { exchangeProductId })
}

/**
 * 获取用户余额
 */
function getUserBalance() {
  return request.get('/miniprogram/recharge/balance')
}

/**
 * 获取余额明细
 */
function getBalanceLogs(params) {
  return request.get('/miniprogram/recharge/balance-logs', params)
}

/**
 * 获取充值规则
 */
function getRechargeRules() {
  return request.get('/miniprogram/recharge/rules')
}

/**
 * 创建充值订单
 */
function createRechargeOrder(ruleId) {
  return request.post('/miniprogram/recharge/create', { ruleId })
}

module.exports = {
  getUserInfo,
  getUserPoints,
  getPointsLogs,
  getExchangeProducts,
  exchangeProduct,
  getUserBalance,
  getBalanceLogs,
  getRechargeRules,
  createRechargeOrder
}
