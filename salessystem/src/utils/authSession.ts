import type { AdminSession, AuthRole, MerchantSession, PlatformUser } from '../types/auth';

const STORAGE_KEYS = {
  currentRole: 'salessystem:current-role',
  merchantSession: 'salessystem:merchant:session',
  adminSession: 'salessystem:admin:session',
  platformUser: 'salessystem:user:profile',
};

function getStorage() {
  if (typeof window === 'undefined') {
    return null;
  }

  return window.localStorage;
}

function readJson<T>(key: string): T | null {
  const raw = getStorage()?.getItem(key);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

function writeJson(key: string, value: unknown) {
  getStorage()?.setItem(key, JSON.stringify(value));
}

function remove(key: string) {
  getStorage()?.removeItem(key);
}

export function getCurrentAuthRole(): AuthRole | null {
  const role = getStorage()?.getItem(STORAGE_KEYS.currentRole);
  if (role === 'user' || role === 'merchant' || role === 'admin') {
    return role;
  }

  return null;
}

export function setCurrentAuthRole(role: AuthRole) {
  getStorage()?.setItem(STORAGE_KEYS.currentRole, role);
}

export function clearCurrentAuthRole() {
  remove(STORAGE_KEYS.currentRole);
}

export function getMerchantSession(): MerchantSession | null {
  return readJson<MerchantSession>(STORAGE_KEYS.merchantSession);
}

export function setMerchantSession(session: MerchantSession) {
  writeJson(STORAGE_KEYS.merchantSession, session);
}

export function clearMerchantSession() {
  remove(STORAGE_KEYS.merchantSession);
}

export function updateMerchantSessionTenant(tenantId: number) {
  const session = getMerchantSession();
  if (!session) {
    return;
  }

  const currentTenant =
    session.tenants.find((item) => item.tenantId === tenantId) ?? session.tenants[0];

  if (!currentTenant) {
    return;
  }

  setMerchantSession({
    ...session,
    tenantId: currentTenant.tenantId,
    tenantName: currentTenant.tenantName,
    employeeRole: currentTenant.employeeRole,
  });
}

export function getCurrentMerchantTenantId(): number | null {
  return getMerchantSession()?.tenantId ?? null;
}

export function getAdminSession(): AdminSession | null {
  return readJson<AdminSession>(STORAGE_KEYS.adminSession);
}

export function setAdminSession(session: AdminSession) {
  writeJson(STORAGE_KEYS.adminSession, session);
}

export function clearAdminSession() {
  remove(STORAGE_KEYS.adminSession);
}

export function getPlatformUserProfile(): PlatformUser | null {
  return readJson<PlatformUser>(STORAGE_KEYS.platformUser);
}

export function setPlatformUserProfile(user: PlatformUser) {
  writeJson(STORAGE_KEYS.platformUser, user);
}

export function clearPlatformUserProfile() {
  remove(STORAGE_KEYS.platformUser);
}

export function clearAllAuthSessions() {
  clearCurrentAuthRole();
  clearMerchantSession();
  clearAdminSession();
  clearPlatformUserProfile();
}

