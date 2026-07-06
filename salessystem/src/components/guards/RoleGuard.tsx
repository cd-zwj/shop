import { type ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import type { AuthRole } from '../../types/auth';
import {
  hasMerchantPermission,
  type MerchantPermission,
} from '../../utils/merchantPermissions';

interface RoleGuardProps {
  children: ReactNode;
  allowedRoles: AuthRole[];
  merchantPermission?: MerchantPermission;
}

export default function RoleGuard({ children, allowedRoles, merchantPermission }: RoleGuardProps) {
  const { currentRole, currentUser, merchantSession, adminSession } = useAuth();

  if (!currentRole || !allowedRoles.includes(currentRole)) {
    if (currentRole === 'merchant') {
      return <Navigate to="/merchant" replace />;
    }
    if (currentRole === 'admin') {
      return <Navigate to="/admin" replace />;
    }
    return <Navigate to="/" replace />;
  }

  if (currentRole === 'user' && !currentUser) {
    return <Navigate to="/login" replace />;
  }
  if (currentRole === 'merchant' && !merchantSession) {
    return <Navigate to="/login" replace />;
  }
  if (currentRole === 'admin' && !adminSession) {
    return <Navigate to="/login" replace />;
  }

  if (currentRole === 'merchant' && merchantPermission) {
    if (!hasMerchantPermission(merchantSession?.employeeRole, merchantPermission)) {
      return <Navigate to="/merchant" replace />;
    }
  }

  return <>{children}</>;
}
