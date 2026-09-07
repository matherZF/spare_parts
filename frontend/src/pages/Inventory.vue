<template>
  <div class="page-container">
    <PageHeader>库存查询</PageHeader>

    <el-tabs v-model="activeTab" type="border-card" @tab-change="onTabChange">
      <!-- Tab1：按商品汇总 -->
      <el-tab-pane label="按商品汇总" name="summary">
        <div class="filter-bar">
          <el-input
            v-model="s.summaryKeyword"
            placeholder="SKU / 商品名称关键字"
            clearable
            style="width: 260px"
            @keyup.enter="loadSummary"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="loadSummary">
            <el-icon><Search /></el-icon>&nbsp;查询
          </el-button>
          <el-button @click="() => { s.summaryKeyword = ''; loadSummary() }">
            重置
          </el-button>
        </div>

        <el-table
          v-loading="s.loading"
          :data="summaries"
          stripe
          border
          style="width: 100%"
          :empty-text="'暂无库存数据'"
        >
          <el-table-column prop="sku" label="SKU编码" min-width="140" />
          <el-table-column prop="name" label="商品名称" min-width="180" />
          <el-table-column prop="spec" label="规格" min-width="120" />
          <el-table-column prop="unit" label="单位" width="90" align="center" />
          <el-table-column label="库存总量" width="140" align="center">
            <template #default="{ row }">
              <b class="qty-num">{{ row.totalQty || 0 }}</b>
            </template>
          </el-table-column>
        </el-table>

        <el-empty
          v-if="!s.loading && summaries.length === 0"
          description="暂无符合条件的库存汇总"
          style="margin-top: 30px"
        />
      </el-tab-pane>

      <!-- Tab2：按库位明细 -->
      <el-tab-pane label="按库位明细" name="details">
        <div class="filter-bar">
          <el-input
            v-model="s.detailSku"
            placeholder="商品 SKU 关键字"
            clearable
            style="width: 200px"
            @keyup.enter="loadDetails"
          />
          <el-input
            v-model="s.detailLocation"
            placeholder="库位编码关键字"
            clearable
            style="width: 200px"
            @keyup.enter="loadDetails"
          />
          <el-button type="primary" @click="loadDetails">
            <el-icon><Search /></el-icon>&nbsp;查询
          </el-button>
          <el-button @click="() => { s.detailSku = ''; s.detailLocation = ''; loadDetails() }">
            重置
          </el-button>
        </div>

        <el-table
          v-loading="s.dLoading"
          :data="details"
          stripe
          border
          style="width: 100%"
          :empty-text="'暂无库位明细'"
        >
          <el-table-column prop="locationCode" label="库位编码" min-width="140" />
          <el-table-column prop="area" label="区域" width="100" />
          <el-table-column prop="sku" label="SKU" min-width="120" />
          <el-table-column prop="productName" label="商品名称" min-width="160" />
          <el-table-column label="数量" width="100" align="center">
            <template #default="{ row }">
              <b class="qty-num">{{ row.qty || 0 }}</b>
            </template>
          </el-table-column>
          <el-table-column label="批次号" min-width="130">
            <template #default="{ row }">
              <b v-if="row.itemKey">{{ row.itemKey }}</b>
              <span v-else style="color:#c0c4cc">-</span>
            </template>
          </el-table-column>
          <el-table-column label="生产日期" width="110" align="center">
            <template #default="{ row }">{{ row.productionDate || '-' }}</template>
          </el-table-column>
          <el-table-column label="到期日" width="110" align="center">
            <template #default="{ row }">{{ row.expiryDate || '-' }}</template>
          </el-table-column>
          <el-table-column label="生产厂商" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ row.manufacturer || '-' }}</template>
          </el-table-column>
          <el-table-column label="更新时间" min-width="170">
            <template #default="{ row }">
              {{ formatTime(row.updatedAt) }}
            </template>
          </el-table-column>
        </el-table>

        <el-empty
          v-if="!s.dLoading && details.length === 0"
          description="暂无符合条件的库位明细"
          style="margin-top: 30px"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import * as inventoryApi from '@/api/inventory'

const activeTab = ref('summary')

const s = reactive({
  // summary
  summaryKeyword: '',
  loading: false,
  // details
  detailSku: '',
  detailLocation: '',
  dLoading: false
})

const summaries = ref([])
const details = ref([])

// 加载汇总
async function loadSummary() {
  s.loading = true
  try {
    // 后端接口实际参数名是 sku
    const r = await inventoryApi.summary({
      sku: s.summaryKeyword?.trim() || undefined
    })
    summaries.value = r || []
  } finally {
    s.loading = false
  }
}

// 加载明细
async function loadDetails() {
  s.dLoading = true
  try {
    const r = await inventoryApi.details({
      sku: s.detailSku?.trim() || undefined,
      locationCode: s.detailLocation?.trim() || undefined
    })
    details.value = r || []
  } finally {
    s.dLoading = false
  }
}

function onTabChange(name) {
  if (name === 'summary') {
    loadSummary()
  } else if (name === 'details') {
    loadDetails()
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

onMounted(() => {
  loadSummary()
  // 预加载明细，tab 切换时不白屏
  loadDetails()
})
</script>

<style scoped lang="scss">
.qty-num {
  color: $primary;
  font-size: 15px;
}
</style>
