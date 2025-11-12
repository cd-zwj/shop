// pages/user/recharge.js
const userApi = require('../../api/user')
const paymentApi = require('../../api/payment')

Page({
  data: {
    balance: 0,
    rules: [],
    selectedRule: null
  },

  onLoad() {
    this.loadBalance()
    this.loadRules()
  },

  // 加载余额
  loadBalance() {
    userApi.getUserBalance().then(res => {
      this.setData({ balance: res.balance || 0 })
    }).catch(err => {
      console.error('加载余额失败', err)
    })
  },

  // 加载充值规则
  loadRules() {
    userApi.getRechargeRules().then(res => {
      this.setData({ rules: res || [] })
    }).catch(err => {
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      })
      console.error('加载充值规则失败', err)
    })
  },

  // 选择充值规则
  selectRule(e) {
    const id = e.currentTarget.dataset.id
    this.setData({ selectedRule: id })
  },

  // 充值
  recharge() {
    if (!this.data.selectedRule) {
      wx.showToast({
        title: '请选择充值金额',
        icon: 'none'
      })
      return
    }

    wx.showLoading({ title: '创建订单...' })
    
    // 创建充值订单
    userApi.createRechargeOrder(this.data.selectedRule).then(res => {
      wx.hideLoading()
      
      // 创建支付
      return paymentApi.createPayment(res.orderNo).then(payRes => {
        // 调起微信支付
        wx.requestPayment({
          timeStamp: payRes.timeStamp,
          nonceStr: payRes.nonceStr,
          package: payRes.package,
          signType: payRes.signType,
          paySign: payRes.paySign,
          success: () => {
            wx.showToast({
              title: '充值成功',
              icon: 'success'
            })
            
            // 刷新余额
            setTimeout(() => {
              this.loadBalance()
            }, 1500)
          },
          fail: (err) => {
            if (err.errMsg === 'requestPayment:fail cancel') {
              wx.showToast({
                title: '充值已取消',
                icon: 'none'
              })
            } else {
              wx.showToast({
                title: '充值失败',
                icon: 'none'
              })
            }
          }
        })
      })
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({
        title: err.message || '充值失败',
        icon: 'none'
      })
      console.error('充值失败', err)
    })
  }
})
