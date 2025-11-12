import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useAdminStore } from '../stores/admin'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/product/list',
    name: 'ProductList',
    component: () => import('../views/ProductList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/product/create',
    name: 'ProductCreate',
    component: () => import('../views/ProductCreate.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/product/edit/:id',
    name: 'ProductEdit',
    component: () => import('../views/ProductEdit.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/order/list',
    name: 'OrderList',
    component: () => import('../views/OrderList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/order/detail/:orderNo',
    name: 'OrderDetail',
    component: () => import('../views/OrderDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/withdrawal/list',
    name: 'WithdrawalList',
    component: () => import('../views/WithdrawalList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/sales/statistics',
    name: 'SalesStatistics',
    component: () => import('../views/SalesStatistics.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/marketing/manage',
    name: 'MarketingManage',
    component: () => import('../views/MarketingManage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/pos/checkout',
    name: 'PosCheckout',
    component: () => import('../views/PosCheckout.vue'),
    meta: { requiresAuth: true }
  },
  // Admin routes
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('../views/AdminLogin.vue')
  },
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: () => import('../views/AdminDashboard.vue'),
    meta: { requiresAdminAuth: true }
  },
  {
    path: '/admin/merchants',
    name: 'AdminMerchants',
    component: () => import('../views/AdminMerchants.vue'),
    meta: { requiresAdminAuth: true }
  },
  {
    path: '/admin/merchant/:id',
    name: 'AdminMerchantDetail',
    component: () => import('../views/AdminMerchantDetail.vue'),
    meta: { requiresAdminAuth: true }
  },
  {
    path: '/admin/withdrawals',
    name: 'AdminWithdrawals',
    component: () => import('../views/AdminWithdrawals.vue'),
    meta: { requiresAdminAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const adminStore = useAdminStore()
  
  if (to.meta.requiresAuth && !userStore.token) {
    next('/login')
  } else if (to.meta.requiresAdminAuth && !adminStore.token) {
    next('/admin/login')
  } else {
    next()
  }
})

export default router

