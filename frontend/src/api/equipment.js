import request from './request'
export const list = (params) => request.get('/equipments', { params })
export const create = (data) => request.post('/equipments', data)
export const update = (id, data) => request.put(`/equipments/${id}`, data)
export const parts = (id) => request.get(`/equipments/${id}/parts`)
export const bindPart = (id, data) => request.post(`/equipments/${id}/parts`, data)
export const alerts = () => request.get('/alerts')
