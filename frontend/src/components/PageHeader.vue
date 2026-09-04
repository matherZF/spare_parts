<template>
  <div class="page-header-comp">
    <h2 class="page-title">
      <slot />
    </h2>
    <div v-if="$slots.desc" class="page-desc">
      <slot name="desc" />
    </div>
  </div>
</template>

<script setup>
// 通用页面标题组件
</script>

<style scoped lang="scss">
.page-header-comp {
  margin-bottom: 16px;
}
.page-desc {
  color: #909399;
  font-size: 13px;
  margin-top: -8px;
  margin-bottom: 12px;
}
</style>
