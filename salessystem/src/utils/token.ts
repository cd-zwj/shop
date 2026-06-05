import type { AuthRole } from '../types/auth';

/**
 * 三端 token 存储 key
 * 用户端: salessystem:app:token
 * 商户端: salessystem:merchant:token
 * 管理端: salessystem:platform:token
 */
const TOKEN_STORAGE_KEYS: Record<AuthRole, string> = {
  user: 'salessystem:app:token',
  merchant: 'salessystem:merchant:token',
  admin: 'salessystem:platform:token',
};

function getStorage(): Storage | null {
  if (typeof window === 'undefined') {
    return null;
  }

  return window.localStorage;
}

export function getToken(role: AuthRole): string | null {
  return getStorage()?.getItem(TOKEN_STORAGE_KEYS[role]) ?? null;
}

export function setToken(role: AuthRole, token: string) {
  getStorage()?.setItem(TOKEN_STORAGE_KEYS[role], token);
}

export function clearToken(role: AuthRole) {
  getStorage()?.removeItem(TOKEN_STORAGE_KEYS[role]);
}

export function clearAllTokens() {
  const storage = getStorage();
  if (!storage) {
    return;
  }

  Object.values(TOKEN_STORAGE_KEYS).forEach((key) => storage.removeItem(key));
}
