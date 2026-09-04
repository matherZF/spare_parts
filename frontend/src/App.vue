<template>
  <!-- 登录页不使用布局外壳 -->
  <router-view v-if="isLoginPage" />

  <!-- 桌面端布局 -->
  <DesktopLayout v-else-if="!isMobile">
    <router-view />
  </DesktopLayout>
  <!-- 移动端布局 -->
  <MobileLayout v-else>
    <router-view />
  </MobileLayout>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import DesktopLayout from '@/layouts/DesktopLayout.vue'
import MobileLayout from '@/layouts/MobileLayout.vue'

const route = useRoute()

// 登录页不显示布局
const isLoginPage = computed(() => route.path === '/login')

// 响应式断点：宽度 < 768px 视为移动端
const isMobile = ref(false)

function checkViewport() {
  isMobile.value = window.innerWidth < 768
}

let resizeTimer = null
function onResize() {
  // 节流，避免频繁触发
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(checkViewport, 100)
}

onMounted(() => {
  checkViewport()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (resizeTimer) clearTimeout(resizeTimer)
})
</script>
