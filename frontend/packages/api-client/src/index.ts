import type { ApiResponse } from '@srm/shared-types'

export class ApiError extends Error {
  constructor(
    readonly code: string,
    message: string,
    readonly status: number,
    readonly traceId?: string,
    readonly details?: unknown,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export interface ApiClientOptions {
  baseUrl: string
  getAccessToken?: () => string | undefined
  refreshAccessToken?: () => Promise<boolean>
  onAuthenticationFailure?: () => void
}

export interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown
  retryAuthentication?: boolean
}

export class ApiClient {
  private refreshPromise?: Promise<boolean>

  constructor(private readonly options: ApiClientOptions) {}

  get<T>(path: string): Promise<T> {
    return this.request<T>(path)
  }

  post<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(path, { method: 'POST', body })
  }

  async request<T>(path: string, request: RequestOptions = {}): Promise<T> {
    const response = await this.perform(path, request)
    if (response.status === 401 && request.retryAuthentication !== false && this.options.refreshAccessToken) {
      this.refreshPromise ??= this.options.refreshAccessToken().finally(() => {
        this.refreshPromise = undefined
      })
      if (await this.refreshPromise) {
        return this.request<T>(path, { ...request, retryAuthentication: false })
      }
      this.options.onAuthenticationFailure?.()
    }
    return this.unwrap<T>(response)
  }

  private perform(path: string, request: RequestOptions): Promise<Response> {
    const headers = new Headers(request.headers)
    headers.set('Accept', 'application/json')
    headers.set('X-Trace-Id', crypto.randomUUID().replaceAll('-', ''))
    const token = this.options.getAccessToken?.()
    if (token) headers.set('Authorization', `Bearer ${token}`)
    let body: BodyInit | undefined
    if (request.body !== undefined) {
      headers.set('Content-Type', 'application/json')
      body = JSON.stringify(request.body)
    }
    return fetch(`${this.options.baseUrl}${path}`, {
      ...request,
      headers,
      body,
      credentials: 'include',
    })
  }

  private async unwrap<T>(response: Response): Promise<T> {
    let payload: ApiResponse<T>
    try {
      const responseText = await response.text()
      payload = JSON.parse(responseText) as ApiResponse<T>
    } catch {
      console.error(
        `SRM API response parse failure url=${response.url} status=${response.status} `
        + `contentType=${response.headers.get('Content-Type') ?? 'none'}`,
      )
      throw new ApiError('PLATFORM_INVALID_RESPONSE', '服务返回了无法解析的响应', response.status)
    }
    if (!response.ok || payload.code !== '0') {
      throw new ApiError(payload.code, payload.message, response.status, payload.traceId, payload.data)
    }
    return payload.data
  }
}

export const createApiClient = (options: ApiClientOptions) => new ApiClient(options)

export { createInternalContractClient } from './generated/internal-api-client'
export { createSupplierContractClient } from './generated/supplier-api-client'
