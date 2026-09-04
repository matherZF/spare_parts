<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑商品' : '新增商品'"
    width="520px"
    @closed="onClosed"
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="90px"
      @submit.prevent
    >
      <el-form-item label="SKU编码" prop="sku">
        <el-input
          v-model="form.sku"
          placeholder="请输入SKU（如 SP-001）"
          :disabled="isEdit"
          maxlength="32"
          clearable
        />
      </el-form-item>
      <el-form-item label="商品名称" prop="name">
        <el-input
          v-model="form.name"
          placeholder="请输入商品名称"
          maxlength="64"
          clearable
        />
      </el-form-item>
      <el-form-item label="规格" prop="spec">
        <el-input
          v-model="form.spec"
          placeholder="规格型号（选填）"
          maxlength="64"
          clearable
        />
      </el-form-item>
      <el-form-item label="单位" prop="unit">
        <el-input
          v-model="form.unit"
          placeholder="单位：个/套/件 等"
          maxlength="16"
          clearable
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as productsApi from '@/api/products'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  row: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const isEdit = computed(() => !!props.row?.id)

const formRef = ref(null)
const submitting = ref(false)

const emptyForm = () => ({
  id: null,
  sku: '',
  name: '',
  spec: '',
  unit: ''
})
const form = reactive(emptyForm())

const rules = {
  sku: [{ required: true, message: '请输入SKU编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }]
}

// 打开时：编辑模式回填
watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      Object.assign(form, emptyForm())
      if (props.row) {
        form.id = props.row.id
        form.sku = props.row.sku || ''
        form.name = props.row.name || ''
        form.spec = props.row.spec || ''
        form.unit = props.row.unit || ''
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
    const payload = {
      sku: form.sku.trim(),
      name: form.name.trim(),
      spec: form.spec?.trim() || null,
      unit: form.unit?.trim() || null
    }
    if (isEdit.value) {
      await productsApi.update(form.id, payload)
      ElMessage.success('编辑商品成功')
    } else {
      await productsApi.create(payload)
      ElMessage.success('新增商品成功')
    }
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}
</script>
