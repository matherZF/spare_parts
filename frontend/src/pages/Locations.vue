<template>
  <div class="page-container">
    <PageHeader>库位管理</PageHeader>

    <!-- 过滤栏 -->
    <div class="filter-bar">
      <el-input
        v-model="filters.code"
        placeholder="库位编码"
        clearable
        style="width: 200px"
        @keyup.enter="loadData"
      />
      <el-input
        v-model="filters.area"
        placeholder="所属区域"
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
        <el-icon><Plus /></el-icon>&nbsp;新增库位
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
      <el-table-column prop="code" label="库位编码" min-width="140" />
      <el-table-column prop="area" label="所属区域" min-width="120" />
      <el-table-column label="类型" min-width="120">
        <template #default="{ row }">
          <el-tag v-if="row.type" :type="typeTagColor(row.type)">
            {{ row.type }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
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

    <LocationFormDialog
      v-model="dialogVisible"
      :row="currentRow"
      @success="loadData"
    />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as locationsApi from '@/api/locations'
import PageHeader from '@/components/PageHeader.vue'
import LocationFormDialog from '@/components/LocationFormDialog.vue'

const filters = reactive({ code: '', area: '' })
const page = reactive({ page: 1, size: 20 })
const list = ref([])
const total = ref(0)
const loading = ref(false)

const dialogVisible = ref(false)
const currentRow = ref(null)

async function loadData() {
  loading.value = true
  try {
    const r = await locationsApi.list({
      code: filters.code?.trim() || undefined,
      area: filters.area?.trim() || undefined,
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
  filters.code = ''
  filters.area = ''
  page.page = 1
  loadData()
}

function openDialog(row = null) {
  currentRow.value = row
  dialogVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除库位 ${row.code}？库位存在库存将无法删除。`,
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
  await locationsApi.remove(row.id)
  ElMessage.success('删除库位成功')
  if (list.value.length === 1 && page.page > 1) {
    page.page -= 1
  }
  loadData()
}

// 类型 → 标签色
function typeTagColor(type) {
  switch (type) {
    case '常温': return ''
    case '冷藏': return 'info'
    case '大件': return 'warning'
    case '危险品': return 'danger'
    default: return 'success'
  }
}

onMounted(loadData)
</script>
