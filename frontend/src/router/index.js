import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const SYS_NAME = '备品备件管理'

// 使用 hash 模式，便于 jar 部署时不需要 history fallback 配置
const routes = [
  { path: '/', redirect: '/orders' },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/products',
    name: 'Products',
    component: () => import('@/pages/Products.vue'),
    meta: { title: '货品管理', icon: 'Goods' }
  },
  {
    path: '/locations',
    name: 'Locations',
    component: () => import('@/pages/Locations.vue'),
    meta: { title: '库位管理', icon: 'Location' }
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('@/pages/Orders.vue'),
    meta: { title: '入库管理', icon: 'Tickets' }
  },
  {
    path: '/orders/:id',
    name: 'OrderDetail',
    component: () => import('@/pages/OrderDetail.vue'),
    meta: { title: '入库单详情', hidden: true }
  },
  {
    path: '/putaway',
    name: 'Putaway',
    component: () => import('@/pages/Putaway.vue'),
    meta: { title: '上架作业', icon: 'Check', primary: true }
  },
  {
    path: '/outbound',
    name: 'Outbound',
    component: () => import('@/pages/Outbound.vue'),
    meta: { title: '出库管理', icon: 'Box' }
  },
  {
    path: '/outbound/picking',
    redirect: '/outbound/picking/list'
  },
  {
    path: '/outbound/picking/:id',
    name: 'Picking',
    component: () => import('@/pages/Picking.vue'),
    meta: { title: '拣货作业', hidden: true, primary: true }
  },
  {
    path: '/inventory',
    name: 'Inventory',
    component: () => import('@/pages/Inventory.vue'),
    meta: { title: '库存查询', icon: 'DataAnalysis' }
  },
  {
    path: '/users',
    name: 'Users',
    component: () => import('@/pages/Users.vue'),
    meta: { title: '用户管理', icon: 'UserFilled', adminOnly: true }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 全局前置守卫：未登录跳转登录页，非管理员不能访问 adminOnly 路由
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.public) {
    if (authStore.isLoggedIn && to.path === '/login') {
      return next('/orders')
    }
    return next()
  }

  if (!authStore.isLoggedIn) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  if (to.meta.adminOnly && !authStore.isAdmin) {
    return next('/orders')
  }

  next()
})

// 页面标题动态设置
router.afterEach((to) => {
  const title = to.meta?.title
  if (title) {
    document.title = `${title} - ${SYS_NAME}`
  }
})

export default router
