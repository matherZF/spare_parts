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
                可点击「开始拣货」由系统按 FIFO 自动分配库位，也可逐项「选择库位」人工指定批次。
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
                <el-table-column prop="requestedQty" label="需求数量" width="110" align="center" />
                <el-table-column label="已选库位/批次" min-width="200">
                  <template #default="{ row }">
                    <template v-if="row.locationCode">
                      <div><b>{{ row.locationCode }}</b></div>
                      <div style="font-size:12px;color:#909399" v-if="row.itemKey">
                        批次：{{ row.itemKey }}
                      </div>
                    </template>
                    <span v-else style="color:#c0c4cc">未分配</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120" align="center">
                  <template #default="{ row }">
                    <el-button type="primary" size="small" link @click="openSelectDialog(row)">
                      选择库位
                    </el-button>
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
                  <!-- 批次信息 -->
                  <div class="pick-batch" v-if="item.itemKey">
                    <el-icon><Box /></el-icon>
                    <span class="batch-label">批次：</span>
                    <b>{{ item.itemKey }}</b>
                    <span v-if="item.productionDate" class="batch-meta">
                      生产 {{ item.productionDate }}
                    </span>
                    <span v-if="item.expiryDate" class="batch-meta">
                      到期 {{ item.expiryDate }}
                    </span>
                    <span v-if="item.manufacturer" class="batch-meta">
                      {{ item.manufacturer }}
                    </span>
                  </div>
                  <div class="pick-device" v-if="item.deviceNo">
                    <el-icon><MagicStick /></el-icon>
                    <span>灯光设备：{{ item.deviceNo }}（已点亮）</span>
                  </div>
                  <div class="pick-device" v-else>
                    <el-icon><InfoFilled /></el-icon>
                    <span style="color:#909399">该库位未绑定灯光设备</span>
                  </div>
                  <div style="margin-top: 8px">
                    <el-button size="small" type="primary" link @click="openSelectDialog(item)">
                      更换库位
                    </el-button>
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

    <!-- 人工选择库位 Dialog -->
    <el-dialog
      v-model="selectDialogVisible"
      title="选择库位 / 批次"
      :width="dialogWidth"
      destroy-on-close
      @closed="onSelectClosed"
    >
      <div v-loading="stockLoading">
        <el-alert
          v-if="currentSelectItem"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 12px"
        >
          <template #title>
            货品：<b>{{ currentSelectItem.sku }} - {{ currentSelectItem.name }}</b>
            ，需求数量 <b>{{ currentSelectItem.requestedQty }}</b> 件
          </template>
        </el-alert>

        <el-empty
          v-if="!stockLoading && availableStock.length === 0"
          description="该货品暂无可用库存"
        />

        <el-table
          v-else
          :data="availableStock"
          stripe
          border
          style="width: 100%"
          highlight-current-row
          @current-change="onStockSelect"
        >
          <el-table-column label="库位" min-width="110">
            <template #default="{ row }">
              <b>{{ row.locationCode }}</b>
              <div style="font-size:11px;color:#909399">{{ row.locationArea || '-' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="可用数量" width="90" align="center">
            <template #default="{ row }">
              <b :class="{ 'stock-low': row.qty < (currentSelectItem?.requestedQty || 0) }">
                {{ row.qty }}
              </b>
            </template>
          </el-table-column>
          <el-table-column label="批次号" min-width="130">
            <template #default="{ row }">
              <b>{{ row.itemKey || '-' }}</b>
            </template>
          </el-table-column>
          <el-table-column label="生产日期" width="110" align="center">
            <template #default="{ row }">{{ row.productionDate || '-' }}</template>
          </el-table-column>
          <el-table-column label="到期日" width="110" align="center">
            <template #default="{ row }">
              <span :class="{ 'expire-soon': isExpiringSoon(row.expiryDate) }">
                {{ row.expiryDate || '-' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="保质期(天)" width="90" align="center">
            <template #default="{ row }">{{ row.shelfLifeDays || '-' }}</template>
          </el-table-column>
          <el-table-column label="生产厂商" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ row.manufacturer || '-' }}</template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="selectDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!selectedStock"
          :loading="assigning"
          @click="confirmAssign"
        >
          确认选择
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import * as outboundApi from '@/api/outbound'
import { useMobile } from '@/composables/useMobile'

const route = useRoute()
const router = useRouter()
const { isMobile } = useMobile()
const dialogWidth = computed(() => (isMobile.value ? '92%' : '720px'))

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

// 人工选库位
const selectDialogVisible = ref(false)
const currentSelectItem = ref(null)
const availableStock = ref([])
const stockLoading = ref(false)
const selectedStock = ref(null)
const assigning = ref(false)

async function loadPending() {
  pendingLoading.value = true
  try {
    pendingList.value = await outboundApi.pending()
  } finally {
    pendingLoading.value = false
  }
}

function selectOrder(order) {
  if (order && order.id != null) {
    router.push(`/outbound/picking/${order.id}`)
  }
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
          availableQty: it.availableQty,
          batchId: it.batchId,
          itemKey: it.itemKey,
          productionDate: it.productionDate,
          shelfLifeDays: it.shelfLifeDays,
          manufacturer: it.manufacturer,
          expiryDate: it.expiryDate
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

// ===== 人工选库位 =====
async function openSelectDialog(item) {
  currentSelectItem.value = item
  selectedStock.value = null
  selectDialogVisible.value = true
  stockLoading.value = true
  try {
    availableStock.value = await outboundApi.availableStock(item.productId)
  } finally {
    stockLoading.value = false
  }
}

function onStockSelect(row) {
  selectedStock.value = row
}

function onSelectClosed() {
  currentSelectItem.value = null
  availableStock.value = []
  selectedStock.value = null
}

async function confirmAssign() {
  if (!selectedStock.value || !currentSelectItem.value) return
  const stock = selectedStock.value
  if (stock.qty < currentSelectItem.value.requestedQty) {
    ElMessage.warning(`所选库存不足（仅剩 ${stock.qty} 件）`)
    return
  }
  assigning.value = true
  try {
    const updated = await outboundApi.assignLocation(
      orderId.value,
      currentSelectItem.value.id,
      stock.locationId,
      stock.batchId
    )
    // 更新本地明细中对应项
    const items = detail.value.items.map((it) =>
      it.id === updated.id ? { ...it, ...updated } : it
    )
    detail.value = { ...detail.value, items }
    ElMessage.success(`已分配库位 ${stock.locationCode}（批次 ${stock.itemKey || '无'}）`)
    selectDialogVisible.value = false
  } finally {
    assigning.value = false
  }
}

function isExpiringSoon(expiryDate) {
  if (!expiryDate) return false
  const days = Math.ceil((new Date(expiryDate) - new Date()) / 86400000)
  return days <= 30
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
  margin-bottom: 6px;
  .qty-num {
    color: $danger;
    font-size: 16px;
  }
}
.pick-batch {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
  background: #f5f7fa;
  padding: 6px 10px;
  border-radius: 6px;
  .batch-label { color: #909399; }
  .batch-meta { color: #909399; font-size: 12px; }
}
.pick-device {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #67C23A;
}
.stock-low {
  color: #F56C6C;
}
.expire-soon {
  color: #E6A23C;
  font-weight: 600;
}
</style>
