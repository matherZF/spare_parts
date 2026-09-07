<template>
  <el-container class="desktop-layout">
    <!-- 左侧菜单 -->
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon class="logo-icon"><Box /></el-icon>
        <span class="logo-text">备品备件管理</span>
      </div>
      <el-menu
        :default-active="$route.path"
        :default-openeds="defaultOpeneds"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        unique-opened
      >
        <!-- 1. 基础数据 -->
        <el-sub-menu index="basic">
          <template #title>
            <el-icon><Files /></el-icon>
            <span>基础数据</span>
          </template>
          <el-menu-item index="/products">
            <el-icon><Goods /></el-icon>
            <span>货品管理</span>
          </el-menu-item>
          <el-menu-item index="/locations">
            <el-icon><Location /></el-icon>
            <span>库位管理</span>
          </el-menu-item>
          <el-menu-item index="/equipments"><el-icon><Cpu /></el-icon><span>设备管理</span></el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/alerts"><el-icon><Bell /></el-icon><span>预警管理</span></el-menu-item>

        <!-- 2. 入库管理 -->
        <el-menu-item index="/orders">
          <el-icon><Tickets /></el-icon>
          <span>入库管理</span>
        </el-menu-item>

        <!-- 3. 出库管理 -->
        <el-menu-item index="/outbound">
          <el-icon><Box /></el-icon>
          <span>出库管理</span>
        </el-menu-item>

        <!-- 4. 库存管理 -->
        <el-sub-menu index="inventory-group">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>库存管理</span>
          </template>
          <el-menu-item index="/inventory">
            <el-icon><Search /></el-icon>
            <span>库存查询</span>
          </el-menu-item>
          <el-menu-item index="/inventory/logs">
            <el-icon><Document /></el-icon>
            <span>库存日志</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 5. 系统管理（仅管理员） -->
        <el-sub-menu v-if="authStore.isAdmin" index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/users">
            <el-icon><UserFilled /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部 Header -->
      <el-header height="60px" class="header">
        <div class="header-left">
          <span class="sys-title">备品备件管理系统</span>
        </div>
        <div class="header-right">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/orders' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="$route.meta.title">
              {{ $route.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
          <!-- 用户下拉菜单 -->
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-info">
              <el-icon><UserFilled /></el-icon>
              <span class="user-name">{{ authStore.displayName }}</span>
              <el-tag size="small" :type="authStore.isAdmin ? 'danger' : 'info'" effect="plain">
                {{ authStore.isAdmin ? '管理员' : '作业员' }}
              </el-tag>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="users" v-if="authStore.isAdmin">用户管理</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容 -->
      <el-main class="main">
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 根据当前路由自动展开对应的父级子菜单
const defaultOpeneds = computed(() => {
  const p = route.path
  if (p.startsWith('/products') || p.startsWith('/locations')) return ['basic']
  if (p.startsWith('/inventory')) return ['inventory-group']
  if (p.startsWith('/users')) return ['system']
  return []
})

function handleCommand(cmd) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '退出确认', { type: 'warning' })
      .then(() => {
        authStore.logout()
        ElMessage.success('已退出登录')
        router.push('/login')
      })
      .catch(() => {})
  } else if (cmd === 'users') {
    router.push('/users')
  }
}
</script>

<style scoped lang="scss">
.desktop-layout {
  height: 100vh;
  min-height: 100vh;
}
.aside {
  background: #304156;
  color: #fff;
  overflow-x: hidden;
  :deep(.el-menu) {
    border-right: none;
  }
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.logo-icon {
  font-size: 24px;
  color: #409EFF;
}
.logo-text {
  font-size: 16px;
  font-weight: 600;
}
.header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}
.sys-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
  color: #606266;
  font-size: 13px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #303133;
  .user-name {
    font-size: 14px;
    font-weight: 500;
  }
}
.main {
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
  padding: 16px;
  overflow-y: auto;
}
</style>
