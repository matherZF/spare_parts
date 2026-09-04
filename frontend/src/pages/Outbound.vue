<template>
  <div class="page-container">
    <PageHeader>出库管理</PageHeader>

    <!-- 顶部快捷入口卡片 -->
    <el-row :gutter="16" style="margin-bottom: 20px">
      <el-col :xs="24" :sm="12" :md="8" style="margin-bottom: 12px">
        <el-card class="quick-card" shadow="hover" @click="openCreateDialog">
          <div class="quick-card-body">
            <div class="quick-icon primary-bg"><el-icon :size="28"><Plus /></el-icon></div>
            <div class="quick-info">
              <div class="quick-title">新建领用单</div>
              <div class="quick-sub">选择货品及数量，生成领用单</div>
            </div>
            <el-icon class="quick-arrow"><ArrowRight /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" style="margin-bottom: 12px">
        <el-card class="quick-card" shadow="hover" @click="goPicking">
          <div class="quick-card-body">
            <div class="quick-icon success-bg"><el-icon :size="28"><Box /></el-icon></div>
            <div class="quick-info">
              <div class="quick-title">进入拣货作业</div>
              <div class="quick-sub">按库位指引完成拣货出库</div>
            </div>
            <el-icon class="quick-arrow"><ArrowRight /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" style="margin-bottom: 12px">
        <el-card class="quick-card" shadow="hover" @click="goInventory">
          <div class="quick-card-body">
            <div class="quick-icon warning-bg"><el-icon :size="28"><DataAnalysis /></el-icon></div>
            <div class="quick-info">
              <div class="quick-title">查看库存汇总</div>
              <div class="quick-sub">查询当前库存情况</div>
            </div>
            <el-icon class="quick-arrow"><ArrowRight /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 过滤栏 -->
    <div class="filter-bar">
      <el-select v-model="filters.status" placeholder="状态筛选" style="width: 160px" clearable @change="onStatusChange">
        <el-option label="全部" value="" />
        <el-option label="待拣货" value="PENDING" />
        <el-option label="拣货中" value="PICKING" />
        <el-option label="已完成" value="DONE" />
      </el-select>
      <el-input
        v-model="filters.keyword"
        placeholder="领用单号"
        clearable
        style="width: 240px"
        @keyup.enter="loadData"
      />
      <el-button type="primary" @click="loadData">
        <el-icon><Search /></el-icon>&nbsp;查询
      </el-button>
      <el-button @click="resetFilters">
        <el-icon><RefreshLeft /></el-icon>&nbsp;重置
      </el-button>
      <el-button type="success" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>&nbsp;新建领用单
      </el-button>
    </div>

    <!-- 数据表格 -->
    <el-table
      v-loading="loading"
      :data="list"
      stripe
      border
      style="width: 100%"
    >
      <el-table-column prop="orderNo" label="领用单号" min-width="170" />
      <el-table-column prop="itemCount" label="货品项数" width="100" align="center" />
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" effect="light">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'PENDING' || row.status === 'PICKING'"
            type="primary"
            size="small"
            link
            @click="goPicking(row)"
          >
            拣货
          </el-button>
          <el-button size="small" link @click="viewDetail(row)">
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 16px; display: flex; justify-content: flex-end">
      <el-pagination
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        v-model:current-page="page.page"
        v-model:page-size="page.size"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 新建领用单 Dialog -->
    <el-dialog
      v-model="createDialogVisible"
      title="新建领用单"
      width="640px"
      @closed="onCreateClosed"
      destroy-on-close
    >
      <el-form :model="createForm" label-width="0" @submit.prevent>
        <div class="items-header">
          <span style="font-weight: 600">货品明细</span>
          <el-button type="primary" size="small" @click="addItem">
            <el-icon><Plus /></el-icon>&nbsp;添加货品
          </el-button>
        </div>

        <div
          v-for="(item, idx) in createForm.items"
          :key="idx"
          class="item-row"
        >
          <el-select
            v-model="item.productId"
            filterable
            placeholder="搜索或选择货品"
            style="flex: 1"
            :loading="productsLoading"
          >
            <el-option
              v-for="p in productList"
              :key="p.id"
              :label="`${p.sku} - ${p.name}`"
              :value="p.id"
            />
          </el-select>
          <el-input-number
            v-model="item.requestedQty"
            :min="1"
            :max="99999"
            controls-position="right"
            style="width: 160px; margin-left: 12px"
          />
          <el-button
            type="danger"
            link
            style="margin-left: 8px"
            :disabled="createForm.items.length <= 1"
            @click="removeItem(idx)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>

        <el-empty
          v-if="createForm.items.length === 0"
          description="请添加货品"
          :image-size="60"
        />
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">
          确定创建
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情 Dialog -->
    <el-dialog
      v-model="detailDialogVisible"
      title="领用单详情"
      width="640px"
      destroy-on-close
    >
      <div v-loading="detailLoading">
        <template v-if="detail">
          <div class="detail-top">
            <span class="detail-order-no">{{ detail.orderNo }}</span>
            <el-tag :type="statusTagType(detail.status)" effect="light">
              {{ statusText(detail.status) }}
            </el-tag>
          </div>
          <el-table
            :data="detail.items"
            stripe
            border
            style="width: 100%; margin-top: 12px"
          >
            <el-table-column label="货品" min-width="180">
              <template #default="{ row }">
                <b>{{ row.sku }}</b>
                <span style="margin-left:6px">{{ row.name }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="requestedQty" label="需求数量" width="100" align="center" />
            <el-table-column prop="pickedQty" label="已拣数量" width="100" align="center" />
            <el-table-column label="库位" min-width="140">
              <template #default="{ row }">
                <span v-if="row.locationCode">
                  {{ row.locationCode }}
                  <span v-if="row.locationArea" style="color:#909399">({{ row.locationArea }})</span>
                </span>
                <span v-else style="color:#c0c4cc">未分配</span>
              </template>
            </el-table-column>
            <el-table-column label="设备" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.deviceNo" size="small" type="warning" effect="plain">
                  {{ row.deviceNo }}
                </el-tag>
                <span v-else style="color:#c0c4cc">未绑定</span>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import * as outboundApi from '@/api/outbound'
import * as productsApi from '@/api/products'

const router = useRouter()

const filters = reactive({ status: '', keyword: '' })
const page = reactive({ page: 1, size: 20 })
const list = ref([])
const total = ref(0)
const loading = ref(false)

const createDialogVisible = ref(false)
const submitting = ref(false)
const productsLoading = ref(false)
const productList = ref([])

const createForm = reactive({ items: [{ productId: null, requestedQty: 1 }] })

const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

async function loadData() {
  loading.value = true
  try {
    const r = await outboundApi.list({
      status: filters.status || undefined,
      page: page.page - 1,
      size: page.size
    })
    list.value = r.content || []
    total.value = r.totalElements || 0
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.status = ''
  filters.keyword = ''
  page.page = 1
  loadData()
}

function onStatusChange() {
  page.page = 1
  loadData()
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

function openCreateDialog() {
  createForm.items = [{ productId: null, requestedQty: 1 }]
  createDialogVisible.value = true
  loadProducts()
}

function onCreateClosed() {
  createForm.items = [{ productId: null, requestedQty: 1 }]
}

async function loadProducts() {
  if (productList.value.length > 0) return
  productsLoading.value = true
  try {
    const r = await productsApi.list({ page: 0, size: 500 })
    productList.value = r.content || []
  } finally {
    productsLoading.value = false
  }
}

function addItem() {
  createForm.items.push({ productId: null, requestedQty: 1 })
}

function removeItem(idx) {
  createForm.items.splice(idx, 1)
}

async function submitCreate() {
  const items = createForm.items
  if (items.length === 0) {
    ElMessage.warning('请至少添加一个货品')
    return
  }
  for (let i = 0; i < items.length; i++) {
    if (!items[i].productId) {
      ElMessage.warning(`第 ${i + 1} 行请选择货品`)
      return
    }
    if (!items[i].requestedQty || items[i].requestedQty <= 0) {
      ElMessage.warning(`第 ${i + 1} 行数量必须大于 0`)
      return
    }
  }
  // 检查重复货品
  const ids = items.map((it) => it.productId)
  if (new Set(ids).size !== ids.length) {
    ElMessage.warning('存在重复货品，请合并数量')
    return
  }
  submitting.value = true
  try {
    const res = await outboundApi.create({
      items: items.map((it) => ({
        productId: it.productId,
        requestedQty: it.requestedQty
      }))
    })
    ElMessage.success(`创建领用单成功：${res.orderNo}`)
    createDialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function goPicking(row) {
  if (row) {
    router.push(`/outbound/picking/${row.id}`)
  } else {
    router.push('/outbound/picking/list')
  }
}

function goInventory() {
  router.push('/inventory')
}

async function viewDetail(row) {
  detail.value = null
  detailDialogVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await outboundApi.detail(row.id)
  } finally {
    detailLoading.value = false
  }
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

onMounted(loadData)
</script>

<style scoped lang="scss">
.quick-card {
  cursor: pointer;
  transition: transform 0.2s;
  &:hover {
    transform: translateY(-2px);
  }
}
.quick-card-body {
  display: flex;
  align-items: center;
  gap: 14px;
}
.quick-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.primary-bg { background: $primary; }
.success-bg { background: $success; }
.warning-bg { background: $warning; }
.quick-info {
  flex: 1;
  min-width: 0;
}
.quick-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}
.quick-sub {
  font-size: 12px;
  color: #909399;
}
.quick-arrow {
  color: #c0c4cc;
}
.items-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.item-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
.detail-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.detail-order-no {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}
</style>
