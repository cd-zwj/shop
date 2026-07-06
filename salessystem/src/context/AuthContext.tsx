import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useRef,
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
  PlatformLoginDTO, SmsLoginDTO,
  PlatformRegisterDTO,
  PlatformUser,
} from '../types/auth';
import {
  clearAdminSession,
  clearAllAuthSessions,
  clearCurrentAuthRole,
  clearMerchantSession,
  clearPlatformUserProfile,
  getCurrentAuthRole,
  getMerchantSession,
  setAdminSession,
  setCurrentAuthRole,
  setMerchantSession,
  setPlatformUserProfile,
} from '../utils/authSession';
import { clearAllTokens, getToken, setToken } from '../utils/token';

type UserLoginMethod = 'password' | 'sms' | 'third-party';

interface AuthContextValue {
  currentRole: AuthRole | null;
  isReady: boolean;
  isAuthenticated: boolean;
  currentUser: PlatformUser | null;
  merchantSession: MerchantSession | null;
  adminSession: AdminSession | null;
  loginUser: (method: UserLoginMethod, payload: PlatformLoginDTO | SmsLoginDTO) => Promise<PlatformUser>;
  loginMerchant: (payload: PlatformLoginDTO) => Promise<MerchantSession>;
  loginAdmin: (payload: PlatformLoginDTO) => Promise<AdminSession>;
  refreshAdminSession: () => Promise<AdminSession | null>;
  refreshMerchantSession: () => Promise<MerchantSession | null>;
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
  const [currentUser, setCurrentUser] = useState<PlatformUser | null>(null);
  const [merchantSession, setMerchantSessionState] = useState<MerchantSession | null>(null);
  const [adminSession, setAdminSessionState] = useState<AdminSession | null>(null);
  const [isReady, setIsReady] = useState(false);
  const isLoginInProgress = useRef(false);

  // 监听 401 事件：http.ts 响应拦截器在收到 401 时分发此事件
  useEffect(() => {
    function handleTokenClear(e: Event) {
      if (isLoginInProgress.current) return;
      const detail = (e as CustomEvent).detail;
      // 如果事件指定了角色，只处理当前角色的事件
      if (detail?.role && detail.role !== currentRole) return;
      resetLocalAuthState(setCurrentRoleState, setCurrentUser, setMerchantSessionState, setAdminSessionState);
    }

    window.addEventListener(AUTH_TOKEN_CLEAR_EVENT, handleTokenClear);
    return () => {
      window.removeEventListener(AUTH_TOKEN_CLEAR_EVENT, handleTokenClear);
    };
  }, [currentRole]);

  useEffect(() => {
    let isMounted = true;
    const role = currentRole;

    async function hydrate() {
      try {
        if (role && !getToken(role)) {
          resetLocalAuthState(setCurrentRoleState, setCurrentUser, setMerchantSessionState, setAdminSessionState);
          return;
        }

        if (role === 'user') {
          const profile = await appUserService.getCurrentUser();
          if (!isMounted) return;
          setPlatformUserProfile(profile);
          setCurrentUser(profile);
        }

        if (role === 'merchant') {
          const cachedTenantId = getMerchantSession()?.tenantId;
          const session = await merchantAuthService.getCurrentSession();
          const selectedTenant = cachedTenantId
            ? session.tenants.find((item) => item.tenantId === cachedTenantId)
            : null;
          const hydratedSession = selectedTenant
            ? {
                ...session,
                tenantId: selectedTenant.tenantId,
                tenantName: selectedTenant.tenantName,
                employeeRole: selectedTenant.employeeRole,
              }
            : session;
          if (!isMounted) return;
          setMerchantSession(hydratedSession);
          setMerchantSessionState(hydratedSession);
        }

        if (role === 'admin') {
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
  }, [currentRole]);

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

  function clearLocalAuthBeforeLogin() {
    clearAllTokens();
    clearAllAuthSessions();
    setCurrentRoleState(null);
    setCurrentUser(null);
    setMerchantSessionState(null);
    setAdminSessionState(null);
  }

  function commitRole(role: AuthRole) {
    setCurrentAuthRole(role);
    setCurrentRoleState(role);
  }

  async function loginUser(method: UserLoginMethod, payload: PlatformLoginDTO | SmsLoginDTO) {
    isLoginInProgress.current = true;
    try {
      clearLocalAuthBeforeLogin();

      let token: string;
      if (method === 'password') {
        token = await appAuthService.loginByPassword(payload as PlatformLoginDTO);
      } else if (method === 'sms') {
        token = await appAuthService.loginBySms(payload as SmsLoginDTO);
      } else {
        token = await appAuthService.loginByThirdParty(payload as PlatformLoginDTO);
      }

      setToken('user', token);
      const profile = await appUserService.getCurrentUser();
      setPlatformUserProfile(profile);
      setCurrentUser(profile);
      commitRole('user');
      return profile;
    } finally {
      isLoginInProgress.current = false;
    }
  }

  async function loginMerchant(payload: PlatformLoginDTO) {
    isLoginInProgress.current = true;
    try {
      clearLocalAuthBeforeLogin();
      const session = await merchantAuthService.login(payload);
      setToken('merchant', session.token);
      setMerchantSession(session);
      setMerchantSessionState(session);
      commitRole('merchant');
      return session;
    } finally {
      isLoginInProgress.current = false;
    }
  }

  async function loginAdmin(payload: PlatformLoginDTO) {
    isLoginInProgress.current = true;
    try {
      clearLocalAuthBeforeLogin();
      const token = await adminAuthService.login(payload);
      setToken('admin', token);
      const session = await adminAuthService.getCurrentSession();
      setAdminSession(session);
      setAdminSessionState(session);
      commitRole('admin');
      return session;
    } finally {
      isLoginInProgress.current = false;
    }
  }

  function registerUser(payload: PlatformRegisterDTO) {
    return appAuthService.register(payload);
  }

  async function refreshCurrentUser() {
    const role = currentRole ?? getCurrentAuthRole();
    if (role !== 'user') {
      return null;
    }

    const profile = await appUserService.getCurrentUser();
    setPlatformUserProfile(profile);
    setCurrentUser(profile);
    return profile;
  }

  async function refreshMerchantSession() {
    const role = currentRole ?? getCurrentAuthRole();
    if (role !== 'merchant') {
      return null;
    }

    const session = await merchantAuthService.getCurrentSession();
    setMerchantSession(session);
    setMerchantSessionState(session);
    return session;
  }

  async function refreshAdminSession() {
    const role = currentRole ?? getCurrentAuthRole();
    if (role !== 'admin') {
      return null;
    }

    const session = await adminAuthService.getCurrentSession();
    setAdminSession(session);
    setAdminSessionState(session);
    return session;
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

  const isAuthenticated =
    (currentRole === 'user' && !!currentUser) ||
    (currentRole === 'merchant' && !!merchantSession) ||
    (currentRole === 'admin' && !!adminSession);

  const value = useMemo<AuthContextValue>(
    () => ({
      currentRole,
      isReady,
      isAuthenticated,
      currentUser,
      merchantSession,
      adminSession,
      loginUser,
      loginMerchant,
      loginAdmin,
      refreshAdminSession,
      refreshMerchantSession,
      registerUser,
      refreshCurrentUser,
      logout,
    }),
    [adminSession, currentRole, currentUser, isAuthenticated, isReady, merchantSession],
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

