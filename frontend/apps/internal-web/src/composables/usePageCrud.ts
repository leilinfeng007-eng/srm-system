import { ref, reactive, onMounted } from 'vue'
import type { Ref } from 'vue'
import { internalApi } from '../api/internal-api'
import type { PageResult } from '@srm/shared-types'

export interface CrudOptions {
  endpoint: string
  permissionPrefix: string
  defaultPageSize?: number
}

export function usePageCrud<T extends Record<string, unknown>>(options: CrudOptions) {
  const loading = ref(false)
  const error = ref('')
  const data = ref<T[]>([]) as Ref<T[]>
  const total = ref(0)
  const page = ref(1)
  const pageSize = ref(options.defaultPageSize ?? 20)

  const dialogVisible = ref(false)
  const dialogMode = ref<'create' | 'edit'>('create')
  const submitting = ref(false)
  const editingRow = ref<T | null>(null)

  const searchForm = reactive<Record<string, string>>({ keyword: '' })

  const fetchData = async () => {
    loading.value = true
    error.value = ''
    try {
      const params = new URLSearchParams({
        page: String(page.value),
        pageSize: String(pageSize.value),
      })
      for (const [k, v] of Object.entries(searchForm)) {
        if (v) params.set(k, v)
      }
      const result = await internalApi.request<PageResult<T>>(`${options.endpoint}?${params.toString()}`, { method: 'GET' })
      data.value = result.items
      total.value = result.total
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '加载数据失败'
      data.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  const handleSearch = () => {
    page.value = 1
    fetchData()
  }

  const handleReset = () => {
    for (const key of Object.keys(searchForm)) {
      searchForm[key] = ''
    }
    handleSearch()
  }

  const handlePageChange = (p: number) => {
    page.value = p
    fetchData()
  }

  const handleSizeChange = (s: number) => {
    pageSize.value = s
    page.value = 1
    fetchData()
  }

  const openCreate = () => {
    dialogMode.value = 'create'
    editingRow.value = null
    dialogVisible.value = true
  }

  const openEdit = (row: T) => {
    dialogMode.value = 'edit'
    editingRow.value = row
    dialogVisible.value = true
  }

  const closeDialog = () => {
    dialogVisible.value = false
    editingRow.value = null
  }

  const handleSubmit = async (form: Record<string, unknown>) => {
    submitting.value = true
    try {
      if (dialogMode.value === 'create') {
        await internalApi.post(`${options.endpoint}`, form)
      } else {
        const id = editingRow.value?.id
        if (id) {
          await internalApi.request<void>(`${options.endpoint}/${id}`, { method: 'PUT', body: form })
        }
      }
      dialogVisible.value = false
      editingRow.value = null
      await fetchData()
    } finally {
      submitting.value = false
    }
  }

  const handleEnable = async (row: T) => {
    try {
      await internalApi.request<void>(`${options.endpoint}/${row.id}/enable`, { method: 'PUT' })
      await fetchData()
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '操作失败'
    }
  }

  const handleDisable = async (row: T) => {
    try {
      await internalApi.request<void>(`${options.endpoint}/${row.id}/disable`, { method: 'PUT' })
      await fetchData()
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '操作失败'
    }
  }

  onMounted(fetchData)

  return {
    loading,
    error,
    data,
    total,
    page,
    pageSize,
    searchForm,
    dialogVisible,
    dialogMode,
    submitting,
    editingRow,
    fetchData,
    handleSearch,
    handleReset,
    handlePageChange,
    handleSizeChange,
    openCreate,
    openEdit,
    closeDialog,
    handleSubmit,
    handleEnable,
    handleDisable,
  }
}
