import request from './request'

// 库存查询 API

// 按商品汇总：参数 sku（SKU / 商品名关键字，实际后端按 sku 模糊匹配）
export function summary(params) {
  return request.get('/inventory/summary', { params })
}

// 按库位明细：参数 sku、locationCode
export function details(params) {
  return request.get('/inventory/details', { params })
}
