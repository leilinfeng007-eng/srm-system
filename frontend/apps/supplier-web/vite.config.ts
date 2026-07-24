import { createSrmViteConfig } from '@srm/config/vite'

export default createSrmViteConfig(5174, process.env.SRM_SUPPLIER_BASE_PATH ?? '/supplier/')
