<template>
  <div class="page-container">
    <!-- 返回头部 -->
    <el-page-header
      :content="null"
      @back="$router.back()"
      style="margin-bottom: 12px"
    >
      <template #content>
        <span style="font-size: 18px; font-weight: 600">上架单详情</span>
      </template>
    </el-page-header>

    <!-- 详情卡片 -->
    <el-card v-loading="loading" style="margin-bottom: 16px">
      <template v-if="detail">
        <div class="info-block">
          <div class="info-top">
            <div>
              <span class="label-muted">单号：</span>
              <span class="order-no">{{ detail.orderNo }}</span>
            </div>
            <el-tag
              size="large"
              effect="light"
              :type="detail.status === 'DONE' ? 'success' : 'warning'"
            >
              {{ detail.status === 'DONE' ? '已完成' : '待上架' }}
            </el-tag>
          </div>

          <el-divider style="margin: 14px 0" />

          <div class="info-grid">
            <div class="info-item">
              <div class="label-muted">SKU编码</div>
              <div class="value"><b>{{ detail.sku }}</b></div>
            </div>
            <div class="info-item">
              <div class="label-muted">商品名称</div>
              <div class="value">{{ detail.productName }}</div>
            </div>
            <div class="info-item">
              <div class="label-muted">计划数量</div>
              <div class="value">{{ detail.planQty }}</div>
            </div>
            <div class="info-item">
              <div class="label-muted">已上架</div>
              <div class="value" style="color: #67C23A"><b>{{ detail.putQty }}</b></div>
            </div>
            <div class="info-item">
              <div class="label-muted">剩余</div>
              <div class="value" style="color: #E6A23C">
                <b>{{ remainingQty }}</b>
              </div>
            </div>
            <div class="info-item">
              <div class="label-muted">创建时间</div>
              <div class="value">{{ formatTime(detail.createdAt) }}</div>
            </div>
          </div>

          <div style="margin-top: 14px">
            <div class="label-muted" style="margin-bottom: 8px">上架进度</div>
            <el-progress
              :percentage="detail.progress || 0"
              :status="detail.status === 'DONE' ? 'success' : undefined"
            />
          </div>
        </div>
      </template>
    </el-card>

    <!-- 已上架明细 -->
    <el-card v-loading="loading">
      <template #header>
        <span style="font-weight: 600">已上架明细（{{ detail?.items?.length || 0 }} 条）</span>
      </template>
      <el-table
        :data="detail?.items || []"
        stripe
        border
        :empty-text="detail?.status === 'PENDING' ? '暂未上架任何商品' : '无数据'"
        style="width: 100%"
      >
        <el-table-column prop="locationCode" label="库位编码" min-width="140" />
        <el-table-column prop="area" label="区域" width="120" />
        <el-table-column prop="qty" label="上架数量" width="120" align="center" />
        <el-table-column label="上架时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 底部操作按钮 -->
    <div style="margin-top: 20px">
      <el-space>
        <el-button
          v-if="detail?.status === 'PENDING'"
          type="primary"
          @click="goPutaway"
        >
          <el-icon><Check /></el-icon>&nbsp;继续上架
        </el-button>
        <el-button @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>&nbsp;返回列表
        </el-button>
      </el-space>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as ordersApi from '@/api/orders'

const route = useRoute()
const router = useRouter()

const detail = ref(null)
const loading = ref(false)

const remainingQty = computed(() => {
  if (!detail.value) return 0
  return Math.max(0, (detail.value.planQty || 0) - (detail.value.putQty || 0))
})

async function loadDetail() {
  const id = route.params.id
  if (!id) return
  loading.value = true
  try {
    detail.value = await ordersApi.detail(id)
  } finally {
    loading.value = false
  }
}

function goPutaway() {
  router.push({ path: '/putaway', query: { orderId: detail.value.id } })
}

function pad(n) {
  return n < 10 ? '0' + n : '' + n
}
function formatTime(isoStr) {
  if (!isoStr) return ''
  const d = new Date(isoStr)
  if (isNaN(d.getTime())) return ''
  return (
    d.getFullYear() +
    '-' +
    pad(d.getMonth() + 1) +
    '-' +
    pad(d.getDate()) +
    ' ' +
    pad(d.getHours()) +
    ':' +
    pad(d.getMinutes())
  )
}

onMounted(loadDetail)
</script>

<style scoped lang="scss">
.info-block {
  padding: 4px 0;
}
.info-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.order-no {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.3px;
  color: #303133;
}
.label-muted {
  color: #909399;
  font-size: 13px;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}
.info-item .value {
  margin-top: 4px;
  font-size: 14px;
  color: #303133;
}
</style>
