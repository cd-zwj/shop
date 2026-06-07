import { type ReactNode, useEffect, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { getToken } from '../../utils/token';

export default function AuthGuard({ children }: { children: ReactNode }) {
  const { isReady, currentRole, currentUser, refreshCurrentUser, logout } = useAuth();
  const location = useLocation();
  const [isRecovering, setIsRecovering] = useState(false);

  // Check if token exists for the active role
  const tokenExists = currentRole ? !!getToken(currentRole) : false;

  useEffect(() => {
    async function recover() {
      if (tokenExists && currentRole === 'user' && !currentUser && !isRecovering) {
        setIsRecovering(true);
        try {
          await refreshCurrentUser();
        } catch (e: unknown) {
          console.error('Failed to recover user profile', e);
          if (e instanceof Error) {
            console.error('Error message:', e.message);
          }
          await logout();
        } finally {
          setIsRecovering(false);
        }
      }
    }
    void recover();
  }, [tokenExists, currentRole, currentUser, refreshCurrentUser, logout, isRecovering]);

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

  // If role is user, we must have currentUser
  if (currentRole === 'user' && !currentUser) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <>{children}</>;
}
