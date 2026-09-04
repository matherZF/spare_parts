<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <el-icon class="logo-icon"><Box /></el-icon>
        <h1 class="login-title">简易 WMS 管理系统</h1>
        <p class="login-subtitle">请登录后继续操作</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="handleLogin">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>
      <div class="login-tip">
        <p>默认管理员：admin / admin123</p>
        <p>默认作业员：operator / operator123</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await authStore.login(form.username, form.password)
      ElMessage.success('登录成功')
      router.push('/orders')
    } catch {
      // 错误已由 axios 拦截器统一 toast
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #304156 0%, #1f3a5f 100%);
  padding: 20px;
}
.login-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 32px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}
.login-header {
  text-align: center;
  margin-bottom: 32px;
}
.logo-icon {
  font-size: 48px;
  color: $primary;
  margin-bottom: 12px;
}
.login-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 8px;
}
.login-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}
.login-btn {
  width: 100%;
  min-height: 44px;
  font-size: 16px;
}
.login-tip {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  p {
    font-size: 12px;
    color: #c0c4cc;
    margin: 4px 0;
    text-align: center;
  }
}
</style>
