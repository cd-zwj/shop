<template>
  <div class="pos-checkout">
    <el-page-header @back="goBack" content="POS收银台" />
    
    <el-row :gutter="20" class="main-content">
      <!-- 左侧：商品搜索和列表 -->
      <el-col :span="14">
        <el-card class="product-section">
          <template #header>
            <div class="section-header">
              <span>商品选择</span>
            </div>
          </template>
          
          <!-- 搜索框 -->
          <el-form :inline="true" class="search-form">
            <el-form-item label="商品搜索">
              <el-input 
                v-model="searchKeyword" 
                placeholder="输入商品名称或编码"
                clearable
                @keyup.enter="searchProducts"
              >
                <template #append>
                  <el-button icon="Search" @click="searchProducts" />
                </template>
              </el-input>
            </el-form-item>
            
            <el-form-item label="扫码枪">
              <el-input 
                v-model="scanCode" 
                placeholder="扫描商品条码"
                clearable
                @keyup.enter="handleScan"
                ref="scanInput"
              >
                <template #prepend>
                  <el-icon><Barcode /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-form>
          
          <!-- 商品列表 -->
          <el-table 
            :data="productList" 
            v-loading="productLoading"
            height="500"
            @row-click="handleProductClick"
            class="product-table"
          >
            <el-table-column prop="productCode" label="商品编码" width="120" />
            <el-table-column prop="name" label="商品名称" min-width="200" />
            <el-table-column prop="price" label="价格" width="100">
              <template #default="{ row }">
                <span class="price">¥{{ row.price }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="stock" label="库存" width="80" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button 
                  type="primary" 
                  size="small" 
                  @click.stop="addToCart(row)"
                >
                  加入
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <!-- 分页 -->
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="fetchProducts"
            @current-change="fetchProducts"
            class="pagination"
          />
        </el-card>
      </el-col>
      
      <!-- 右侧：购物车 -->
      <el-col :span="10">
        <el-card class="cart-section">
          <template #header>
            <div class="section-header">
              <span>购物车 ({{ cartItems.length }})</span>
              <el-button 
                type="danger" 
                size="small" 
                @click="clearCart"
                :disabled="cartItems.length === 0"
              >
                清空
              </el-button>
            </div>
          </template>
          
          <!-- 购物车列表 -->
          <div class="cart-list" v-loading="cartLoading">
            <el-empty 
              v-if="cartItems.length === 0" 
              description="购物车为空"
              :image-size="100"
            />
            
            <div v-else class="cart-items">
              <div 
                v-for="item in cartItems" 
                :key="item.productId"
                class="cart-item"
              >
                <div class="item-info">
                  <div class="item-name">{{ item.productName }}</div>
                  <div class="item-code">{{ item.productCode }}</div>
                  <div class="item-price">¥{{ item.price }}</div>
                </div>
                
                <div class="item-actions">
                  <el-input-number
                    v-model="item.quantity"
                    :min="1"
                    :max="999"
                    size="small"
                    @change="updateQuantity(item)"
                  />
                  <el-button
                    type="danger"
                    size="small"
                    icon="Delete"
                    @click="removeFromCart(item.productId)"
                    circle
                  />
                </div>
                
                <div class="item-subtotal">
                  小计: ¥{{ (item.price * item.quantity).toFixed(2) }}
                </div>
              </div>
            </div>
          </div>
          
          <!-- 合计信息 -->
          <div class="cart-summary">
            <el-divider />
            <div class="summary-row">
              <span>商品数量：</span>
              <span class="value">{{ totalQuantity }} 件</span>
            </div>
            <div class="summary-row total">
              <span>合计金额：</span>
              <span class="value">¥{{ totalAmount.toFixed(2) }}</span>
            </div>
          </div>
          
          <!-- 结账按钮 -->
          <el-button
            type="primary"
            size="large"
            class="checkout-btn"
            :disabled="cartItems.length === 0"
            :loading="checkoutLoading"
            @click="handleCheckout"
          >
            结账
          </el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

// 会话ID（用于标识当前收银会话）
const sessionId = ref('session_' + Date.now())

// 商品搜索
const searchKeyword = ref('')
const scanCode = ref('')
const scanInput = ref(null)
const productList = ref([])
const productLoading = ref(false)

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 购物车
const cartItems = ref([])
const cartLoading = ref(false)
const checkoutLoading = ref(false)

// 计算属性
const totalQuantity = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + item.quantity, 0)
})

const totalAmount = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0)
})

const goBack = () => {
  router.push('/dashboard')
}

// 获取商品列表
const fetchProducts = async () => {
  productLoading.value = true
  try {
    const res = await api.get('/product/list', {
      params: {
        current: pagination.current,
        size: pagination.size,
        keyword: searchKeyword.value
      },
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (res.data.code === 200) {
      productList.value = res.data.data.records
      pagination.total = res.data.data.total
    }
  } catch (error) {
    ElMessage.error('获取商品列表失败：' + (error.message || '未知错误'))
  } finally {
    productLoading.value = false
  }
}

// 搜索商品
const searchProducts = () => {
  pagination.current = 1
  fetchProducts()
}

// 扫码处理
const handleScan = async () => {
  if (!scanCode.value) return
  
  try {
    const res = await api.post('/pos/scan', {
      productCode: scanCode.value,
      deviceId: sessionId.value,
      quantity: 1
    }, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (res.data.code === 200) {
      ElMessage.success('商品已添加到购物车')
      scanCode.value = ''
      await fetchCart()
    } else {
      ElMessage.error(res.data.message || '扫码失败')
    }
  } catch (error) {
    ElMessage.error('扫码失败：' + (error.message || '未知错误'))
  }
}

// 添加到购物车
const addToCart = async (product) => {
  try {
    const res = await api.post(`/pos/cart/${sessionId.value}/add`, {
      productId: product.id,
      quantity: 1
    }, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (res.data.code === 200) {
      ElMessage.success('商品已添加到购物车')
      await fetchCart()
    } else {
      ElMessage.error(res.data.message || '添加失败')
    }
  } catch (error) {
    ElMessage.error('添加失败：' + (error.message || '未知错误'))
  }
}

// 商品行点击（快速添加）
const handleProductClick = (row) => {
  addToCart(row)
}

// 获取购物车
const fetchCart = async () => {
  cartLoading.value = true
  try {
    const res = await api.get(`/pos/cart/${sessionId.value}`, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (res.data.code === 200) {
      cartItems.value = res.data.data
    }
  } catch (error) {
    console.error('获取购物车失败', error)
  } finally {
    cartLoading.value = false
  }
}

// 更新数量
const updateQuantity = async (item) => {
  try {
    const res = await api.put(`/pos/cart/${sessionId.value}/update`, {
      productId: item.productId,
      quantity: item.quantity
    }, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (res.data.code !== 200) {
      ElMessage.error(res.data.message || '更新失败')
      await fetchCart() // 重新获取购物车
    }
  } catch (error) {
    ElMessage.error('更新失败：' + (error.message || '未知错误'))
    await fetchCart()
  }
}

// 从购物车移除
const removeFromCart = async (productId) => {
  try {
    const res = await api.delete(`/pos/cart/${sessionId.value}/remove/${productId}`, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (res.data.code === 200) {
      ElMessage.success('商品已移除')
      await fetchCart()
    } else {
      ElMessage.error(res.data.message || '移除失败')
    }
  } catch (error) {
    ElMessage.error('移除失败：' + (error.message || '未知错误'))
  }
}

// 清空购物车
const clearCart = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空购物车吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res = await api.delete(`/pos/cart/${sessionId.value}`, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (res.data.code === 200) {
      ElMessage.success('购物车已清空')
      cartItems.value = []
    } else {
      ElMessage.error(res.data.message || '清空失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清空失败：' + (error.message || '未知错误'))
    }
  }
}

// 结账
const handleCheckout = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要结账吗？\n商品数量：${totalQuantity.value} 件\n合计金额：¥${totalAmount.value.toFixed(2)}`,
      '确认结账',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    checkoutLoading.value = true
    
    const res = await api.post(`/pos/checkout/${sessionId.value}`, null, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })
    
    if (res.data.code === 200) {
      const order = res.data.data
      ElMessage.success('订单创建成功')
      
      // 跳转到订单详情或支付页面
      router.push(`/order/detail/${order.orderNo}`)
    } else {
      ElMessage.error(res.data.message || '结账失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('结账失败：' + (error.message || '未知错误'))
    }
  } finally {
    checkoutLoading.value = false
  }
}

// 键盘快捷键
const handleKeyPress = (e) => {
  // F2 - 聚焦到扫码输入框
  if (e.key === 'F2') {
    e.preventDefault()
    scanInput.value?.focus()
  }
  // F12 - 结账
  if (e.key === 'F12') {
    e.preventDefault()
    if (cartItems.value.length > 0) {
      handleCheckout()
    }
  }
}

onMounted(() => {
  fetchProducts()
  fetchCart()
  
  // 添加键盘事件监听
  window.addEventListener('keydown', handleKeyPress)
  
  // 自动聚焦到扫码输入框
  setTimeout(() => {
    scanInput.value?.focus()
  }, 500)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyPress)
})
</script>

<style scoped>
.pos-checkout {
  padding: 20px;
  height: calc(100vh - 40px);
}

.main-content {
  margin-top: 20px;
  height: calc(100% - 80px);
}

.product-section,
.cart-section {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 16px;
}

.search-form {
  margin-bottom: 20px;
}

.product-table {
  cursor: pointer;
}

.product-table :deep(.el-table__row) {
  cursor: pointer;
}

.product-table :deep(.el-table__row:hover) {
  background-color: #f5f7fa;
}

.price {
  color: #f56c6c;
  font-weight: bold;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.cart-list {
  flex: 1;
  overflow-y: auto;
  min-height: 400px;
  max-height: 500px;
}

.cart-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cart-item {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  background-color: #fff;
}

.item-info {
  margin-bottom: 10px;
}

.item-name {
  font-weight: bold;
  font-size: 14px;
  margin-bottom: 4px;
}

.item-code {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.item-price {
  color: #f56c6c;
  font-weight: bold;
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.item-subtotal {
  text-align: right;
  font-weight: bold;
  color: #303133;
}

.cart-summary {
  margin-top: 20px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
}

.summary-row.total {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
}

.summary-row .value {
  font-weight: bold;
}

.checkout-btn {
  width: 100%;
  margin-top: 20px;
  height: 50px;
  font-size: 18px;
  font-weight: bold;
}
</style>
