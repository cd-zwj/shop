<template>
  <div class="create-order-container">
    <el-card>
      <h2>创建订单</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="订单金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" />
        </el-form-item>
        <el-form-item label="支付方式" prop="payType">
          <el-radio-group v-model="form.payType">
            <el-radio label="WECHAT">微信支付</el-radio>
            <el-radio label="ALIPAY">支付宝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="订单标题" prop="subject">
          <el-input v-model="form.subject" />
        </el-form-item>
        <el-form-item label="订单描述" prop="body">
          <el-input v-model="form.body" type="textarea" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleCreate" :loading="loading">创建订单</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-card v-if="payResponse" style="margin-top: 20px">
      <h3>支付信息</h3>
      <div v-if="payResponse.payType === 'WECHAT'">
        <p>请使用微信扫码支付</p>
        <img :src="`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${payResponse.qrCode}`" />
      </div>
      <div v-else>
        <p>订单号：{{ payResponse.orderNo }}</p>
        <el-button type="primary" @click="goToPay">前往支付</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const formRef = ref()
const payResponse = ref(null)

const form = reactive({
  amount: 0.01,
  payType: 'WECHAT',
  subject: '',
  body: ''
})

const rules = {
  amount: [{ required: true, message: '请输入订单金额', trigger: 'blur' }],
  payType: [{ required: true, message: '请选择支付方式', trigger: 'change' }],
  subject: [{ required: true, message: '请输入订单标题', trigger: 'blur' }]
}

const handleCreate = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await api.post('/order/create', form)
    if (res.data.code === 200) {
      const orderNo = res.data.data.orderNo
      // 发起支付
      const payRes = await api.post('/order/pay', null, {
        params: { orderNo }
      })
      if (payRes.data.code === 200) {
        payResponse.value = payRes.data.data
        ElMessage.success('订单创建成功')
      }
    }
  } catch (error) {
    ElMessage.error('创建订单失败')
  } finally {
    loading.value = false
  }
}

const goToPay = () => {
  window.open(payResponse.value.payUrl)
}
</script>

<style scoped>
.create-order-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}
</style>

