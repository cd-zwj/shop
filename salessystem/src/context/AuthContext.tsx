import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { adminAuthService } from '../services/modules/adminAuth';
import { appAuthService } from '../services/modules/appAuth';
import { appUserService } from '../services/modules/appUser';
import { merchantAuthService } from '../services/modules/merchantAuth';
import { AUTH_TOKEN_CLEAR_EVENT } from '../services/http';
import type {
  AdminSession,
  AuthRole,
  MerchantSession,
  PlatformLoginDTO,
  PlatformRegisterDTO,
  PlatformUser,
} from '../types/auth';
import {
  clearAdminSession,
  clearAllAuthSessions,
  clearCurrentAuthRole,
  clearMerchantSession,
  clearPlatformUserProfile,
  getAdminSession,
  getCurrentAuthRole,
  getMerchantSession,
  getPlatformUserProfile,
  setAdminSession,
  setCurrentAuthRole,
  setMerchantSession,
  setPlatformUserProfile,
} from '../utils/authSession';
import { clearAllTokens, setToken } from '../utils/token';

type UserLoginMethod = 'password' | 'sms' | 'third-party';

interface AuthContextValue {
  currentRole: AuthRole | null;
  isReady: boolean;
  isAuthenticated: boolean;
  currentUser: PlatformUser | null;
  merchantSession: MerchantSession | null;
  adminSession: AdminSession | null;
  loginUser: (method: UserLoginMethod, payload: PlatformLoginDTO) => Promise<PlatformUser>;
  loginMerchant: (payload: PlatformLoginDTO) => Promise<MerchantSession>;
  loginAdmin: (payload: PlatformLoginDTO) => Promise<AdminSession>;
  registerUser: (payload: PlatformRegisterDTO) => Promise<PlatformUser>;
  refreshCurrentUser: () => Promise<PlatformUser | null>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/** 统一清除本地认证状态（token + 会话缓存 + React state） */
function resetLocalAuthState(
  setCurrentRoleState: (role: AuthRole | null) => void,
  setCurrentUser: (user: PlatformUser | null) => void,
  setMerchantSessionState: (session: MerchantSession | null) => void,
  setAdminSessionState: (session: AdminSession | null) => void,
) {
  clearAllTokens();
  clearAllAuthSessions();
  setCurrentRoleState(null);
  setCurrentUser(null);
  setMerchantSessionState(null);
  setAdminSessionState(null);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [currentRole, setCurrentRoleState] = useState<AuthRole | null>(() => getCurrentAuthRole());
  const [currentUser, setCurrentUser] = useState<PlatformUser | null>(() => getPlatformUserProfile());
  const [merchantSession, setMerchantSessionState] = useState<MerchantSession | null>(() =>
    getMerchantSession(),
  );
  const [adminSession, setAdminSessionState] = useState<AdminSession | null>(() => getAdminSession());
  const [isReady, setIsReady] = useState(false);

  // 监听 401 事件：http.ts 响应拦截器在收到 401 时分发此事件
  useEffect(() => {
    function handleTokenClear() {
      resetLocalAuthState(setCurrentRoleState, setCurrentUser, setMerchantSessionState, setAdminSessionState);
    }

    window.addEventListener(AUTH_TOKEN_CLEAR_EVENT, handleTokenClear);
    return () => {
      window.removeEventListener(AUTH_TOKEN_CLEAR_EVENT, handleTokenClear);
    };
  }, []);

  useEffect(() => {
    let isMounted = true;

    async function hydrate() {
      try {
        if (currentRole === 'user' && !currentUser) {
          const profile = await appUserService.getCurrentUser();
          if (!isMounted) return;
          setPlatformUserProfile(profile);
          setCurrentUser(profile);
        }

        if (currentRole === 'merchant' && !merchantSession) {
          const session = await merchantAuthService.getCurrentSession();
          if (!isMounted) return;
          setMerchantSession(session);
          setMerchantSessionState(session);
        }

        if (currentRole === 'admin' && !adminSession) {
          const session = await adminAuthService.getCurrentSession();
          if (!isMounted) return;
          setAdminSession(session);
          setAdminSessionState(session);
        }
      } catch {
        if (!isMounted) return;
        resetLocalAuthState(setCurrentRoleState, setCurrentUser, setMerchantSessionState, setAdminSessionState);
      } finally {
        if (isMounted) {
          setIsReady(true);
        }
      }
    }

    void hydrate();

    return () => {
      isMounted = false;
    };
  }, [adminSession, currentRole, currentUser, merchantSession]);

  function resetState(role: AuthRole) {
    if (role === 'user') {
      clearPlatformUserProfile();
    }
    if (role === 'merchant') {
      clearMerchantSession();
    }
    if (role === 'admin') {
      clearAdminSession();
    }
  }

  async function activateRole(role: AuthRole) {
    clearAllTokens();
    clearAllAuthSessions();
    setCurrentAuthRole(role);
    setCurrentRoleState(role);
    setCurrentUser(null);
    setMerchantSessionState(null);
    setAdminSessionState(null);
  }

  async function loginUser(method: UserLoginMethod, payload: PlatformLoginDTO) {
    await activateRole('user');

    const token =
      method === 'password'
        ? await appAuthService.loginByPassword(payload)
        : method === 'sms'
          ? await appAuthService.loginBySms(payload)
          : await appAuthService.loginByThirdParty(payload);

    setToken('user', token);
    const profile = await appUserService.getCurrentUser();
    setPlatformUserProfile(profile);
    setCurrentUser(profile);
    return profile;
  }

  async function loginMerchant(payload: PlatformLoginDTO) {
    await activateRole('merchant');
    const session = await merchantAuthService.login(payload);
    setToken('merchant', session.token);
    setMerchantSession(session);
    setMerchantSessionState(session);
    return session;
  }

  async function loginAdmin(payload: PlatformLoginDTO) {
    await activateRole('admin');
    const token = await adminAuthService.login(payload);
    setToken('admin', token);
    const session = await adminAuthService.getCurrentSession();
    setAdminSession(session);
    setAdminSessionState(session);
    return session;
  }

  function registerUser(payload: PlatformRegisterDTO) {
    return appAuthService.register(payload);
  }

  async function refreshCurrentUser() {
    if (currentRole !== 'user') {
      return null;
    }

    const profile = await appUserService.getCurrentUser();
    setPlatformUserProfile(profile);
    setCurrentUser(profile);
    return profile;
  }

  async function logout() {
    const role = currentRole;

    try {
      if (role === 'user') {
        await appAuthService.logout();
      } else if (role === 'merchant') {
        await merchantAuthService.logout();
      } else if (role === 'admin') {
        await adminAuthService.logout();
      }
    } catch {
      // Keep local cleanup even when server logout fails.
    } finally {
      clearAllTokens();
      clearAllAuthSessions();
      if (role) {
        resetState(role);
      }
      clearCurrentAuthRole();
      setCurrentRoleState(null);
      setCurrentUser(null);
      setMerchantSessionState(null);
      setAdminSessionState(null);
    }
  }

  const value = useMemo<AuthContextValue>(
    () => ({
      currentRole,
      isReady,
      isAuthenticated: Boolean(currentRole),
      currentUser,
      merchantSession,
      adminSession,
      loginUser,
      loginMerchant,
      loginAdmin,
      registerUser,
      refreshCurrentUser,
      logout,
    }),
    [adminSession, currentRole, currentUser, isReady, merchantSession],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }

  return context;
}

