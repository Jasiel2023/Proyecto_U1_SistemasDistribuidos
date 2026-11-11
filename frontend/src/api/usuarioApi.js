import { api } from './client'

export async function getByIdCuenta(idCuenta) {
  const { data } = await api.get(`/v1/usuarios/cuenta/${idCuenta}`)
  return data
}

export async function listUsuarios(params = {}) {
  const searchParams = new URLSearchParams()
  if (params.page !== undefined) searchParams.set('page', params.page)
  if (params.size !== undefined) searchParams.set('size', params.size)
  if (params.sort) searchParams.set('sort', params.sort)
  if (params.search) searchParams.set('search', params.search)

  const { data } = await api.get(`/v1/usuarios?${searchParams}`)
  return data
}