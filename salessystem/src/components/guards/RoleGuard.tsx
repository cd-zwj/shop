import { type ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import type { AuthRole } from '../../types/auth';

interface RoleGuardProps {
  children: ReactNode;
  allowedRoles: AuthRole[];
}

export default function RoleGuard({ children, allowedRoles }: RoleGuardProps) {
  const { currentRole } = useAuth();

  if (!currentRole || !allowedRoles.includes(currentRole)) {
    // Redirect to the default home page for the current role
    if (currentRole === 'merchant') {
      return <Navigate to="/merchant" replace />;
    }
    if (currentRole === 'admin') {
      return <Navigate to="/admin" replace />;
    }
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
