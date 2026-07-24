let accessToken: string | undefined

export const getAccessToken = () => accessToken
export const setAccessToken = (token: string) => { accessToken = token }
export const clearAccessToken = () => { accessToken = undefined }
