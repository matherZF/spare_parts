import request from './request'

// 上架单管理 API
export function list(params) {
  return request.get('/orders', { params })
}

// 待上架单列表（不分页）
export function pending() {
  return request.get('/orders/pending')
}

// 上架单详情
export function detail(id) {
  return request.get(`/orders/${id}`)
}

// 创建上架单
export function create(payload) {
  return request.post('/orders', payload)
}
