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
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        unique-opened
      >
        <el-menu-item index="/orders">
          <el-icon><Tickets /></el-icon>
          <span>入库管理</span>
        </el-menu-item>
        <el-menu-item index="/products">
          <el-icon><Goods /></el-icon>
          <span>货品管理</span>
        </el-menu-item>
        <el-menu-item index="/locations">
          <el-icon><Location /></el-icon>
          <span>库位管理</span>
        </el-menu-item>
        <el-menu-item index="/putaway" class="menu-primary">
          <el-icon><Check /></el-icon>
          <span>上架作业</span>
        </el-menu-item>
        <el-menu-item index="/outbound">
          <el-icon><Box /></el-icon>
          <span>出库管理</span>
        </el-menu-item>
        <el-menu-item index="/inventory">
          <el-icon><DataAnalysis /></el-icon>
          <span>库存查询</span>
        </el-menu-item>
        <!-- 仅管理员可见 -->
        <el-menu-item v-if="authStore.isAdmin" index="/users">
          <el-icon><UserFilled /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

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
.menu-primary {
  :deep(.el-menu-item.is-active) {
    font-weight: 600;
  }
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
