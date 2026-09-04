<template>
  <div class="page-container">
    <PageHeader>上架单管理</PageHeader>

    <!-- 顶部快捷入口卡片 -->
    <el-row :gutter="16" style="margin-bottom: 20px">
      <el-col :xs="24" :sm="12" :md="8" style="margin-bottom: 12px">
        <el-card class="quick-card" shadow="hover" @click="openCreateDialog">
          <div class="quick-card-body">
            <div class="quick-icon primary-bg"><el-icon :size="28"><Plus /></el-icon></div>
            <div class="quick-info">
              <div class="quick-title">新建上架单</div>
              <div class="quick-sub">快速创建一个新的上架单</div>
            </div>
            <el-icon class="quick-arrow"><ArrowRight /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" style="margin-bottom: 12px">
        <el-card class="quick-card" shadow="hover" @click="goPutaway">
          <div class="quick-card-body">
            <div class="quick-icon success-bg"><el-icon :size="28"><Check /></el-icon></div>
            <div class="quick-info">
              <div class="quick-title">进入上架作业</div>
              <div class="quick-sub">开始执行上架流程</div>
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
        <el-option label="待上架" value="PENDING" />
        <el-option label="已完成" value="DONE" />
      </el-select>
      <el-input
        v-model="filters.keyword"
        placeholder="商品 SKU / 名称"
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
        <el-icon><Plus /></el-icon>&nbsp;新建上架单
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
      <el-table-column prop="orderNo" label="单号" min-width="170" />
      <el-table-column label="商品" min-width="220">
        <template #default="{ row }">
          <b>{{ row.sku }}</b>
          <span style="color:#909399; margin-left:6px">-</span>
          <span style="margin-left:6px">{{ row.productName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="计划/已上架" width="130" align="center">
        <template #default="{ row }">
          {{ row.planQty }} / <b>{{ row.putQty }}</b>
        </template>
      </el-table-column>
      <el-table-column label="进度" min-width="180">
        <template #default="{ row }">
          <el-progress
            :percentage="row.progress || 0"
            :status="row.status === 'DONE' ? 'success' : undefined"
          />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'DONE' ? 'success' : 'info'" effect="light">
            {{ row.status === 'DONE' ? '已完成' : '待上架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="goDetail(row)">
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

    <!-- 新建单 Dialog -->
    <CreateOrderDialog
      v-model="createDialogVisible"
      @success="onCreateSuccess"
    />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import CreateOrderDialog from '@/components/CreateOrderDialog.vue'
import * as ordersApi from '@/api/orders'

const router = useRouter()

const filters = reactive({ status: '', keyword: '' })
const page = reactive({ page: 1, size: 20 })
const list = ref([])
const total = ref(0)
const loading = ref(false)

const createDialogVisible = ref(false)

async function loadData() {
  loading.value = true
  try {
    const r = await ordersApi.list({
      status: filters.status || undefined,
      keyword: filters.keyword?.trim() || undefined,
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

function openCreateDialog() {
  createDialogVisible.value = true
}

function onCreateSuccess() {
  loadData()
}

function goDetail(row) {
  router.push(`/orders/${row.id}`)
}

function goPutaway() {
  router.push('/putaway')
}

function goInventory() {
  router.push('/inventory')
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
</style>
