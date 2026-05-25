const TOKEN_KEY = 'ontograph_token'
const USER_KEY = 'ontograph_user'

export interface LoginResult {
  token: string
  expiresIn: number
  user: {
    id: number
    username: string
    nickname: string
  }
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(result: LoginResult): void {
  localStorage.setItem(TOKEN_KEY, result.token)
  if (result.user) {
    localStorage.setItem(USER_KEY, JSON.stringify(result.user))
  } else {
    localStorage.removeItem(USER_KEY)
  }
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getUser(): LoginResult['user'] | null {
  const userStr = localStorage.getItem(USER_KEY)
  if (!userStr || userStr === 'undefined' || userStr === 'null') return null
  try {
    return JSON.parse(userStr)
  } catch {
    return null
  }
}
