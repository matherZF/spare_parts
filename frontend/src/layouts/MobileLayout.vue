<template>
  <div class="mobile-layout">
    <!-- 顶部 Header -->
    <header class="mobile-header">
      <span class="mobile-title">
        {{ $route.meta.title || '备品备件管理' }}
      </span>
      <!-- 右侧用户操作 -->
      <div class="header-actions">
        <el-icon v-if="authStore.isAdmin" class="action-icon" @click="goUsers"><UserFilled /></el-icon>
        <el-icon class="action-icon" @click="handleLogout"><SwitchButton /></el-icon>
      </div>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// Tab 顺序：入库 / 出库
const tabs = [
  { path: '/orders', label: '入库', icon: 'Tickets', primary: true },
  { path: '/outbound', label: '出库', icon: 'Box', primary: true }
]

function isActive(tab) {
  if (tab.path === '/orders' && route.path.startsWith('/orders/')) return true
  if (tab.path === '/outbound' && route.path.startsWith('/outbound/')) return true
  return route.path === tab.path
}

function goTab(path) {
  if (route.path !== path) {
    router.push(path)
  }
}

function goUsers() {
  router.push('/users')
}

function handleLogout() {
  ElMessageBox.confirm('确定退出登录吗？', '退出确认', { type: 'warning' })
    .then(() => {
      authStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    })
    .catch(() => {})
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
  justify-content: space-between;
  padding: 0 16px;
  font-size: 16px;
  font-weight: 600;
  position: sticky;
  top: 0;
  z-index: 10;
}
.mobile-title {
  flex: 1;
  text-align: center;
}
.header-actions {
  display: flex;
  gap: 16px;
  min-width: 48px;
  justify-content: flex-end;
}
.action-icon {
  font-size: 22px;
  cursor: pointer;
  opacity: 0.9;
  &:active {
    opacity: 1;
  }
}
.mobile-main {
  flex: 1;
  padding-bottom: 72px;
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
  grid-template-columns: repeat(2, 1fr);
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
