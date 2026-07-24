import { createApiClient } from '@srm/api-client'
import type { ApiResponse, TokenResponse, WorkbenchBaseline } from '@srm/shared-types'
import { clearAccessToken, getAccessToken, setAccessToken } from '../auth/session'

const baseUrl = import.meta.env.VITE_INTERNAL_API_BASE_URL ?? '/api/v1'

export const refreshInternalSession = async (): Promise<TokenResponse | undefined> => {
  const response = await fetch(`${baseUrl}/auth/refresh`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'Accept': 'application/json', 'X-Trace-Id': crypto.randomUUID().replaceAll('-', '') },
  })
  if (!response.ok) {
    clearAccessToken()
    return undefined
  }
  const envelope = await response.json() as ApiResponse<TokenResponse>
  if (envelope.code !== '0') {
    clearAccessToken()
    return undefined
  }
  setAccessToken(envelope.data.accessToken)
  return envelope.data
}

export const internalApi = createApiClient({
  baseUrl,
  getAccessToken,
  refreshAccessToken: async () => Boolean(await refreshInternalSession()),
  onAuthenticationFailure: clearAccessToken,
})

export const getWorkbenchBaseline = () =>
  internalApi.get<WorkbenchBaseline>('/workbench/baseline')
