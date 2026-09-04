<template>
  <div class="mobile-layout">
    <!-- 顶部 Header -->
    <header class="mobile-header">
      <span class="mobile-title">
        {{ $route.meta.title || 'WMS' }}
      </span>
    </header>

    <!-- 主内容 -->
    <main class="mobile-main">
      <slot />
    </main>

    <!-- 底部导航（5 个 Tab） -->
    <nav class="mobile-nav">
      <div
        v-for="tab in tabs"
        :key="tab.path"
        class="tab-item"
        :class="{
          active: isActive(tab),
          primary: tab.primary && isActive(tab)
        }"
        @click="goTab(tab.path)"
      >
        <el-icon class="icon">
          <component :is="tab.icon" />
        </el-icon>
        <span class="tab-label">{{ tab.label }}</span>
      </div>
    </nav>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// Tab 顺序：上架单 / 商品 / 库位 / 上架作业（高亮）/ 库存
const tabs = [
  { path: '/orders', label: '上架单', icon: 'Tickets', primary: false },
  { path: '/products', label: '商品', icon: 'Goods', primary: false },
  { path: '/locations', label: '库位', icon: 'Location', primary: false },
  { path: '/putaway', label: '上架作业', icon: 'Check', primary: true },
  { path: '/inventory', label: '库存', icon: 'DataAnalysis', primary: false }
]

function isActive(tab) {
  // 详情页归属到上架单 tab
  if (tab.path === '/orders' && route.path.startsWith('/orders/')) {
    return true
  }
  return route.path === tab.path
}

function goTab(path) {
  if (route.path !== path) {
    router.push(path)
  }
}
</script>

<style scoped lang="scss">
.mobile-layout {
  min-height: 100vh;
  background: #f0f2f5;
  display: flex;
  flex-direction: column;
}
.mobile-header {
  height: 52px;
  background: $primary;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  position: sticky;
  top: 0;
  z-index: 10;
}
.mobile-main {
  flex: 1;
  padding-bottom: 72px; // 给底部 nav 留空间
  min-height: calc(100vh - 52px - 56px);
  overflow-y: auto;
}
.mobile-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 56px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  text-align: center;
  z-index: 100;
}
.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 56px;
  font-size: 12px;
  color: #606266;
  cursor: pointer;
  transition: color 0.2s;
  .icon {
    font-size: 24px;
    line-height: 1;
    margin-bottom: 2px;
  }
  &.active {
    color: $primary;
  }
  &.primary.active {
    color: darken($primary, 8%);
    font-weight: 700;
    .icon {
      transform: scale(1.05);
    }
  }
}
</style>
