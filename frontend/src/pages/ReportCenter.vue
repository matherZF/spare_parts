<template>
  <div class="report-center">
    <iframe
      v-if="iframeSrc"
      :src="iframeSrc"
      class="report-iframe"
      frameborder="0"
      allowfullscreen
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'

// 积木报表工作台地址，生产环境与 WMS 同源，开发环境通过 Vite proxy 转发
const iframeSrc = computed(() => '/jmreport/list')
</script>

<style scoped lang="scss">
.report-center {
  width: 100%;
  height: 100%;
}
.report-iframe {
  width: 100%;
  height: 100%;
  min-height: calc(100vh - 120px);
  border: none;
}
</style>
