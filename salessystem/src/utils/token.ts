import type { AuthRole } from '../types/auth';

const TOKEN_STORAGE_KEYS: Record<AuthRole, string> = {
  user: 'salessystem:user:token',
  merchant: 'salessystem:merchant:token',
  admin: 'salessystem:admin:token',
};

function getStorage() {
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

