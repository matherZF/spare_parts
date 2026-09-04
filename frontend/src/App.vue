<template>
  <!-- 桌面端布局 -->
  <DesktopLayout v-if="!isMobile">
    <router-view />
  </DesktopLayout>
  <!-- 移动端布局 -->
  <MobileLayout v-else>
    <router-view />
  </MobileLayout>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import DesktopLayout from '@/layouts/DesktopLayout.vue'
import MobileLayout from '@/layouts/MobileLayout.vue'

// 响应式断点：宽度 < 768px 视为移动端
const isMobile = ref(false)

function checkViewport() {
  isMobile.value = window.innerWidth < 768
  // 可打开下面注释用于自测
  // console.log('[App.vue] viewport width:', window.innerWidth, 'isMobile:', isMobile.value)
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
