import { type ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import type { AuthRole } from '../types/auth';
import RoleDenied from './RoleDenied';

interface ProtectedRouteProps {
  children: ReactNode;
  /** When set, only this role is allowed. When omitted, any authenticated role is accepted. */
  requiredRole?: AuthRole;
}

/**
 * Minimal route guard:
 * - Shows a brief loading state while auth hydration runs.
 * - Redirects to /login when unauthenticated.
 * - Shows a "role denied" screen when the user's role does not match.
 * - Otherwise renders children normally.
 *
 * No existing behaviour is changed -- public routes stay unprotected.
 */
export default function ProtectedRoute({ children, requiredRole }: ProtectedRouteProps) {
  const { isReady, isAuthenticated, currentRole } = useAuth();
  const location = useLocation();

  // Auth context still hydrating from localStorage / API -- show a lightweight loader.
  if (!isReady) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="flex flex-col items-center gap-3 text-slate-500">
          <div className="w-8 h-8 border-2 border-primary/30 border-t-primary rounded-full animate-spin" />
          <span className="text-sm font-medium">Verifying credentials...</span>
        </div>
      </div>
    );
  }

  // Not authenticated at all -- bounce to login, preserving the intended destination.
  if (!isAuthenticated || !currentRole) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  // Authenticated but wrong role -- show a focused "denied" screen instead of silently redirecting.
  if (requiredRole && currentRole !== requiredRole) {
    return <RoleDenied requiredRole={requiredRole} currentRole={currentRole} />;
  }

  return <>{children}</>;
}
