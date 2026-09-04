import request from './request'

// 认证相关 API
export default {
  /** 登录：POST /api/auth/login { username, password } → { token, userId, username, displayName, role } */
  login(username, password) {
    return request.post('/auth/login', { username, password })
  },
  /** 获取当前登录用户信息：GET /api/auth/me → UserDTO */
  me() {
    return request.get('/auth/me')
  }
}
