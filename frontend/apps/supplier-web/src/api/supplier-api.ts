import { createApiClient } from '@srm/api-client'

// Supplier identity is deliberately separate from internal JWT/menu authentication.
// This client has no internal access token or refresh hook in stage 0.
export const supplierApi = createApiClient({
  baseUrl: import.meta.env.VITE_SUPPLIER_API_BASE_URL ?? '/supplier-api/v1',
})
