<template>
  <div class="marketing-manage">
    <h2 class="page-title">营销管理</h2>
    
    <el-tabs v-model="activeTab">
      <!-- 积分规则 -->
      <el-tab-pane label="积分规则" name="points">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>积分规则设置</span>
              <el-switch 
                v-model="pointsRule.enabled" 
                @change="handlePointsEnabledChange"
                active-text="启用"
                inactive-text="禁用"
              />
            </div>
          </template>
          
          <el-form :model="pointsRule" label-width="150px" :disabled="!pointsRule.enabled">
            <el-form-item label="积分比例">
              <el-input-number 
                v-model="pointsRule.pointsRatio" 
                :min="1" 
                :step="1"
              />
              <span style="margin-left: 10px;">积分/元（每消费1元获得的积分）</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="savePointsRule" :loading="pointsLoading">
                保存设置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
        
        <!-- 积分兑换商品 -->
        <el-card class="exchange-card">
          <template #header>
            <div class="card-header">
              <span>积分兑换商品</span>
              <el-button type="primary" size="small" @click="showExchangeDialog()">
                <el-icon><Plus /></el-icon>
                <span>添加兑换商品</span>
              </el-button>
            </div>
          </template>
          
          <el-table :data="exchangeProducts" v-loading="exchangeLoading" stripe>
            <el-table-column label="商品图片" width="100">
              <template #default="{ row }">
                <el-image 
                  :src="row.productImage" 
                  fit="cover"
                  style="width: 60px; height: 60px; border-radius: 4px;"
                />
              </template>
            </el-table-column>
            <el-table-column prop="productName" label="商品名称" min-width="200" />
            <el-table-column prop="pointsRequired" label="所需积分" width="120" />
            <el-table-column prop="stock" label="兑换库存" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">
                  {{ row.status === 1 ? '上架' : '下架' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" fixed="right" width="200">
              <template #default="{ row }">
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="showExchangeDialog(row)"
                >
                  编辑
                </el-button>
                <el-button 
                  :type="row.status === 1 ? 'warning' : 'success'" 
                  size="small" 
                  @click="toggleExchangeStatus(row)"
                >
                  {{ row.status === 1 ? '下架' : '上架' }}
                </el-button>
                <el-popconfirm
                  title="确定删除该兑换商品吗？"
                  @confirm="deleteExchangeProduct(row.id)"
                >
                  <template #reference>
                    <el-button type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
      
      <!-- 充值规则 -->
      <el-tab-pane label="充值规则" name="recharge">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>充值规则设置</span>
              <el-button type="primary" size="small" @click="showRechargeDialog()">
                <el-icon><Plus /></el-icon>
                <span>添加充值档位</span>
              </el-button>
            </div>
          </template>
          
          <el-table :data="rechargeRules" v-loading="rechargeLoading" stripe>
            <el-table-column prop="rechargeAmount" label="充值金额" width="150">
              <template #default="{ row }">
                ¥{{ row.rechargeAmount }}
              </template>
            </el-table-column>
            <el-table-column prop="bonusAmount" label="赠送金额" width="150">
              <template #default="{ row }">
                ¥{{ row.bonusAmount }}
              </template>
            </el-table-column>
            <el-table-column label="实际到账" width="150">
              <template #default="{ row }">
                ¥{{ (parseFloat(row.rechargeAmount) + parseFloat(row.bonusAmount)).toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column label="优惠力度" width="120">
              <template #default="{ row }">
                {{ ((parseFloat(row.bonusAmount) / parseFloat(row.rechargeAmount)) * 100).toFixed(1) }}%
              </template>
            </el-table-column>
            <el-table-column prop="enabled" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.enabled === 1 ? 'success' : 'info'">
                  {{ row.enabled === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="sortOrder" label="排序" width="80" />
            <el-table-column label="操作" fixed="right" width="200">
              <template #default="{ row }">
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="showRechargeDialog(row)"
                >
                  编辑
                </el-button>
                <el-button 
                  :type="row.enabled === 1 ? 'warning' : 'success'" 
                  size="small" 
                  @click="toggleRechargeStatus(row)"
                >
                  {{ row.enabled === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-popconfirm
                  title="确定删除该充值档位吗？"
                  @confirm="deleteRechargeRule(row.id)"
                >
                  <template #reference>
                    <el-button type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
    
    <!-- 积分兑换商品对话框 -->
    <el-dialog 
      v-model="exchangeDialogVisible" 
      :title="exchangeForm.id ? '编辑兑换商品' : '添加兑换商品'" 
      width="500px"
    >
      <el-form :model="exchangeForm" :rules="exchangeRules" ref="exchangeFormRef" label-width="120px">
        <el-form-item label="选择商品" prop="productId">
          <el-select 
            v-model="exchangeForm.productId" 
            placeholder="请选择商品"
            filterable
            @change="handleProductChange"
          >
            <el-option 
              v-for="product in products" 
              :key="product.id" 
              :label="product.name" 
              :value="product.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所需积分" prop="pointsRequired">
          <el-input-number 
            v-model="exchangeForm.pointsRequired" 
            :min="1" 
            :step="10"
          />
        </el-form-item>
        <el-form-item label="兑换库存" prop="stock">
          <el-input-number 
            v-model="exchangeForm.stock" 
            :min="0" 
            :step="1"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="exchangeForm.status">
            <el-radio :label="1">上架</el-radio>
            <el-radio :label="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exchangeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveExchangeProduct" :loading="exchangeSaveLoading">
          保存
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 充值规则对话框 -->
    <el-dialog 
      v-model="rechargeDialogVisible" 
      :title="rechargeForm.id ? '编辑充值档位' : '添加充值档位'" 
      width="500px"
    >
      <el-form :model="rechargeForm" :rules="rechargeRules" ref="rechargeFormRef" label-width="120px">
        <el-form-item label="充值金额" prop="rechargeAmount">
          <el-input-number 
            v-model="rechargeForm.rechargeAmount" 
            :min="0.01" 
            :precision="2"
            :step="10"
          />
          <span style="margin-left: 10px;">元</span>
        </el-form-item>
        <el-form-item label="赠送金额" prop="bonusAmount">
          <el-input-number 
            v-model="rechargeForm.bonusAmount" 
            :min="0" 
            :precision="2"
            :step="1"
          />
          <span style="margin-left: 10px;">元</span>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number 
            v-model="rechargeForm.sortOrder" 
            :min="0" 
            :step="1"
          />
          <div class="form-tip">数字越小越靠前</div>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="rechargeForm.enabled">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRechargeRule" :loading="rechargeSaveLoading">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const activeTab = ref('points')
const pointsLoading = ref(false)
const exchangeLoading = ref(false)
const rechargeLoading = ref(false)
const exchangeSaveLoading = ref(false)
const rechargeSaveLoading = ref(false)
const exchangeDialogVisible = ref(false)
const rechargeDialogVisible = ref(false)
const exchangeFormRef = ref()
const rechargeFormRef = ref()

// 积分规则
const pointsRule = ref({
  enabled: false,
  pointsRatio: 1
})

// 积分兑换商品列表
const exchangeProducts = ref([])

// 充值规则列表
const rechargeRules = ref([])

// 商品列表（用于选择）
const products = ref([])

// 积分兑换商品表单
const exchangeForm = reactive({
  id: null,
  productId: null,
  pointsRequired: null,
  stock: 0,
  status: 1
})

// 积分兑换商品表单验证
const exchangeRules = {
  productId: [
    { required: true, message: '请选择商品', trigger: 'change' }
  ],
  pointsRequired: [
    { required: true, message: '请输入所需积分', trigger: 'blur' }
  ]
}

// 充值规则表单
const rechargeForm = reactive({
  id: null,
  rechargeAmount: null,
  bonusAmount: null,
  sortOrder: 0,
  enabled: 1
})

// 充值规则表单验证
const rechargeRulesValidation = {
  rechargeAmount: [
    { required: true, message: '请输入充值金额', trigger: 'blur' }
  ],
  bonusAmount: [
    { required: true, message: '请输入赠送金额', trigger: 'blur' }
  ]
}

// 获取积分规则
const fetchPointsRule = async () => {
  try {
    const res = await api.get('/points/rule')
    if (res.data.code === 200) {
      pointsRule.value = res.data.data || { enabled: false, pointsRatio: 1 }
    }
  } catch (error) {
    console.error('获取积分规则失败', error)
  }
}

// 保存积分规则
const savePointsRule = async () => {
  pointsLoading.value = true
  try {
    const res = await api.post('/points/rule', pointsRule.value)
    if (res.data.code === 200) {
      ElMessage.success('保存成功')
    } else {
      ElMessage.error(res.data.message || '保存失败')
    }
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    pointsLoading.value = false
  }
}

// 积分启用状态变化
const handlePointsEnabledChange = () => {
  savePointsRule()
}

// 获取积分兑换商品列表
const fetchExchangeProducts = async () => {
  exchangeLoading.value = true
  try {
    const res = await api.get('/points/exchange-products')
    if (res.data.code === 200) {
      exchangeProducts.value = res.data.data || []
    }
  } catch (error) {
    ElMessage.error('获取兑换商品列表失败')
  } finally {
    exchangeLoading.value = false
  }
}

// 获取商品列表
const fetchProducts = async () => {
  try {
    const res = await api.get('/product/list', {
      params: { status: 1, pageSize: 1000 }
    })
    if (res.data.code === 200) {
      products.value = res.data.data.records || []
    }
  } catch (error) {
    console.error('获取商品列表失败', error)
  }
}

// 显示兑换商品对话框
const showExchangeDialog = (row) => {
  if (row) {
    Object.assign(exchangeForm, row)
  } else {
    exchangeForm.id = null
    exchangeForm.productId = null
    exchangeForm.pointsRequired = null
    exchangeForm.stock = 0
    exchangeForm.status = 1
  }
  exchangeDialogVisible.value = true
}

// 商品选择变化
const handleProductChange = (productId) => {
  const product = products.value.find(p => p.id === productId)
  if (product && !exchangeForm.pointsRequired) {
    // 默认积分为价格的10倍
    exchangeForm.pointsRequired = Math.ceil(product.price * 10)
  }
}

// 保存兑换商品
const saveExchangeProduct = async () => {
  await exchangeFormRef.value.validate()
  
  exchangeSaveLoading.value = true
  try {
    const url = exchangeForm.id ? '/points/exchange-product/update' : '/points/exchange-product/create'
    const res = await api.post(url, exchangeForm)
    if (res.data.code === 200) {
      ElMessage.success('保存成功')
      exchangeDialogVisible.value = false
      fetchExchangeProducts()
    } else {
      ElMessage.error(res.data.message || '保存失败')
    }
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    exchangeSaveLoading.value = false
  }
}

// 切换兑换商品状态
const toggleExchangeStatus = async (row) => {
  try {
    const newStatus = row.status === 1 ? 0 : 1
    const res = await api.post('/points/exchange-product/update', {
      ...row,
      status: newStatus
    })
    if (res.data.code === 200) {
      ElMessage.success(newStatus === 1 ? '上架成功' : '下架成功')
      fetchExchangeProducts()
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 删除兑换商品
const deleteExchangeProduct = async (id) => {
  try {
    const res = await api.delete(`/points/exchange-product/delete/${id}`)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      fetchExchangeProducts()
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 获取充值规则列表
const fetchRechargeRules = async () => {
  rechargeLoading.value = true
  try {
    const res = await api.get('/recharge/rules')
    if (res.data.code === 200) {
      rechargeRules.value = res.data.data || []
    }
  } catch (error) {
    ElMessage.error('获取充值规则失败')
  } finally {
    rechargeLoading.value = false
  }
}

// 显示充值规则对话框
const showRechargeDialog = (row) => {
  if (row) {
    Object.assign(rechargeForm, row)
  } else {
    rechargeForm.id = null
    rechargeForm.rechargeAmount = null
    rechargeForm.bonusAmount = null
    rechargeForm.sortOrder = 0
    rechargeForm.enabled = 1
  }
  rechargeDialogVisible.value = true
}

// 保存充值规则
const saveRechargeRule = async () => {
  await rechargeFormRef.value.validate()
  
  rechargeSaveLoading.value = true
  try {
    const url = rechargeForm.id ? '/recharge/rule/update' : '/recharge/rule/create'
    const res = await api.post(url, rechargeForm)
    if (res.data.code === 200) {
      ElMessage.success('保存成功')
      rechargeDialogVisible.value = false
      fetchRechargeRules()
    } else {
      ElMessage.error(res.data.message || '保存失败')
    }
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    rechargeSaveLoading.value = false
  }
}

// 切换充值规则状态
const toggleRechargeStatus = async (row) => {
  try {
    const newEnabled = row.enabled === 1 ? 0 : 1
    const res = await api.post('/recharge/rule/update', {
      ...row,
      enabled: newEnabled
    })
    if (res.data.code === 200) {
      ElMessage.success(newEnabled === 1 ? '启用成功' : '禁用成功')
      fetchRechargeRules()
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 删除充值规则
const deleteRechargeRule = async (id) => {
  try {
    const res = await api.delete(`/recharge/rule/delete/${id}`)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      fetchRechargeRules()
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  fetchPointsRule()
  fetchExchangeProducts()
  fetchRechargeRules()
  fetchProducts()
})
</script>

<style scoped>
.marketing-manage {
  padding: 0;
}

.page-title {
  margin: 0 0 20px 0;
  font-size: 24px;
  color: #303133;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.exchange-card {
  margin-top: 20px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>
