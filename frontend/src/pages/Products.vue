<template>
  <div class="page-container">
    <PageHeader>商品管理</PageHeader>

    <!-- 过滤栏 -->
    <div class="filter-bar">
      <el-input
        v-model="filters.sku"
        placeholder="SKU"
        clearable
        style="width: 200px"
        @keyup.enter="loadData"
      />
      <el-input
        v-model="filters.name"
        placeholder="商品名称"
        clearable
        style="width: 200px"
        @keyup.enter="loadData"
      />
      <el-button type="primary" @click="loadData">
        <el-icon><Search /></el-icon>&nbsp;查询
      </el-button>
      <el-button @click="resetFilters">
        <el-icon><RefreshLeft /></el-icon>&nbsp;重置
      </el-button>
      <el-button type="success" @click="openDialog()">
        <el-icon><Plus /></el-icon>&nbsp;新增商品
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
      <el-table-column prop="sku" label="SKU编码" min-width="140" />
      <el-table-column prop="name" label="商品名称" min-width="180" />
      <el-table-column prop="spec" label="规格" min-width="120" />
      <el-table-column prop="unit" label="单位" width="90" />
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">
            <el-icon><Edit /></el-icon>&nbsp;编辑
          </el-button>
          <el-button
            size="small"
            type="danger"
            @click="handleDelete(row)"
          >
            <el-icon><Delete /></el-icon>&nbsp;删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
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

    <!-- 新增/编辑 Dialog -->
    <ProductFormDialog
      v-model="dialogVisible"
      :row="currentRow"
      @success="loadData"
    />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as productsApi from '@/api/products'
import PageHeader from '@/components/PageHeader.vue'
import ProductFormDialog from '@/components/ProductFormDialog.vue'

// 过滤条件
const filters = reactive({ sku: '', name: '' })
// 分页
const page = reactive({ page: 1, size: 20 })
// 数据
const list = ref([])
const total = ref(0)
const loading = ref(false)

// 弹窗状态
const dialogVisible = ref(false)
const currentRow = ref(null)

// 加载列表
async function loadData() {
  loading.value = true
  try {
    const r = await productsApi.list({
      sku: filters.sku?.trim() || undefined,
      name: filters.name?.trim() || undefined,
      page: page.page - 1, // 后端 Spring Page 从 0 开始
      size: page.size
    })
    list.value = r.content || []
    total.value = r.totalElements || 0
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.sku = ''
  filters.name = ''
  page.page = 1
  loadData()
}

function openDialog(row = null) {
  currentRow.value = row
  dialogVisible.value = true
}

// 删除
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除商品 ${row.name}（SKU: ${row.sku}）？若该商品已被入库单引用将无法删除。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }
  await productsApi.remove(row.id)
  ElMessage.success('删除商品成功')
  // 删除后若当前页空了，回退一页
  if (list.value.length === 1 && page.page > 1) {
    page.page -= 1
  }
  loadData()
}

// 时间格式化工具
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
