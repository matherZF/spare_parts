import { createRouter, createWebHashHistory } from 'vue-router'

// 使用 hash 模式，便于 jar 部署时不需要 history fallback 配置
const routes = [
  { path: '/', redirect: '/orders' },
  {
    path: '/products',
    name: 'Products',
    component: () => import('@/pages/Products.vue'),
    meta: { title: '商品管理', icon: 'Goods' }
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
    meta: { title: '上架单管理', icon: 'Tickets' }
  },
  {
    path: '/orders/:id',
    name: 'OrderDetail',
    component: () => import('@/pages/OrderDetail.vue'),
    meta: { title: '上架单详情', hidden: true }
  },
  {
    path: '/putaway',
    name: 'Putaway',
    component: () => import('@/pages/Putaway.vue'),
    meta: { title: '上架作业', icon: 'Check', primary: true }
  },
  {
    path: '/inventory',
    name: 'Inventory',
    component: () => import('@/pages/Inventory.vue'),
    meta: { title: '库存查询', icon: 'DataAnalysis' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 页面标题动态设置
router.afterEach((to) => {
  const title = to.meta?.title
  if (title) {
    document.title = `${title} - 简易WMS管理系统`
  }
})

export default router
