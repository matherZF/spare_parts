<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑库位' : '新增库位'"
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
      <el-form-item label="库位编码" prop="code">
        <el-input
          v-model="form.code"
          placeholder="请输入库位编码（如 A-01-01）"
          :disabled="isEdit"
          maxlength="32"
          clearable
        />
      </el-form-item>
      <el-form-item label="所属区域" prop="area">
        <el-input
          v-model="form.area"
          placeholder="如 A区 / B区"
          maxlength="32"
          clearable
        />
      </el-form-item>
      <el-form-item label="库位类型" prop="type">
        <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
          <el-option label="常温" value="常温" />
          <el-option label="冷藏" value="冷藏" />
          <el-option label="大件" value="大件" />
          <el-option label="危险品" value="危险品" />
          <el-option label="其他" value="其他" />
        </el-select>
      </el-form-item>
      <el-form-item label="设备编号" prop="deviceNo">
        <el-input
          v-model="form.deviceNo"
          placeholder="绑定的灯光提示设备编号（选填）"
          maxlength="64"
          clearable
        />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
          placeholder="选填"
          maxlength="128"
          show-word-limit
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
import * as locationsApi from '@/api/locations'

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
  code: '',
  area: '',
  type: '',
  deviceNo: '',
  remark: ''
})
const form = reactive(emptyForm())

const rules = {
  code: [{ required: true, message: '请输入库位编码', trigger: 'blur' }],
  type: [{ required: true, message: '请选择库位类型', trigger: 'change' }]
}

watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      Object.assign(form, emptyForm())
      if (props.row) {
        form.id = props.row.id
        form.code = props.row.code || ''
        form.area = props.row.area || ''
        form.type = props.row.type || ''
        form.deviceNo = props.row.deviceNo || ''
        form.remark = props.row.remark || ''
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
      code: form.code.trim(),
      area: form.area?.trim() || null,
      type: form.type || null,
      deviceNo: form.deviceNo?.trim() || null,
      remark: form.remark?.trim() || null
    }
    if (isEdit.value) {
      await locationsApi.update(form.id, payload)
      ElMessage.success('编辑库位成功')
    } else {
      await locationsApi.create(payload)
      ElMessage.success('新增库位成功')
    }
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}
</script>
