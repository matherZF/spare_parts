<template>
  <div class="page-container picking-page">
    <!-- 模式一：待拣货单列表 -->
    <template v-if="isListMode">
      <PageHeader>选择待拣货领用单</PageHeader>

      <div v-loading="pendingLoading">
        <el-empty
          v-if="!pendingLoading && pendingList.length === 0"
          description="暂无待拣货领用单"
        />

        <el-card
          v-for="order in pendingList"
          :key="order.id"
          class="order-card"
          shadow="hover"
          @click="selectOrder(order)"
        >
          <div class="order-card-header">
            <span class="order-no">{{ order.orderNo }}</span>
            <el-tag size="small" type="warning" effect="light">待拣货</el-tag>
          </div>
          <div class="order-card-info">
            <el-icon><Goods /></el-icon>
            <span>共 {{ order.itemCount }} 项货品</span>
          </div>
          <div class="order-card-time">
            创建时间：{{ formatTime(order.createdAt) }}
          </div>
        </el-card>
      </div>
    </template>

    <!-- 模式二：拣货作业 -->
    <template v-else>
      <el-page-header
        :content="null"
        @back="backToList"
        style="margin-bottom: 12px"
      >
        <template #content>
          <span style="font-size: 18px; font-weight: 600">拣货作业</span>
        </template>
      </el-page-header>

      <div v-loading="loading">
        <template v-if="detail">
          <!-- 单据基本信息 -->
          <el-card class="info-card">
            <div class="info-top">
              <span class="order-no">{{ detail.orderNo }}</span>
              <el-tag :type="statusTagType(detail.status)" effect="light" size="large">
                {{ statusText(detail.status) }}
              </el-tag>
            </div>
            <div class="info-line">
              <span class="lbl">货品项数：</span>
              <b>{{ detail.items.length }}</b>
            </div>
          </el-card>

          <!-- 状态：待拣货 -->
          <template v-if="detail.status === 'PENDING'">
            <el-alert
              type="info"
              :closable="false"
              show-icon
              style="margin-top: 16px"
            >
              <template #title>
                领用单已创建，点击下方「开始拣货」按钮，系统将为每项货品分配库位并点亮对应灯光设备。
              </template>
            </el-alert>

            <!-- 货品清单（未分配库位） -->
            <el-card style="margin-top: 16px">
              <template #header>
                <span style="font-weight: 600">货品清单</span>
              </template>
              <el-table :data="detail.items" stripe border style="width: 100%">
                <el-table-column label="货品" min-width="180">
                  <template #default="{ row }">
                    <b>{{ row.sku }}</b>
                    <span style="margin-left:6px">{{ row.name }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="requestedQty" label="需求数量" width="120" align="center" />
                <el-table-column label="库位" min-width="140">
                  <template #default>
                    <span style="color:#c0c4cc">开始拣货后分配</span>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>

            <div style="margin-top: 20px">
              <el-button
                type="primary"
                size="large"
                class="action-btn"
                :loading="starting"
                @click="handleStart"
              >
                <el-icon><VideoPlay /></el-icon>&nbsp;开始拣货
              </el-button>
            </div>
          </template>

          <!-- 状态：拣货中 -->
          <template v-else-if="detail.status === 'PICKING'">
            <el-alert
              type="warning"
              :closable="false"
              show-icon
              style="margin-top: 16px"
            >
              <template #title>
                正在拣货中，请按照下方库位指引取货，取完所有货品后点击「拣货完成」。
              </template>
            </el-alert>

            <!-- 库位指引卡片 -->
            <div style="margin-top: 16px">
              <div
                v-for="(item, idx) in detail.items"
                :key="item.id"
                class="pick-card"
              >
                <div class="pick-card-header">
                  <div class="pick-index">{{ idx + 1 }}</div>
                  <div class="pick-product">
                    <b>{{ item.sku }}</b>
                    <span style="margin-left:6px">{{ item.name }}</span>
                  </div>
                </div>
                <div class="pick-card-body">
                  <div class="pick-location">
                    <el-icon :size="20" color="#E6A23C"><Location /></el-icon>
                    <span class="loc-code">{{ item.locationCode }}</span>
                    <span v-if="item.locationArea" class="loc-area">{{ item.locationArea }}区</span>
                  </div>
                  <div class="pick-qty">
                    取货数量：<b class="qty-num">{{ item.requestedQty }}</b> 件
                  </div>
                  <div class="pick-device" v-if="item.deviceNo">
                    <el-icon><MagicStick /></el-icon>
                    <span>灯光设备：{{ item.deviceNo }}（已点亮）</span>
                  </div>
                  <div class="pick-device" v-else>
                    <el-icon><InfoFilled /></el-icon>
                    <span style="color:#909399">该库位未绑定灯光设备</span>
                  </div>
                </div>
              </div>
            </div>

            <div style="margin-top: 20px">
              <el-button
                type="success"
                size="large"
                class="action-btn"
                :loading="completing"
                @click="handleComplete"
              >
                <el-icon><CircleCheck /></el-icon>&nbsp;拣货完成
              </el-button>
            </div>
          </template>

          <!-- 状态：已完成 -->
          <template v-else-if="detail.status === 'DONE'">
            <el-result icon="success" title="拣货完成" sub-title="该领用单已全部出库" style="padding: 30px 0">
              <template #extra>
                <el-button type="primary" @click="backToList">返回领用单列表</el-button>
              </template>
            </el-result>
          </template>
        </template>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import * as outboundApi from '@/api/outbound'

const route = useRoute()
const router = useRouter()

const orderId = computed(() => route.params.id)
const isListMode = computed(() => !orderId.value || orderId.value === 'list')

// 列表模式
const pendingList = ref([])
const pendingLoading = ref(false)

// 作业模式
const detail = ref(null)
const loading = ref(false)
const starting = ref(false)
const completing = ref(false)

async function loadPending() {
  pendingLoading.value = true
  try {
    pendingList.value = await outboundApi.pending()
  } finally {
    pendingLoading.value = false
  }
}

function selectOrder(order) {
  router.push(`/outbound/picking/${order.id}`)
}

function backToList() {
  router.push('/outbound')
}

async function loadDetail() {
  if (isListMode.value) return
  loading.value = true
  try {
    detail.value = await outboundApi.detail(orderId.value)
  } finally {
    loading.value = false
  }
}

async function handleStart() {
  starting.value = true
  try {
    const res = await outboundApi.startPicking(orderId.value)
    ElMessage.success('已开始拣货，库位灯光已点亮')
    // 使用返回结果刷新明细（含库位分配）
    // startPicking 返回项用 itemId，统一映射为 id 以保持与 detail 接口一致
    if (res && res.items) {
      detail.value = {
        ...detail.value,
        status: 'PICKING',
        items: res.items.map((it) => ({
          id: it.itemId,
          productId: it.productId,
          sku: it.sku,
          name: it.name,
          requestedQty: it.requestedQty,
          pickedQty: 0,
          locationId: it.locationId,
          locationCode: it.locationCode,
          locationArea: it.locationArea,
          deviceNo: it.deviceNo,
          availableQty: it.availableQty
        }))
      }
    } else {
      await loadDetail()
    }
  } finally {
    starting.value = false
  }
}

async function handleComplete() {
  try {
    await ElMessageBox.confirm(
      '确认所有货品已按数量拣取完成？完成后将扣减库存。',
      '拣货完成确认',
      {
        type: 'warning',
        confirmButtonText: '确认完成',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }
  completing.value = true
  try {
    const res = await outboundApi.completePicking(orderId.value)
    ElMessage.success(res?.msg || '拣货完成，出库成功')
    detail.value = { ...detail.value, status: 'DONE' }
  } finally {
    completing.value = false
  }
}

function statusText(s) {
  if (s === 'PENDING') return '待拣货'
  if (s === 'PICKING') return '拣货中'
  if (s === 'DONE') return '已完成'
  return s
}

function statusTagType(s) {
  if (s === 'PENDING') return 'info'
  if (s === 'PICKING') return 'warning'
  if (s === 'DONE') return 'success'
  return ''
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

onMounted(() => {
  if (isListMode.value) {
    loadPending()
  } else {
    loadDetail()
  }
})

// 路由参数变化时（从列表进入具体单据，或返回列表）刷新数据
watch(
  () => route.params.id,
  (newId) => {
    detail.value = null
    if (!newId || newId === 'list') {
      loadPending()
    } else {
      loadDetail()
    }
  }
)
</script>

<style scoped lang="scss">
.picking-page {
  max-width: 720px;
  margin: 0 auto;
}
.order-card {
  margin-bottom: 12px;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  &:hover {
    transform: translateY(-1px);
  }
}
.order-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.order-no {
  font-weight: 700;
  font-size: 15px;
  color: #303133;
  letter-spacing: 0.3px;
}
.order-card-info {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 14px;
  margin-bottom: 4px;
}
.order-card-time {
  color: #909399;
  font-size: 12px;
}
.info-card {
  border-radius: 10px;
  .info-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
  }
  .info-line {
    font-size: 14px;
    .lbl { color: #909399; }
  }
}
.action-btn {
  width: 100%;
  min-height: 48px;
  font-size: 16px;
}
.pick-card {
  border: 1px solid #ebeef5;
  border-left: 4px solid $primary;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 12px;
  background: #fff;
}
.pick-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.pick-index {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: $primary;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}
.pick-product {
  font-size: 14px;
  color: #303133;
}
.pick-card-body {
  padding-left: 36px;
}
.pick-location {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  .loc-code {
    font-size: 18px;
    font-weight: 700;
    color: #E6A23C;
    letter-spacing: 0.5px;
  }
  .loc-area {
    font-size: 13px;
    color: #909399;
    margin-left: 4px;
  }
}
.pick-qty {
  font-size: 14px;
  color: #606266;
  margin-bottom: 4px;
  .qty-num {
    color: $danger;
    font-size: 16px;
  }
}
.pick-device {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #67C23A;
}
</style>
