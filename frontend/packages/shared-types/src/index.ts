export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  traceId: string
  timestamp: string
}

export interface PageResult<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface MenuNode {
  menuCode: string
  label: string
  route?: string
  componentKey?: string
  permission?: string
  sortOrder: number
  children: MenuNode[]
}

export interface UserPrincipal {
  userId: number
  username: string
  displayName: string
  roles: string[]
  permissions: string[]
}

export interface TokenResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresInSeconds: number
  user: UserPrincipal
}

export interface ModuleMeta {
  code: string
  label: string
  backendPackage: string
  sortOrder: number
}

export interface WorkbenchBaseline {
  moduleCode: 'workbench'
  status: 'READY'
  databaseReachable: boolean
}

export type {
  components as InternalApiComponents,
  paths as InternalApiPaths,
} from './generated/internal-api'
export type {
  components as SupplierApiComponents,
  paths as SupplierApiPaths,
} from './generated/supplier-api'
