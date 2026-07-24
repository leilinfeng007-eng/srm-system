import { computed, type MaybeRefOrGetter, toValue } from 'vue'
import { useAuthStore } from '../stores/auth'

export const usePermission = (permission: MaybeRefOrGetter<string | undefined>) => {
  const auth = useAuthStore()
  return computed(() => auth.hasPermission(toValue(permission)))
}
