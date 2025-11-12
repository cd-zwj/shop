// utils/util.js

/**
 * 格式化时间
 */
const formatTime = date => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()

  return `${[year, month, day].map(formatNumber).join('-')} ${[hour, minute, second].map(formatNumber).join(':')}`
}

const formatNumber = n => {
  n = n.toString()
  return n[1] ? n : `0${n}`
}

/**
 * 格式化金额
 */
const formatMoney = (money) => {
  return parseFloat(money).toFixed(2)
}

/**
 * 订单状态文本
 */
const getOrderStatusText = (status) => {
  const statusMap = {
    0: '待支付',
    1: '已支付',
    2: '待发货',
    3: '已发货',
    4: '已完成',
    5: '已取消',
    6: '退款中',
    7: '已退款'
  }
  return statusMap[status] || '未知状态'
}

/**
 * 订单状态颜色
 */
const getOrderStatusColor = (status) => {
  const colorMap = {
    0: '#faad14',
    1: '#52c41a',
    2: '#1296db',
    3: '#1296db',
    4: '#52c41a',
    5: '#999',
    6: '#faad14',
    7: '#999'
  }
  return colorMap[status] || '#999'
}

/**
 * 防抖函数
 */
const debounce = (fn, delay = 500) => {
  let timer = null
  return function(...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

/**
 * 节流函数
 */
const throttle = (fn, delay = 500) => {
  let lastTime = 0
  return function(...args) {
    const now = Date.now()
    if (now - lastTime >= delay) {
      fn.apply(this, args)
      lastTime = now
    }
  }
}

module.exports = {
  formatTime,
  formatMoney,
  getOrderStatusText,
  getOrderStatusColor,
  debounce,
  throttle
}
