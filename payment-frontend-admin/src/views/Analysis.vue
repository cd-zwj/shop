<template>
  <div class="analysis-container">
    <el-card>
      <h2>数据分析</h2>
      <el-form :model="form" inline>
        <el-form-item label="分析类型">
          <el-select v-model="form.analysisType">
            <el-option label="用户行为分析" value="USER_BEHAVIOR" />
            <el-option label="支付趋势分析" value="PAYMENT_TREND" />
            <el-option label="用户分群分析" value="USER_SEGMENT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleAnalyze" :loading="loading">开始分析</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-card v-if="analysisResult" style="margin-top: 20px">
      <h3>分析结果</h3>
      <div v-if="analysisResult.status === 'PROCESSING'">
        <el-alert message="分析处理中，请稍候..." type="info" />
      </div>
      <div v-else-if="analysisResult.status === 'SUCCESS'">
        <div v-if="analysisResult.chartUrl">
          <img :src="analysisResult.chartUrl" style="max-width: 100%" />
        </div>
        <pre>{{ analysisResult.analysisData }}</pre>
      </div>
      <div v-else>
        <el-alert message="分析失败" type="error" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const analysisResult = ref(null)

const form = reactive({
  analysisType: 'USER_BEHAVIOR'
})

const handleAnalyze = async () => {
  loading.value = true
  try {
    const res = await api.post('/analysis/analyze', {
      analysisType: form.analysisType,
      params: {}
    })
    if (res.data.code === 200) {
      analysisResult.value = res.data.data
      ElMessage.success('分析任务已提交')
      // 轮询查询结果
      pollResult(res.data.data.id)
    }
  } catch (error) {
    ElMessage.error('分析失败')
  } finally {
    loading.value = false
  }
}

const pollResult = async (id) => {
  const timer = setInterval(async () => {
    try {
      const res = await api.get(`/analysis/result/${id}`)
      if (res.data.code === 200) {
        analysisResult.value = res.data.data
        if (res.data.data.status !== 'PROCESSING') {
          clearInterval(timer)
        }
      }
    } catch (error) {
      clearInterval(timer)
    }
  }, 2000)
}
</script>

<style scoped>
.analysis-container {
  padding: 20px;
}
</style>

