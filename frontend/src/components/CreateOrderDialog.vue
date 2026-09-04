<template>
  <el-dialog
    v-model="visible"
    title="新建上架单"
    width="520px"
    @closed="onClosed"
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      @submit.prevent
    >
      <el-form-item label="选择商品" prop="productId">
        <el-select
          v-model="form.productId"
          filterable
          placeholder="搜索或选择商品"
          style="width: 100%"
          :loading="productsLoading"
        >
          <el-option
            v-for="p in productOptions"
            :key="p.id"
            :label="`${p.sku} - ${p.name}`"
            :value="p.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="计划上架数" prop="planQty">
        <el-input-number
          v-model="form.planQty"
          :min="1"
          :max="99999"
          style="width: 100%"
          controls-position="right"
        />
      </el-form-item>
      <el-alert
        v-if="selectedProduct"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 0"
      >
        <template #title>
          已选：{{ selectedProduct.sku }} / {{ selectedProduct.name }}
          <span v-if="selectedProduct.spec">（规格：{{ selectedProduct.spec }}）</span>
        </template>
      </el-alert>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">
        确定创建
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as ordersApi from '@/api/orders'
import * as productsApi from '@/api/products'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const formRef = ref(null)
const submitting = ref(false)
const productsLoading = ref(false)
const productList = ref([])

const emptyForm = () => ({ productId: null, planQty: 100 })
const form = reactive(emptyForm())

const rules = {
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  planQty: [{ required: true, message: '请输入计划数量', trigger: 'blur' }]
}

// 商品全量列表（分页取大值即可）
async function loadProducts() {
  productsLoading.value = true
  try {
    const r = await productsApi.list({ page: 0, size: 500 })
    productList.value = r.content || []
  } finally {
    productsLoading.value = false
  }
}

const productOptions = computed(() => productList.value)

const selectedProduct = computed(() => {
  if (!form.productId) return null
  return productList.value.find((p) => p.id === form.productId) || null
})

watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      Object.assign(form, emptyForm())
      if (productList.value.length === 0) {
        loadProducts()
      }
    }
  }
)

function onClosed() {
  formRef.value?.resetFields()
  Object.assign(form, emptyForm())
}

async function submit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const res = await ordersApi.create({
      productId: form.productId,
      planQty: form.planQty
    })
    ElMessage.success(`创建上架单成功：${res.orderNo}`)
    visible.value = false
    emit('success', res)
  } finally {
    submitting.value = false
  }
}
</script>
