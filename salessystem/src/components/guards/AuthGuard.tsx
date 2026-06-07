import { type ReactNode, useEffect, useRef, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { adminAuthService } from '../../services/modules/adminAuth';
import { merchantAuthService } from '../../services/modules/merchantAuth';
import { getToken } from '../../utils/token';

export default function AuthGuard({ children }: { children: ReactNode }) {
  const { isReady, currentRole, currentUser, merchantSession, adminSession, refreshCurrentUser, logout } = useAuth();
  const location = useLocation();
  const [isRecovering, setIsRecovering] = useState(false);
  const verificationDone = useRef(false);

  const tokenExists = currentRole ? !!getToken(currentRole) : false;

  // Determine whether the server-confirmed session is present for the active role.
  const hasSession =
    (currentRole === 'user' && !!currentUser) ||
    (currentRole === 'merchant' && !!merchantSession) ||
    (currentRole === 'admin' && !!adminSession) ||
    false;

  useEffect(() => {
    // Only verify once per mount to avoid infinite loops when session objects are
    // re-populated by AuthContext hydrate() and trigger this effect again.
    if (!tokenExists || hasSession || isRecovering || verificationDone.current) {
      return;
    }
    verificationDone.current = true;

    async function verifySession() {
      setIsRecovering(true);
      try {
        if (currentRole === 'user') {
          await refreshCurrentUser();
        } else if (currentRole === 'merchant') {
          await merchantAuthService.getCurrentSession();
        } else if (currentRole === 'admin') {
          await adminAuthService.getCurrentSession();
        }
      } catch {
        await logout();
      } finally {
        setIsRecovering(false);
      }
    }

    void verifySession();
  }, [tokenExists, hasSession, isRecovering, currentRole, refreshCurrentUser, logout]);

  if (!isReady || isRecovering) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="flex flex-col items-center gap-3 text-slate-500">
          <div className="w-8 h-8 border-2 border-primary/30 border-t-primary rounded-full animate-spin" />
          <span className="text-sm font-medium">验证登录状态...</span>
        </div>
      </div>
    );
  }

  if (!tokenExists) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  // After verification, reject any role that still lacks a server-confirmed session.
  if (!hasSession) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <>{children}</>;
}
