<template>
  <div class="page-container">
    <PageHeader>库存日志</PageHeader>

    <el-card class="log-card" shadow="never">
      <!-- 筛选条件 -->
      <div class="filter-bar">
        <el-input
          v-model="s.sku"
          placeholder="SKU / 商品名称"
          clearable
          style="width: 220px"
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-input
          v-model="s.locationCode"
          placeholder="库位编码"
          clearable
          style="width: 180px"
          @keyup.enter="onSearch"
        />
        <el-select
          v-model="s.changeType"
          placeholder="变动类型"
          clearable
          style="width: 150px"
        >
          <el-option label="入库" value="INBOUND" />
          <el-option label="出库" value="OUTBOUND" />
        </el-select>
        <el-button type="primary" @click="onSearch">
          <el-icon><Search /></el-icon>&nbsp;查询
        </el-button>
        <el-button @click="onReset">重置</el-button>
      </div>

      <!-- 日志表格 -->
      <el-table
        v-loading="s.loading"
        :data="list"
        stripe
        border
        style="width: 100%; margin-top: 16px"
        :empty-text="'暂无库存变动记录'"
      >
        <el-table-column label="时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="变动类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.changeType === 'INBOUND' ? 'success' : 'warning'"
              effect="light"
            >
              {{ row.changeType === 'INBOUND' ? '入库' : '出库' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sku" label="SKU" min-width="120" />
        <el-table-column prop="productName" label="商品名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="locationCode" label="库位" width="120" />
        <el-table-column label="变动数量" width="110" align="center">
          <template #default="{ row }">
            <span :class="row.changeType === 'INBOUND' ? 'qty-in' : 'qty-out'">
              {{ row.changeType === 'INBOUND' ? '+' : '-' }}{{ row.changeQty }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="变动前" width="100" align="center">
          <template #default="{ row }">{{ row.beforeQty }}</template>
        </el-table-column>
        <el-table-column label="变动后" width="100" align="center">
          <template #default="{ row }">
            <b class="qty-num">{{ row.afterQty }}</b>
          </template>
        </el-table-column>
        <el-table-column label="关联单据" min-width="150">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" v-if="row.refType">
              {{ refTypeLabel(row.refType) }} · {{ row.refNo }}
            </el-tag>
            <span v-else>-</span>
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
          <template #default="{ row }">
            <span v-if="row.expiryDate" :class="{ 'expire-soon': isExpiringSoon(row.expiryDate) }">
              {{ row.expiryDate }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="生产厂商" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.manufacturer || '-' }}</template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="110" align="center">
          <template #default="{ row }">{{ row.operator || '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="s.page"
          v-model:page-size="s.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="s.total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="load"
          @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import * as inventoryLogApi from '@/api/inventoryLog'

const s = reactive({
  sku: '',
  locationCode: '',
  changeType: '',
  page: 1,
  size: 20,
  total: 0,
  loading: false
})

const list = ref([])

async function load() {
  s.loading = true
  try {
    // 后端 Page 的页码从 0 开始，前端从 1 开始
    const params = {
      page: s.page - 1,
      size: s.size
    }
    if (s.sku?.trim()) params.sku = s.sku.trim()
    if (s.locationCode?.trim()) params.locationCode = s.locationCode.trim()
    if (s.changeType) params.changeType = s.changeType

    const res = await inventoryLogApi.page(params)
    list.value = (res && res.content) || []
    s.total = (res && res.totalElements) || 0
  } finally {
    s.loading = false
  }
}

function onSearch() {
  s.page = 1
  load()
}

function onReset() {
  s.sku = ''
  s.locationCode = ''
  s.changeType = ''
  s.page = 1
  load()
}

function refTypeLabel(type) {
  if (type === 'PUTAWAY') return '上架单'
  if (type === 'OUTBOUND') return '出库单'
  return type
}

function isExpiringSoon(expiryDate) {
  if (!expiryDate) return false
  const days = Math.ceil((new Date(expiryDate) - new Date()) / 86400000)
  return days <= 30
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
    pad(d.getMinutes()) +
    ':' +
    pad(d.getSeconds())
  )
}

onMounted(() => {
  load()
})
</script>

<style scoped lang="scss">
.log-card {
  border-radius: 6px;
}
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.qty-num {
  color: $primary;
}
.qty-in {
  color: #67c23a;
  font-weight: 600;
}
.qty-out {
  color: #e6a23c;
  font-weight: 600;
}
.expire-soon {
  color: #e6a23c;
  font-weight: 600;
}
</style>
