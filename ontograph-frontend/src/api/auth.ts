import request from './request'
import type { LoginResult } from '@/utils/auth'
import type { MenuItem } from './menu'

/**
 * 登录表单
 */
export interface LoginForm {
  username: string
  password: string
}

/**
 * 用户信息
 */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  email?: string
  avatar?: string
}

/**
 * 认证 API
 */
export const authApi = {
  /**
   * 用户登录
   * POST /auth/login
   */
  login: (data: LoginForm): Promise<LoginResult> => {
    return request.post('/auth/login', data)
  },

  /**
   * 用户登出
   * POST /auth/logout
   */
  logout: (): Promise<void> => {
    return request.post('/auth/logout')
  },

  /**
   * 获取当前用户信息
   * GET /auth/info
   */
  getInfo: (): Promise<UserInfo> => {
    return request.get('/auth/info')
  },

  /**
   * 获取当前用户的菜单树
   * GET /auth/menus
   */
  getMenus: (): Promise<MenuItem[]> => {
    return request.get('/auth/menus')
  }
}

export default authApi
