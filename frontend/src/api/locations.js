import request from './request'

// 库位管理 API
export function list(params) {
  return request.get('/locations', { params })
}

export function detail(id) {
  return request.get(`/locations/${id}`)
}

export function create(payload) {
  return request.post('/locations', payload)
}

export function update(id, payload) {
  return request.put(`/locations/${id}`, payload)
}

export function remove(id) {
  return request.delete(`/locations/${id}`)
}
