<template>
  <el-container class="desktop-layout">
    <!-- 左侧菜单 -->
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon class="logo-icon"><Box /></el-icon>
        <span class="logo-text">简易 WMS</span>
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
          <span>上架单管理</span>
        </el-menu-item>
        <el-menu-item index="/products">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/locations">
          <el-icon><Location /></el-icon>
          <span>库位管理</span>
        </el-menu-item>
        <el-menu-item index="/putaway" class="menu-primary">
          <el-icon><Check /></el-icon>
          <span>上架作业</span>
        </el-menu-item>
        <el-menu-item index="/inventory">
          <el-icon><DataAnalysis /></el-icon>
          <span>库存查询</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部 Header -->
      <el-header height="60px" class="header">
        <div class="header-left">
          <span class="sys-title">简易WMS管理系统</span>
        </div>
        <div class="header-right">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/orders' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="$route.meta.title">
              {{ $route.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
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
// 桌面端布局：左侧侧边栏 + 顶部 Header + 内容区
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
  color: #606266;
  font-size: 13px;
}
.main {
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
  padding: 16px;
  overflow-y: auto;
}
</style>
