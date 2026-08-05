const toHex = (bytes: Uint8Array): string =>
  Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')

const randomHex = (byteLength: number): string => {
  const globalCrypto = typeof globalThis !== 'undefined' ? globalThis.crypto : undefined
  if (globalCrypto && typeof globalCrypto.getRandomValues === 'function') {
    const bytes = new Uint8Array(byteLength)
    globalCrypto.getRandomValues(bytes)
    return toHex(bytes)
  }
  let hex = ''
  for (let i = 0; i < byteLength * 2; i += 1) hex += Math.floor(Math.random() * 16).toString(16)
  return hex
}

export const randomTraceId = (): string => {
  const globalCrypto = typeof globalThis !== 'undefined' ? globalThis.crypto : undefined
  if (globalCrypto && typeof globalCrypto.randomUUID === 'function') {
    try {
      return globalCrypto.randomUUID().replaceAll('-', '')
    } catch {
      return randomHex(16)
    }
  }
  return randomHex(16)
}
