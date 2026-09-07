import request from './request'

// 出库管理（领用单）API

// 分页列表：返回 Spring Page 对象 { content, totalElements, totalPages, ... }
export function list(params) {
  return request.get('/outbound', { params })
}

// 待拣货领用单列表（不分页）
export function pending() {
  return request.get('/outbound/pending')
}

// 领用单详情
export function detail(id) {
  return request.get(`/outbound/${id}`)
}

// 创建领用单
// payload: { items: [{ productId, requestedQty }] }
export function create(payload) {
  return request.post('/outbound', payload)
}

// 开始拣货：分配库位 + 触发灯光设备
// 返回: { orderId, orderNo, items: [{ itemId, sku, name, requestedQty, locationId, locationCode, locationArea, deviceNo, availableQty, batchId, itemKey, productionDate, shelfLifeDays, manufacturer, expiryDate }] }
export function startPicking(id) {
  return request.post(`/outbound/${id}/start`)
}

// 查询某商品可用库存（含批次详情），用于人工选库位
export function availableStock(productId) {
  return request.get('/outbound/available-stock', { params: { productId } })
}

// 人工为出库项分配库位+批次
export function assignLocation(orderId, itemId, locationId, batchId) {
  const params = { locationId }
  if (batchId != null) params.batchId = batchId
  return request.post(`/outbound/${orderId}/items/${itemId}/assign`, null, { params })
}

// 拣货完成：扣减库存
// 返回: { orderId, done, msg }
export function completePicking(id) {
  return request.post(`/outbound/${id}/complete`)
}
