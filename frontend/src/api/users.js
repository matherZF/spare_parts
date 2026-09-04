import request from './request'

// 用户管理 API（仅 ADMIN 角色可访问）
export default {
  /** 分页查询用户列表 */
  list(params = {}) {
    return request.get('/users', { params })
  },
  /** 获取用户详情 */
  detail(id) {
    return request.get(`/users/${id}`)
  },
  /** 创建新用户 */
  create(payload) {
    return request.post('/users', payload)
  },
  /** 修改用户信息（password 为可选字段） */
  update(id, payload) {
    return request.put(`/users/${id}`, payload)
  },
  /** 删除用户 */
  remove(id) {
    return request.delete(`/users/${id}`)
  }
}
